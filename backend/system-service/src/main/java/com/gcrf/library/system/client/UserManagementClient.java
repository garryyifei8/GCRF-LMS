package com.gcrf.library.system.client;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcrf.library.common.result.Result;
import com.gcrf.library.system.dto.request.UserCreateRequest;
import com.gcrf.library.system.dto.request.UserPasswordResetRequest;
import com.gcrf.library.system.dto.request.UserStatusRequest;
import com.gcrf.library.system.dto.request.UserUpdateRequest;
import com.gcrf.library.system.dto.response.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理服务Feign客户端（代理至auth-service）
 *
 * @author GCRF Team
 * @since 2026-04-15
 */
@FeignClient(name = "auth-service", contextId = "userManagementClient", path = "/api/v1/users")
public interface UserManagementClient {

    @GetMapping
    Result<IPage<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String status);

    @GetMapping("/{userId}")
    Result<UserVO> getUserById(@PathVariable("userId") Long userId);

    @PostMapping
    Result<UserVO> createUser(@RequestBody UserCreateRequest request);

    @PutMapping("/{userId}")
    Result<UserVO> updateUser(@PathVariable("userId") Long userId, @RequestBody UserUpdateRequest request);

    @DeleteMapping("/{userId}")
    Result<Void> deleteUser(@PathVariable("userId") Long userId);

    @PutMapping("/{userId}/password/reset")
    Result<Void> resetPassword(@PathVariable("userId") Long userId, @RequestBody UserPasswordResetRequest request);

    @PutMapping("/{userId}/status")
    Result<Void> updateUserStatus(@PathVariable("userId") Long userId, @RequestBody UserStatusRequest request);

    @DeleteMapping("/batch")
    Result<Void> batchDeleteUsers(@RequestParam String ids);
}
