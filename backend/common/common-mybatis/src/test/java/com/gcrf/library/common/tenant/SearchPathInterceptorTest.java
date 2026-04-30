package com.gcrf.library.common.tenant;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SearchPathInterceptorTest {

    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void intercept_setsSearchPathToRegion_whenNoTenant() throws Throwable {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(conn.createStatement()).thenReturn(stmt);

        Method prepare = StatementHandler.class.getMethod("prepare", Connection.class, Integer.class);
        Invocation inv = new Invocation(mock(StatementHandler.class), prepare, new Object[]{conn, null});

        new SearchPathInterceptor().intercept(inv);

        verify(stmt).execute("SET search_path TO gcrf_region");
    }

    @Test
    void intercept_setsSearchPathToTenantThenRegion_whenTenantSet() throws Throwable {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(conn.createStatement()).thenReturn(stmt);
        TenantContext.setTenant("school_001");

        Method prepare = StatementHandler.class.getMethod("prepare", Connection.class, Integer.class);
        Invocation inv = new Invocation(mock(StatementHandler.class), prepare, new Object[]{conn, null});

        new SearchPathInterceptor().intercept(inv);

        verify(stmt).execute("SET search_path TO school_001, gcrf_region");
    }

    @Test
    void intercept_rejectsUnsafeSchemaName() throws Throwable {
        TenantContext.setTenant("school_001; DROP TABLE foo --");
        SearchPathInterceptor sut = new SearchPathInterceptor();

        assertThat(sut.isSafeSearchPath("school_001; DROP TABLE foo --")).isFalse();
        assertThat(sut.isSafeSearchPath("school_001, gcrf_region")).isTrue();
    }
}
