package com.aroura.sentinel.web.config;

import com.aroura.sentinel.web.vo.CurrentUserVO;

/**
 * 会话串编解码：Redis 里 {@code sentinel:token:<token>} 的值格式。
 * <p>
 * <b>v2 格式：</b>{@code v2:<userId>:<merchantId>:<username>:<role>}
 * <ul>
 *   <li>username 置于 role 之前 —— 这样任何沿用 {@code lastIndexOf(':')} 取末段的旧解析
 *       （如微服务网关的 {@code AuthGlobalFilter.Session#parse}）仍能取到正确的 role；
 *       若改写成 JSON，那些未同步的组件会直接判会话格式异常。</li>
 *   <li>merchantId 未绑定时写 {@code 0}，不留空段。</li>
 *   <li>userId / merchantId 为 {@code 0} 一律解析为 null。</li>
 * </ul>
 * <b>旧格式：</b>{@code username:role}，保留 legacy 分支做懒迁移，行为与改造前完全一致。
 * 遗留分支的生命周期 = {@code auth.remember-ttl-seconds}（7 天），
 * <b>最早可于 2026-10-02 删除</b>；删除前先确认 Redis 内已无以非 {@code v2:} 开头的 token 值。
 *
 * @author sentinel
 */
public final class SessionCodec {

    private static final String V2_PREFIX = "v2:";

    private SessionCodec() {
    }

    public static String encode(Long userId, Long merchantId, String username, String role) {
        return V2_PREFIX
                + (userId == null ? 0L : userId) + ":"
                + (merchantId == null ? 0L : merchantId) + ":"
                + (username == null ? "" : username) + ":"
                + (role == null ? "" : role);
    }

    /**
     * @return 解析结果；无法解析时返回 {@code null}（调用方应按未登录处理）。
     *         <p>
     *         注意：以 {@code v2:} 开头但结构损坏时**硬失败返回 null，绝不回退到 legacy 解析** ——
     *         否则 {@code v2:4:1:陈浩:MERCHANT} 会被旧解析读成 username={@code v2:4:1:陈浩}，
     *         静默产生一个查不到商家的假用户，比直接 401 危险得多。
     */
    public static CurrentUserVO decode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        if (value.startsWith(V2_PREFIX)) {
            return decodeV2(value.substring(V2_PREFIX.length()));
        }
        return decodeLegacy(value);
    }

    private static CurrentUserVO decodeV2(String rest) {
        // role 位于末段且不可为空，先按最后一个冒号切出来
        int lastColon = rest.lastIndexOf(':');
        if (lastColon <= 0 || lastColon == rest.length() - 1) {
            return null;
        }
        String role = rest.substring(lastColon + 1);
        // 剩余部分为 userId:merchantId:username，username 允许含冒号
        String[] head = rest.substring(0, lastColon).split(":", 3);
        if (head.length != 3) {
            return null;
        }
        Long userId = longOrNull(head[0]);
        Long merchantId = longOrNull(head[1]);
        if (userId == null && !isZero(head[0])) {
            return null;
        }
        if (merchantId == null && !isZero(head[1])) {
            return null;
        }
        CurrentUserVO vo = new CurrentUserVO();
        vo.setUserId(userId);
        vo.setMerchantId(merchantId);
        vo.setUsername(head[2]);
        vo.setRole(role);
        return vo;
    }

    private static CurrentUserVO decodeLegacy(String value) {
        int idx = value.lastIndexOf(':');
        CurrentUserVO vo = new CurrentUserVO();
        vo.setUsername(idx > 0 ? value.substring(0, idx) : value);
        vo.setRole(idx > 0 ? value.substring(idx + 1) : "");
        // legacy 会话不含 userId / merchantId，调用方需回退到按用户名反查商家
        return vo;
    }

    private static boolean isZero(String s) {
        return "0".equals(s);
    }

    private static Long longOrNull(String s) {
        try {
            long v = Long.parseLong(s);
            return v == 0L ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
