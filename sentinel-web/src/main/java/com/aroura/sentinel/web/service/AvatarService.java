package com.aroura.sentinel.web.service;

import com.aroura.sentinel.web.dao.SentinelUserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Locale;

@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);
    private static final long MAX_SIZE_BYTES = 2L * 1024 * 1024;

    private final SentinelUserDao userDao;

    public AvatarService(SentinelUserDao userDao) {
        this.userDao = userDao;
    }

    public String upload(String username, MultipartFile file) {
        if (username == null || username.trim().isEmpty() || file == null || file.isEmpty()) {
            return null;
        }
        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || file.getSize() > MAX_SIZE_BYTES) {
            return null;
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > MAX_SIZE_BYTES) {
                return null;
            }
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            userDao.updateAvatar(username.trim(), dataUrl);
            log.info("[Avatar] 用户头像已更新 username={} size={} type={}", username, bytes.length, contentType);
            return dataUrl;
        } catch (Exception e) {
            log.error("[Avatar] 用户头像上传失败 username={} error={}", username, e.getMessage(), e);
            return null;
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String value = contentType.trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(value)) {
            return "image/jpeg";
        }
        if ("image/jpeg".equals(value) || "image/png".equals(value)
                || "image/gif".equals(value) || "image/webp".equals(value)) {
            return value;
        }
        return null;
    }
}