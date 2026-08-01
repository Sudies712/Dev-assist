package com.sudies.devassist.common.constant;

/**
 * Redis 缓存 key 约定（对应关键流程详细设计 §1.1）。
 */
public final class CacheKey {

    private CacheKey() {
    }

    /**
     * 登录失败计数：login_fail:{userId}
     */
    public static final String LOGIN_FAIL_PREFIX = "login_fail:";

    /**
     * Token 黑名单：blacklist:{tokenValue}
     */
    public static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";

    /**
     * refreshToken：refresh:{userId}
     */
    public static final String REFRESH_TOKEN_PREFIX = "refresh:";

    /**
     * 用户权限缓存：user_perms:{userId}
     */
    public static final String USER_PERMS_PREFIX = "user_perms:";

    public static String loginFail(long userId) {
        return LOGIN_FAIL_PREFIX + userId;
    }

    public static String tokenBlacklist(String tokenValue) {
        return TOKEN_BLACKLIST_PREFIX + tokenValue;
    }

    public static String refreshToken(long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    public static String userPerms(long userId) {
        return USER_PERMS_PREFIX + userId;
    }
}
