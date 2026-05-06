package com.gcrf.library.opac.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionVO {
    private String text;     // suggested title or author
    private String type;     // "title" | "author" | "isbn"
    private String isbn;     // populated when type=isbn or convenient
    private long count;      // number of distinct books matching
}
