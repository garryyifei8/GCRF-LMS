package com.gcrf.library.auth.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

/** Stub — fully implemented (Redis storage + rotation) in Task 9. */
@Service
public class RefreshTokenService {
    public String issue(Long userId) { return UUID.randomUUID().toString(); }
    public Long consume(String token) { throw new UnsupportedOperationException("Implemented in Task 9"); }
    public void revoke(String token) {}
}
