package com.rediscoveru.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private Long   programId;
    private Long   contentId;
    private String contentType;    // "CONTENT" | "FILE"
    private String commentText;
    private Long   parentId;       // null for top-level, set for reply
}
