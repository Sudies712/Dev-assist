package com.sudies.devassist.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sudies.devassist.common.constant.CacheKey;
import com.sudies.devassist.common.enums.UserStatus;
import com.sudies.devassist.common.exception.BizException;
import com.sudies.devassist.common.result.ResultCode;
import com.sudies.devassist.common.utils.SecurityUtils;
import com.sudies.devassist.modules.system.dto.ChangePasswordDTO;
import com.sudies.devassist.modules.system.dto.LoginDTO;
import com.sudies.devassist.modules.system.dto.ProfileDTO;
import com.sudies.devassist.modules.system.dto.RefreshDTO;
import com.sudies.devassist.modules.system.entity.User;
import com.sudies.devassist.modules.system.mapper.UserMapper;
import com.sudies.devassist.modules.system.vo.LoginVO;
import com.sudies.devassist.modules.system.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证服务：登录/登出/刷新 Token/改密/个人信息。对应关键流程详细设计 §1.1。
 * <p>登录失败 5 次锁定 30 分钟；refreshToken 双向存 Redis（refresh:{userId} 与反向映射）；
 * 登出将 accessToken 加入 Redis 黑名单。
 */
@Service
public class AuthService {

    private static final int MAX_FAIL = 5;
    private static final long LOCK_MINUTES = 30L;
    private static final long REFRESH_TIMEOUT_SECONDS = 7 * 24 * 3600L;
    private static final int DEFAULT_LOGIN_DAY = 7;
    private static final String REFRESH_REVERSE_PREFIX = CacheKey.REFRESH_TOKEN_PREFIX + "token:";

    @Resource
    private UserMapper userMapper;

    @Resource
    private PermissionService permissionService;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Value("${sa-token.timeout:7200}")
    private long accessTimeout;

    @Value("${dev-assist.upload.dir:./uploads}")
    private String uploadDir;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "账号不存在或密码错误");
        }
        if (UserStatus.DISABLED.name().equals(user.getStatus())) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BizException(ResultCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLoginFail(user);
            throw new BizException(ResultCode.UNAUTHORIZED, "账号不存在或密码错误");
        }
        // 登录成功：清失败计数、记录登录时间
        User upd = new User();
        upd.setId(user.getId());
        upd.setLoginFailCount(0);
        upd.setLockUntil(null);
        upd.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(upd);

        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();
        // refreshToken 时长按"免登录"勾选：勾选→loginDay 天；未勾选→会话级（与 accessToken 同寿命）
        long refreshTtl = resolveRefreshTtl(dto.getRemember(), dto.getLoginDay());
        String refreshToken = issueRefreshToken(user.getId(), refreshTtl);

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpires(accessTimeout);
        vo.setUser(toUserVO(user));
        return vo;
    }

    /**
     * 计算 refreshToken TTL：勾选免登录=loginDay 天（默认 7）；未勾选=会话级，与 accessToken 同寿命
     */
    private long resolveRefreshTtl(Boolean remember, Integer loginDay) {
        if (Boolean.TRUE.equals(remember)) {
            int days = (loginDay != null && loginDay > 0) ? loginDay : DEFAULT_LOGIN_DAY;
            return days * 24 * 3600L;
        }
        return accessTimeout;
    }

    private void recordLoginFail(User user) {
        int fail = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
        User upd = new User();
        upd.setId(user.getId());
        upd.setLoginFailCount(fail);
        if (fail >= MAX_FAIL) {
            upd.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
        userMapper.updateById(upd);
    }

    private String issueRefreshToken(Long userId, long ttlSeconds) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        redisTemplate.opsForValue().set(CacheKey.refreshToken(userId), token, ttl);
        redisTemplate.opsForValue().set(REFRESH_REVERSE_PREFIX + token, String.valueOf(userId), ttl);
        return token;
    }

    public void logout() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        String tokenValue = StpUtil.getTokenValue();
        if (loginId != null) {
            redisTemplate.delete(CacheKey.refreshToken(Long.valueOf(loginId.toString())));
        }
        if (tokenValue != null) {
            redisTemplate.opsForValue().set(CacheKey.tokenBlacklist(tokenValue), "1",
                    Duration.ofSeconds(accessTimeout));
        }
        StpUtil.logout();
    }

    public LoginVO refresh(RefreshDTO dto) {
        String userIdStr = redisTemplate.opsForValue().get(REFRESH_REVERSE_PREFIX + dto.getRefreshToken());
        if (userIdStr == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }
        Long userId = Long.valueOf(userIdStr);
        String current = redisTemplate.opsForValue().get(CacheKey.refreshToken(userId));
        if (current == null || !current.equals(dto.getRefreshToken())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }
        User user = userMapper.selectById(userId);
        if (user == null || UserStatus.DISABLED.name().equals(user.getStatus())) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        // 沿用原 refreshToken 的剩余 TTL 重签，保持"免登录"语义在刷新后不被意外缩短/延长
        Long remain = redisTemplate.getExpire(REFRESH_REVERSE_PREFIX + dto.getRefreshToken());
        long ttl = (remain != null && remain > 0) ? remain : REFRESH_TIMEOUT_SECONDS;
        StpUtil.login(userId);
        LoginVO vo = new LoginVO();
        vo.setAccessToken(StpUtil.getTokenValue());
        vo.setRefreshToken(issueRefreshToken(userId, ttl));
        vo.setExpires(accessTimeout);
        vo.setUser(toUserVO(user));
        return vo;
    }

    public UserVO getProfile() {
        return toUserVO(userMapper.selectById(SecurityUtils.currentUserId()));
    }

    public UserVO updateProfile(ProfileDTO dto) {
        Long uid = SecurityUtils.currentUserId();
        User upd = new User();
        upd.setId(uid);
        upd.setRealName(dto.getRealName());
        upd.setEmail(dto.getEmail());
        upd.setPhone(dto.getPhone());
        upd.setAvatar(dto.getAvatar());
        userMapper.updateById(upd);
        return toUserVO(userMapper.selectById(uid));
    }

    /**
     * 上传头像：保存到 uploads/avatar/，更新 user.avatar，返回最新 UserVO（含可访问 URL）
     */
    public UserVO uploadAvatar(MultipartFile file) {
        Long uid = SecurityUtils.currentUserId();
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "头像文件不能为空");
        }
        String origName = file.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf('.'));
        }
        String stored = uid + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadDir, "avatar");
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(stored));
        } catch (IOException e) {
            throw new BizException(ResultCode.ERROR, "头像保存失败: " + e.getMessage());
        }
        String avatarUrl = "/api/uploads/avatar/" + stored;
        User upd = new User();
        upd.setId(uid);
        upd.setAvatar(avatarUrl);
        userMapper.updateById(upd);
        return toUserVO(userMapper.selectById(uid));
    }

    public void changePassword(ChangePasswordDTO dto) {
        Long uid = SecurityUtils.currentUserId();
        User user = userMapper.selectById(uid);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "原密码错误");
        }
        User upd = new User();
        upd.setId(uid);
        upd.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(upd);
    }

    public UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setRoles(permissionService.getRoleCodes(user.getId()));
        vo.setPermissions(permissionService.getPermissionCodes(user.getId()));
        return vo;
    }
}
