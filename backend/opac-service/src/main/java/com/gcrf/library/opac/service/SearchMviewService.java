package com.gcrf.library.opac.service;

public interface SearchMviewService {
    /** Calls the PG function refresh_book_search_mview() and returns row count. */
    int refresh();
}
