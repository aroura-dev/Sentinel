package com.aroura.sentinel.web.vo;

import lombok.Data;

/**
 * 登录结果 VO
 *
 * @author sentinel
 */
@Data
public class LoginResultVO {

    private String token;
    private String username;
    private String role;
    private String nickname;
    private String avatar;

    /** sentinel_user.id */
    private Long userId;

    /** 绑定的商家ID；非商家角色或未绑定时为 null */
    private Long merchantId;
}
