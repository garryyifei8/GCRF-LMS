package com.gcrf.library.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBatis-Plus配置测试
 *
 * @author Claude Code
 * @date 2025-10-27
 */
@SpringBootTest
@ContextConfiguration(classes = MybatisPlusConfiguration.class)
class MybatisPlusConfigurationTest {

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Test
    void testMybatisPlusInterceptorExists() {
        // Assert
        assertNotNull(mybatisPlusInterceptor);
    }

    @Test
    void testPaginationInterceptorConfigured() {
        // Act
        List<?> interceptors = mybatisPlusInterceptor.getInterceptors();

        // Assert
        assertNotNull(interceptors);
        assertTrue(interceptors.stream()
                .anyMatch(i -> i instanceof PaginationInnerInterceptor));
    }

    @Test
    void testOptimisticLockerInterceptorConfigured() {
        // Act
        List<?> interceptors = mybatisPlusInterceptor.getInterceptors();

        // Assert
        assertTrue(interceptors.stream()
                .anyMatch(i -> i instanceof OptimisticLockerInnerInterceptor));
    }

    @Test
    void testBlockAttackInterceptorConfigured() {
        // Act
        List<?> interceptors = mybatisPlusInterceptor.getInterceptors();

        // Assert
        assertTrue(interceptors.stream()
                .anyMatch(i -> i instanceof BlockAttackInnerInterceptor));
    }

    @Test
    void testInterceptorCount() {
        // Act
        List<?> interceptors = mybatisPlusInterceptor.getInterceptors();

        // Assert
        // Should have 3 interceptors: Pagination, OptimisticLocker, BlockAttack
        assertEquals(3, interceptors.size());
    }

    @Test
    void testInterceptorOrder() {
        // Act
        List<?> interceptors = mybatisPlusInterceptor.getInterceptors();

        // Assert
        // Pagination should be first
        assertTrue(interceptors.get(0) instanceof PaginationInnerInterceptor);
        // OptimisticLocker should be second
        assertTrue(interceptors.get(1) instanceof OptimisticLockerInnerInterceptor);
        // BlockAttack should be third
        assertTrue(interceptors.get(2) instanceof BlockAttackInnerInterceptor);
    }

    @Test
    void testPaginationInterceptorMaxLimit() {
        // Act
        List<?> interceptors = mybatisPlusInterceptor.getInterceptors();
        PaginationInnerInterceptor paginationInterceptor = (PaginationInnerInterceptor) interceptors.stream()
                .filter(i -> i instanceof PaginationInnerInterceptor)
                .findFirst()
                .orElse(null);

        // Assert
        assertNotNull(paginationInterceptor);
        assertEquals(100L, paginationInterceptor.getMaxLimit());
    }

    @Test
    void testPaginationInterceptorOverflow() {
        // Act
        List<?> interceptors = mybatisPlusInterceptor.getInterceptors();
        PaginationInnerInterceptor paginationInterceptor = (PaginationInnerInterceptor) interceptors.stream()
                .filter(i -> i instanceof PaginationInnerInterceptor)
                .findFirst()
                .orElse(null);

        // Assert
        assertNotNull(paginationInterceptor);
        assertFalse(paginationInterceptor.isOverflow());
    }
}
