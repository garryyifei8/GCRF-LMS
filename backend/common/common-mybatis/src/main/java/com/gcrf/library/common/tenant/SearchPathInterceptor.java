package com.gcrf.library.common.tenant;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Connection;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * MyBatis 拦截器：在每个 SQL 执行前 SET search_path 切换 PostgreSQL schema。
 * 使 mapper 写裸 SQL（不带 schema 前缀）即可在多租户下工作。
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare",
               args = {Connection.class, Integer.class})
})
public class SearchPathInterceptor implements Interceptor {

    /** 仅允许 字母/数字/下划线/逗号/空格 ，防止 SQL 注入到 search_path 字符串。 */
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9_, ]+$");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Connection conn = (Connection) invocation.getArgs()[0];
        String path = TenantContext.resolveSearchPath();
        if (!isSafeSearchPath(path)) {
            throw new IllegalStateException("unsafe search_path: " + path);
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + path);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /** 公开供单测验证白名单逻辑。 */
    public boolean isSafeSearchPath(String path) {
        return path != null && SAFE.matcher(path).matches();
    }
}
