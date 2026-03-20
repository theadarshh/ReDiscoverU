package com.rediscoveru.dto;

import lombok.Data;

@Data
public class BookmarkRequest {
    private Long    programId;
    private Long    contentId;
    private String  contentType;    // "CONTENT" | "FILE"
    private String  contentTitle;
    private Integer timestampSeconds; // optional
    private String  label;            // optional
}
