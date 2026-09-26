package com.aroura.sentinel.web.vo;

import lombok.Data;

/**
 * 当前登录用户 VO（由 AuthInterceptor 从 Redis 会话解析，贯穿请求）
 *
 * @author sentinel
 */
@Data
public class CurrentUserVO {

    /** sentinel_user.id；legacy 格式会话（不含 userId）时为 null */
    private Long userId;

    /** 该账号绑定的商家ID；非商家角色、或商家账号未绑定档案时为 null */
    private Long merchantId;

    private String username;
    private String role;
    private String nickname;
    private String avatar;
}
