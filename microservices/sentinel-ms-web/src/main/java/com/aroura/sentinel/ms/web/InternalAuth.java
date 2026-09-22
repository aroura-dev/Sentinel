package com.aroura.sentinel.ms.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 服务间身份签名。
 * <p>
 * 背景：业务服务无条件信任 {@code X-User-Name / X-User-Role} 头（见 CurrentUserFilter）。
 * 端口收敛挡住了从宿主机直连，但同一个 compose 网络内（或被攻陷的任一容器）仍可伪造这两个头，
 * 冒充任意角色。本类给身份头加上网关持有的共享密钥签名，下游校验通过才认。
 *
 * <h3>待签内容（规范化字符串，用 {@code \n} 连接）</h3>
 * <pre>principal \n role \n timestamp</pre>
 * 网关签用户身份（principal=用户名、role=用户角色）；内部服务互调签服务身份
 * （principal={@code svc:<服务名>}、role={@code INTERNAL}）。
 *
 * <h3>算法</h3>
 * {@code base64(HMAC-SHA256(secret, canonical))}。
 * <p>
 * <b>该实现必须与 {@code com.aroura.gateway.filter.AuthGlobalFilter} 内的同名前缀计算
 * 逐字节一致</b> —— 网关是 WebFlux 应用，不能依赖本 servlet 模块，所以那边有一份等价实现。
 * 改动本类时务必同步改那边，否则全网关请求都会被判为签名无效。
 *
 * @author sentinel-ms
 */
public final class InternalAuth {

    /** 身份签名头。 */
    public static final String HDR_TIMESTAMP = "X-Auth-Timestamp";
    public static final String HDR_SIGNATURE = "X-Auth-Signature";
    /** 服务身份头（内部接口要求，值为调用方服务名）。 */
    public static final String HDR_SERVICE = "X-Service-Name";
    /** 内部服务身份使用的角色占位值。 */
    public static final String SERVICE_ROLE = "INTERNAL";

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private InternalAuth() {
    }

    /** 计算签名。与网关侧实现必须一致。 */
    public static String sign(String secret, String principal, String role, long timestampSeconds) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            String canonical = principal + "\n" + (role == null ? "" : role) + "\n" + timestampSeconds;
            return ENCODER.encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // HmacSHA256 是 JDK 必备算法，走到这里说明运行环境异常，不应静默放过
            throw new IllegalStateException("服务间签名计算失败", e);
        }
    }

    /**
     * 校验签名。时间戳用于限制重放窗口。
     *
     * @param maxSkewSeconds 允许的时间偏移（秒）；超出即视为过期
     * @return 签名有效且时间戳在窗口内
     */
    public static boolean verify(String secret, String principal, String role,
                                 String timestamp, String signature, long maxSkewSeconds) {
        if (secret == null || secret.isEmpty() || signature == null || timestamp == null) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - ts) > maxSkewSeconds) {
            return false;
        }
        String expected = sign(secret, principal, role, ts);
        // 定长比较，避免按字节提前返回泄露信息
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
