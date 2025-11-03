package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.auth.dto.*;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 *
 * @author GCRF Team
 * @date 2025-10-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 创建用户
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse createUser(CreateUserRequest request) {
        log.info("创建用户请求: username={}", request.getUsername());

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 检查手机号是否已存在
        if (StringUtils.hasText(request.getPhone())) {
            queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, request.getPhone());
            if (userMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException(ResultCode.PHONE_ALREADY_EXISTS);
            }
        }

        // 检查邮箱是否已存在
        if (StringUtils.hasText(request.getEmail())) {
            queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getEmail, request.getEmail());
            if (userMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS);
            }
        }

        // 创建用户实体
        User user = new User();
        user.setUserId(generateUserId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUserType(request.getUserType());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setStatus("ACTIVE");
        user.setFailedLoginCount(0);

        userMapper.insert(user);

        log.info("用户创建成功: userId={}, username={}", user.getId(), user.getUsername());

        return convertToUserInfoResponse(user);
    }

    /**
     * 更新用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoResponse updateUser(Long userId, UpdateUserRequest request) {
        log.info("更新用户信息请求: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查手机号是否已被其他用户使用
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, request.getPhone());
            queryWrapper.ne(User::getId, userId);
            if (userMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException(ResultCode.PHONE_ALREADY_EXISTS);
            }
        }

        // 检查邮箱是否已被其他用户使用
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getEmail, request.getEmail());
            queryWrapper.ne(User::getId, userId);
            if (userMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS);
            }
        }

        // 更新用户信息
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (StringUtils.hasText(request.getStatus())) {
            user.setStatus(request.getStatus());
        }

        userMapper.updateById(user);

        log.info("用户信息更新成功: userId={}", userId);

        return convertToUserInfoResponse(user);
    }

    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("修改密码请求: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_INCORRECT);
        }

        // 新密码不能与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        log.info("密码修改成功: userId={}", userId);
    }

    /**
     * 删除用户（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        log.info("删除用户请求: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        userMapper.deleteById(userId);

        log.info("用户删除成功: userId={}", userId);
    }

    /**
     * 获取用户详情
     */
    public UserInfoResponse getUserById(Long userId) {
        log.info("获取用户详情请求: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return convertToUserInfoResponse(user);
    }

    /**
     * 根据用户名获取用户
     */
    public UserInfoResponse getUserByUsername(String username) {
        log.info("根据用户名获取用户: username={}", username);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return convertToUserInfoResponse(user);
    }

    /**
     * 分页查询用户列表
     */
    public IPage<UserInfoResponse> getUserList(int pageNum, int pageSize, String username, String userType, String status) {
        log.info("分页查询用户列表: pageNum={}, pageSize={}, username={}, userType={}, status={}",
                 pageNum, pageSize, username, userType, status);

        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            queryWrapper.like(User::getUsername, username);
        }
        if (StringUtils.hasText(userType)) {
            queryWrapper.eq(User::getUserType, userType);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(User::getStatus, status);
        }

        queryWrapper.orderByDesc(User::getCreatedAt);

        IPage<User> userPage = userMapper.selectPage(page, queryWrapper);

        // 转换为响应DTO
        IPage<UserInfoResponse> responsePage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserInfoResponse> records = userPage.getRecords().stream()
                .map(this::convertToUserInfoResponse)
                .collect(Collectors.toList());
        responsePage.setRecords(records);

        return responsePage;
    }

    /**
     * 生成用户ID
     */
    private String generateUserId() {
        return "U" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();
    }

    /**
     * 转换为用户信息响应DTO
     */
    private UserInfoResponse convertToUserInfoResponse(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getUserId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getUserType(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getLastLoginTime(),
                user.getLastLoginIp(),
                user.getCreatedAt()
        );
    }
}
