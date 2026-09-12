package com.aroura.sentinel;

import com.aroura.sentinel.common.constant.SentinelConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @author Sentinel
 */
@SpringBootApplication
@Slf4j
public class SentinelApplication implements CommandLineRunner {

    @Value("${server.port}")
    private String serverPort;

    public static void main(String[] args) {
        /**
         * 如果你需要启动Apollo动态配置
         * 1、启动apollo
         * 2、将application.properties配置文件的 sentinel.apollo.enabled 改为true
         * 3、下方的property替换真实的ip和port
         */
        System.setProperty("apollo.config-service", "http://sentinel-apollo-config:8080");
        SpringApplication.run(SentinelApplication.class, args);

    }

    @Override
    public void run(String... args) {
        log.info(AnsiOutput.toString(SentinelConstant.PROJECT_BANNER, "\n", AnsiColor.GREEN, SentinelConstant.PROJECT_NAME, AnsiColor.DEFAULT, AnsiStyle.FAINT));
        log.info("Sentinel start succeeded, Index >> http://127.0.0.1:{}/", serverPort);
        log.info("Sentinel start succeeded, Swagger Url >> http://127.0.0.1:{}/swagger-ui/index.html", serverPort);
    }
}
