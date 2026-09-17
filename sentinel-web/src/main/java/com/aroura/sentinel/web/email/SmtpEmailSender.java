package com.aroura.sentinel.web.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

@Slf4j
@Component
public class SmtpEmailSender implements EmailSender {

    private final EmailProperties properties;

    public SmtpEmailSender(EmailProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean send(String email, String code) {
        if (!properties.isEnabled()) {
            log.warn("[Email] SMTP 未开启，拒绝发送 email={}", mask(email));
            return false;
        }
        if (isBlank(properties.getHost()) || isBlank(properties.getUsername())
                || isBlank(properties.getPassword())) {
            log.error("[Email] SMTP 配置不完整：host/username/password 必填");
            return false;
        }

        String from = isBlank(properties.getFrom()) ? properties.getUsername() : properties.getFrom().trim();
        try {
            Session session = Session.getInstance(buildProperties(),
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(properties.getUsername(), properties.getPassword());
                        }
                    });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSentDate(new Date());
            message.setSubject("SENTINEL 登录验证码", "UTF-8");
            message.setContent(buildContent(code), "text/html;charset=UTF-8");
            message.saveChanges();

            Transport.send(message);
            log.info("[Email] SMTP 发送成功 email={}", mask(email));
            return true;
        } catch (Exception e) {
            log.error("[Email] SMTP 发送异常 email={} error={}", mask(email), e.getMessage(), e);
            return false;
        }
    }

    private Properties buildProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", properties.getHost().trim());
        props.put("mail.smtp.port", String.valueOf(properties.getPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", String.valueOf(properties.getConnectionTimeoutSeconds() * 1000));
        props.put("mail.smtp.timeout", String.valueOf(properties.getTimeoutSeconds() * 1000));
        props.put("mail.smtp.writetimeout", String.valueOf(properties.getTimeoutSeconds() * 1000));
        if (properties.isSslEnabled()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.socketFactory.port", String.valueOf(properties.getPort()));
        }
        if (properties.isStarttlsEnabled()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        return props;
    }

    private static String buildContent(String code) {
        return "<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;color:#1d2129;line-height:1.7\">"
                + "<p>你正在登录 SENTINEL 物流异常智能处置平台。</p>"
                + "<p>本次验证码：<strong style=\"font-size:24px;letter-spacing:4px;color:#0891b2\">"
                + code + "</strong></p>"
                + "<p>验证码 5 分钟内有效，输入错误 5 次后失效，请勿转发给他人。</p>"
                + "<p style=\"color:#86909c;font-size:12px\">如果不是你本人操作，请忽略本邮件。</p>"
                + "</div>";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String mask(String email) {
        if (email == null) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}