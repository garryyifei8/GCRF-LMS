package com.gcrf.library.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis-Plus配置类
 *
 * @author 张三
 * @date 2025-10-11
 */
@Configuration
@EnableTransactionManagement
public class MybatisPlusConfiguration {

    /**
     * MyBatis-Plus拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件 - 使用PostgreSQL数据库类型
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInnerInterceptor.setMaxLimit(100L); // 最大单页限制数量
        paginationInnerInterceptor.setOverflow(false); // 溢出总页数后是否进行处理
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }
}
