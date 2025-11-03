package com.gcrf.library.common.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcrf.library.common.TestMybatisApplication;
import com.gcrf.library.common.config.MybatisPlusConfiguration;
import com.gcrf.library.common.handler.MetaObjectHandlerImpl;
import com.gcrf.library.common.integration.entity.TestUser;
import com.gcrf.library.common.integration.mapper.TestUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBatis-Plus集成测试
 * 测试BaseEntity、MetaObjectHandler、Pagination等功能
 *
 * @author Claude Code
 * @date 2025-10-27
 */
@SpringBootTest(classes = {TestMybatisApplication.class, MybatisPlusConfiguration.class, MetaObjectHandlerImpl.class})
@Sql(scripts = "/schema.sql")
class MybatisPlusIntegrationTest {

    @Autowired
    private TestUserMapper testUserMapper;

    @BeforeEach
    void setUp() {
        // Clean up before each test - delete all records individually to avoid BlockAttackInnerInterceptor
        List<TestUser> allUsers = testUserMapper.selectList(null);
        allUsers.forEach(user -> testUserMapper.deleteById(user.getId()));
    }

    @Test
    void testInsertWithAutoFillTimestamps() {
        // Arrange
        TestUser user = new TestUser();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setAge(30);

        // Act
        int result = testUserMapper.insert(user);

        // Assert
        assertEquals(1, result);
        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(user.getUpdatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testUpdateWithAutoFillTimestamps() throws InterruptedException {
        // Arrange
        TestUser user = new TestUser();
        user.setUsername("jane_doe");
        user.setEmail("jane@example.com");
        user.setAge(25);
        testUserMapper.insert(user);

        LocalDateTime originalUpdatedAt = user.getUpdatedAt();
        Long userId = user.getId();
        Thread.sleep(1000); // Wait to ensure different timestamp

        // Act - Create a new object to update, simulating partial update scenario
        TestUser updateUser = new TestUser();
        updateUser.setId(userId);
        updateUser.setAge(26);
        int result = testUserMapper.updateById(updateUser);

        // Assert
        assertEquals(1, result);
        TestUser updated = testUserMapper.selectById(userId);
        assertNotNull(updated.getUpdatedAt());
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testLogicDelete() {
        // Arrange
        TestUser user = new TestUser();
        user.setUsername("to_delete");
        user.setEmail("delete@example.com");
        user.setAge(40);
        testUserMapper.insert(user);
        Long userId = user.getId();

        // Act
        int result = testUserMapper.deleteById(userId);

        // Assert
        assertEquals(1, result);
        // Should not find the deleted user in normal query
        TestUser deleted = testUserMapper.selectById(userId);
        assertNull(deleted);
    }

    @Test
    void testPagination() {
        // Arrange - Insert 15 test users
        for (int i = 1; i <= 15; i++) {
            TestUser user = new TestUser();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + i);
            testUserMapper.insert(user);
        }

        // Act - Query page 1 with size 10
        Page<TestUser> page = new Page<>(1, 10);
        IPage<TestUser> result = testUserMapper.selectPage(page, null);

        // Assert
        assertEquals(15, result.getTotal());
        assertEquals(10, result.getRecords().size());
        assertEquals(2, result.getPages());
    }

    @Test
    void testPaginationSecondPage() {
        // Arrange - Insert 15 test users
        for (int i = 1; i <= 15; i++) {
            TestUser user = new TestUser();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + i);
            testUserMapper.insert(user);
        }

        // Act - Query page 2 with size 10
        Page<TestUser> page = new Page<>(2, 10);
        IPage<TestUser> result = testUserMapper.selectPage(page, null);

        // Assert
        assertEquals(15, result.getTotal());
        assertEquals(5, result.getRecords().size()); // Only 5 records on page 2
    }

    @Test
    void testPaginationWithMaxLimit() {
        // Arrange - Insert 150 test users (exceeds max limit of 100)
        for (int i = 1; i <= 150; i++) {
            TestUser user = new TestUser();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + (i % 50));
            testUserMapper.insert(user);
        }

        // Act - Try to query more than max limit
        Page<TestUser> page = new Page<>(1, 150);
        IPage<TestUser> result = testUserMapper.selectPage(page, null);

        // Assert - Should be limited to 100
        assertEquals(150, result.getTotal());
        assertEquals(100, result.getRecords().size());
    }

    @Test
    void testLambdaQueryWrapper() {
        // Arrange
        TestUser user1 = new TestUser();
        user1.setUsername("alice");
        user1.setEmail("alice@example.com");
        user1.setAge(25);
        testUserMapper.insert(user1);

        TestUser user2 = new TestUser();
        user2.setUsername("bob");
        user2.setEmail("bob@example.com");
        user2.setAge(30);
        testUserMapper.insert(user2);

        // Act
        LambdaQueryWrapper<TestUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestUser::getUsername, "alice");
        List<TestUser> result = testUserMapper.selectList(wrapper);

        // Assert
        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getUsername());
        assertEquals(25, result.get(0).getAge());
    }

    @Test
    void testComplexQuery() {
        // Arrange
        for (int i = 1; i <= 10; i++) {
            TestUser user = new TestUser();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + i);
            testUserMapper.insert(user);
        }

        // Act - Query users with age > 25
        LambdaQueryWrapper<TestUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(TestUser::getAge, 25)
               .orderByDesc(TestUser::getAge);
        List<TestUser> result = testUserMapper.selectList(wrapper);

        // Assert
        assertEquals(5, result.size()); // Ages 26-30
        assertEquals(30, result.get(0).getAge()); // Descending order
    }

    @Test
    void testSelectOne() {
        // Arrange
        TestUser user = new TestUser();
        user.setUsername("unique_user");
        user.setEmail("unique@example.com");
        user.setAge(35);
        testUserMapper.insert(user);

        // Act
        LambdaQueryWrapper<TestUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TestUser::getUsername, "unique_user");
        TestUser result = testUserMapper.selectOne(wrapper);

        // Assert
        assertNotNull(result);
        assertEquals("unique_user", result.getUsername());
        assertEquals(35, result.getAge());
    }

    @Test
    void testCount() {
        // Arrange
        for (int i = 1; i <= 7; i++) {
            TestUser user = new TestUser();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + i);
            testUserMapper.insert(user);
        }

        // Act
        Long count = testUserMapper.selectCount(null);

        // Assert
        assertEquals(7, count);
    }

    @Test
    void testBatchInsert() {
        // This tests that BaseEntity works correctly for multiple inserts
        // Arrange & Act
        for (int i = 1; i <= 5; i++) {
            TestUser user = new TestUser();
            user.setUsername("batch_user" + i);
            user.setEmail("batch" + i + "@example.com");
            user.setAge(20 + i);
            testUserMapper.insert(user);
        }

        // Assert
        Long count = testUserMapper.selectCount(null);
        assertEquals(5, count);

        // Verify all have timestamps
        List<TestUser> users = testUserMapper.selectList(null);
        users.forEach(user -> {
            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getUpdatedAt());
        });
    }
}
