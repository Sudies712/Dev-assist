package com.sudies.devassist.modules.system.controller;

import com.sudies.devassist.common.result.Result;
import com.sudies.devassist.modules.system.dto.ChangePasswordDTO;
import com.sudies.devassist.modules.system.dto.LoginDTO;
import com.sudies.devassist.modules.system.dto.ProfileDTO;
import com.sudies.devassist.modules.system.dto.RefreshDTO;
import com.sudies.devassist.modules.system.service.AuthService;
import com.sudies.devassist.modules.system.vo.LoginVO;
import com.sudies.devassist.modules.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "认证")
@RestController
@RequestMapping("/api")
public class AuthController {

    @Resource
    private AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh-token")
    public Result<LoginVO> refresh(@RequestBody @Valid RefreshDTO dto) {
        return Result.success(authService.refresh(dto));
    }

    @Operation(summary = "修改自己的密码")
    @PutMapping("/users/change-password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success();
    }

    @Operation(summary = "获取个人信息")
    @GetMapping("/users/profile")
    public Result<UserVO> profile() {
        return Result.success(authService.getProfile());
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/users/profile")
    public Result<UserVO> updateProfile(@RequestBody ProfileDTO dto) {
        return Result.success(authService.updateProfile(dto));
    }

    @Operation(summary = "上传头像")
    @PostMapping(value = "/users/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(authService.uploadAvatar(file));
    }
}
