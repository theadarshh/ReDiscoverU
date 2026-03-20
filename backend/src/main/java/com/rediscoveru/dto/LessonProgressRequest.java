package com.rediscoveru.dto;

import lombok.Data;

/** Sent by client to record/update progress on a single lesson */
@Data
public class LessonProgressRequest {

    /** ID of ProgramContent or UploadedFile */
    private Long contentId;

    /** "CONTENT" or "FILE" */
    private String contentType;

    private Long programId;
    private String contentTitle;

    /** Current watch position in seconds (videos) */
    private Integer watchedSeconds;

    /** Total duration in seconds (videos) */
    private Integer totalSeconds;

    /**
     * If true, mark as COMPLETED regardless of watchedSeconds.
     * Used for PDFs, articles, and manual completion button.
     */
    private Boolean markCompleted;
}
