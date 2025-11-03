package com.gcrf.library.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.auth.dto.*;
import com.gcrf.library.auth.entity.User;
import com.gcrf.library.auth.mapper.UserMapper;
import com.gcrf.library.common.exception.BusinessException;
import com.gcrf.library.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService单元测试
 *
 * @author GCRF Team
 * @date 2025-10-28
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private BCryptPasswordEncoder passwordEncoder;
    private User testUser;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserId("U123456");
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setUserType("STUDENT");
        testUser.setStatus("ACTIVE");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // 创建用户请求
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("newuser");
        createUserRequest.setPassword("password123");
        createUserRequest.setEmail("newuser@example.com");
        createUserRequest.setPhone("13800138001");
        createUserRequest.setUserType("STUDENT");

        // 更新用户请求
        updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setEmail("updated@example.com");
        updateUserRequest.setPhone("13800138002");
        updateUserRequest.setStatus("ACTIVE");

        // 修改密码请求
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("password123");
        changePasswordRequest.setNewPassword("newpassword123");
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        // Act
        UserInfoResponse response = userService.createUser(createUserRequest);

        // Assert
        assertNotNull(response);
        assertEquals(createUserRequest.getUsername(), response.getUsername());
        assertEquals(createUserRequest.getEmail(), response.getEmail());
        assertEquals(createUserRequest.getPhone(), response.getPhone());
        assertEquals(createUserRequest.getUserType(), response.getUserType());
        assertEquals("ACTIVE", response.getStatus());

        verify(userMapper, times(3)).selectCount(any(LambdaQueryWrapper.class)); // username, phone, email checks
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void testCreateUser_UsernameAlreadyExists() {
        // Arrange
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(createUserRequest);
        });

        assertEquals(ResultCode.USER_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(userMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testCreateUser_PhoneAlreadyExists() {
        // Arrange
        when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)  // username check passes
                .thenReturn(1L); // phone check fails

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(createUserRequest);
        });

        assertEquals(ResultCode.PHONE_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(userMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testCreateUser_EmailAlreadyExists() {
        // Arrange
        when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)  // username check passes
                .thenReturn(0L)  // phone check passes
                .thenReturn(1L); // email check fails

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.createUser(createUserRequest);
        });

        assertEquals(ResultCode.EMAIL_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(userMapper, times(3)).selectCount(any(LambdaQueryWrapper.class));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testUpdateUser_Success() {
        // Arrange
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // Act
        UserInfoResponse response = userService.updateUser(1L, updateUserRequest);

        // Assert
        assertNotNull(response);
        assertEquals(updateUserRequest.getEmail(), response.getEmail());
        assertEquals(updateUserRequest.getPhone(), response.getPhone());

        verify(userMapper).selectById(1L);
        verify(userMapper, times(2)).selectCount(any(LambdaQueryWrapper.class)); // phone and email checks
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void testUpdateUser_UserNotFound() {
        // Arrange
        when(userMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateUser(999L, updateUserRequest);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(userMapper).selectById(999L);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void testUpdateUser_PhoneAlreadyExistsByAnotherUser() {
        // Arrange
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(1L); // phone already exists

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateUser(1L, updateUserRequest);
        });

        assertEquals(ResultCode.PHONE_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(userMapper).selectById(1L);
        verify(userMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void testUpdateUser_EmailAlreadyExistsByAnotherUser() {
        // Arrange
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)  // phone check passes
                .thenReturn(1L); // email already exists

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.updateUser(1L, updateUserRequest);
        });

        assertEquals(ResultCode.EMAIL_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(userMapper).selectById(1L);
        verify(userMapper, times(2)).selectCount(any(LambdaQueryWrapper.class));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void testChangePassword_Success() {
        // Arrange
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // Act
        userService.changePassword(1L, changePasswordRequest);

        // Assert
        verify(userMapper).selectById(1L);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void testChangePassword_UserNotFound() {
        // Arrange
        when(userMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.changePassword(999L, changePasswordRequest);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(userMapper).selectById(999L);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void testChangePassword_OldPasswordIncorrect() {
        // Arrange
        changePasswordRequest.setOldPassword("wrongpassword");
        when(userMapper.selectById(1L)).thenReturn(testUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.changePassword(1L, changePasswordRequest);
        });

        assertEquals(ResultCode.OLD_PASSWORD_INCORRECT.getCode(), exception.getCode());
        verify(userMapper).selectById(1L);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void testChangePassword_NewPasswordSameAsOld() {
        // Arrange
        changePasswordRequest.setNewPassword("password123"); // same as old
        when(userMapper.selectById(1L)).thenReturn(testUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.changePassword(1L, changePasswordRequest);
        });

        assertEquals(ResultCode.NEW_PASSWORD_SAME_AS_OLD.getCode(), exception.getCode());
        verify(userMapper).selectById(1L);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void testDeleteUser_Success() {
        // Arrange
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.deleteById(1L)).thenReturn(1);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userMapper).selectById(1L);
        verify(userMapper).deleteById(1L);
    }

    @Test
    void testDeleteUser_UserNotFound() {
        // Arrange
        when(userMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.deleteUser(999L);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(userMapper).selectById(999L);
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    void testGetUserById_Success() {
        // Arrange
        when(userMapper.selectById(1L)).thenReturn(testUser);

        // Act
        UserInfoResponse response = userService.getUserById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getPhone(), response.getPhone());
        assertEquals(testUser.getUserType(), response.getUserType());
        assertEquals(testUser.getStatus(), response.getStatus());

        verify(userMapper).selectById(1L);
    }

    @Test
    void testGetUserById_UserNotFound() {
        // Arrange
        when(userMapper.selectById(999L)).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getUserById(999L);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(userMapper).selectById(999L);
    }

    @Test
    void testGetUserByUsername_Success() {
        // Arrange
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        // Act
        UserInfoResponse response = userService.getUserByUsername("testuser");

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals(testUser.getEmail(), response.getEmail());

        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetUserByUsername_UserNotFound() {
        // Arrange
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.getUserByUsername("nonexistent");
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetUserList_WithFilters() {
        // Arrange
        User user2 = new User();
        user2.setId(2L);
        user2.setUserId("U789012");
        user2.setUsername("testuser2");
        user2.setPassword(passwordEncoder.encode("password123"));
        user2.setEmail("test2@example.com");
        user2.setPhone("13800138003");
        user2.setUserType("STUDENT");
        user2.setStatus("ACTIVE");
        user2.setCreatedAt(LocalDateTime.now());

        Page<User> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(Arrays.asList(testUser, user2));

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // Act
        IPage<UserInfoResponse> result = userService.getUserList(1, 10, "test", "STUDENT", "ACTIVE");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals("testuser", result.getRecords().get(0).getUsername());
        assertEquals("testuser2", result.getRecords().get(1).getUsername());

        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetUserList_WithoutFilters() {
        // Arrange
        Page<User> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(Arrays.asList(testUser));

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // Act
        IPage<UserInfoResponse> result = userService.getUserList(1, 10, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("testuser", result.getRecords().get(0).getUsername());

        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void testGetUserList_EmptyResult() {
        // Arrange
        Page<User> mockPage = new Page<>(1, 10, 0);
        mockPage.setRecords(Arrays.asList());

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // Act
        IPage<UserInfoResponse> result = userService.getUserList(1, 10, "nonexistent", null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getRecords().size());

        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
}
