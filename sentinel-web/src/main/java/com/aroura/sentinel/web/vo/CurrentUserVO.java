package com.aroura.sentinel.web.vo;

import lombok.Data;

/**
 * 当前登录用户 VO（由 AuthInterceptor 从 Redis 会话解析，贯穿请求）
 *
 * @author sentinel
 */
@Data
public class CurrentUserVO {

    private String username;
    private String role;
    private String nickname;
}
