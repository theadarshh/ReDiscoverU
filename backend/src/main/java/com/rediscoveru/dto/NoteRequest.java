package com.rediscoveru.dto;

import lombok.Data;

@Data
public class NoteRequest {
    private Long   programId;
    private Long   contentId;
    private String contentType;   // "CONTENT" | "FILE"
    private String contentTitle;
    private String noteText;
    private Integer timestampSeconds; // optional
}
