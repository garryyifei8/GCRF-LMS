package com.gcrf.library.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void resolveSearchPath_returnsRegionOnly_whenNoTenant() {
        assertThat(TenantContext.resolveSearchPath()).isEqualTo("gcrf_region");
    }

    @Test
    void resolveSearchPath_prependsTenant_whenSet() {
        TenantContext.setTenant("school_001");
        assertThat(TenantContext.resolveSearchPath()).isEqualTo("school_001, gcrf_region");
    }

    @Test
    void clear_removesTenant() {
        TenantContext.setTenant("school_001");
        TenantContext.clear();
        assertThat(TenantContext.getTenant()).isNull();
    }

    @Test
    void threadLocal_isolatesAcrossThreads() throws Exception {
        TenantContext.setTenant("school_001");
        Thread other = new Thread(() -> {
            assertThat(TenantContext.getTenant()).isNull();
        });
        other.start();
        other.join();
        assertThat(TenantContext.getTenant()).isEqualTo("school_001");
    }
}
