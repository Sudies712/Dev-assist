package com.sudies.devassist.modules.system.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String accessToken;
    private String refreshToken;
    /**
     * accessToken 有效期（秒）
     */
    private long expires;
    private UserVO user;
}
