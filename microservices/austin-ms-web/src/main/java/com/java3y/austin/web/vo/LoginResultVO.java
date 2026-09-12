package com.java3y.austin.web.vo;

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
}
