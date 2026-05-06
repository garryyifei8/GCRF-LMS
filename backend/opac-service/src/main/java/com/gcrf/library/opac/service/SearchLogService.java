package com.gcrf.library.opac.service;

public interface SearchLogService {
    /** Fire-and-forget. Blank keywords are dropped silently. */
    void recordAsync(String keyword, String clientIp, long resultCount);
}
