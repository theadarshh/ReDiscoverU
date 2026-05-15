package com.rediscoveru.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "uploaded_files") @Data
public class UploadedFile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private Program program;

    @Column(nullable = false)
    private String title;

    /** Original filename (sanitized) */
    @Column(nullable = false)
    private String fileName;

    /** ENUM: VIDEO, PDF, DOC, PPT, IMAGE, OTHER */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileCategory fileCategory;

    /** MIME type e.g. video/mp4, application/pdf */
    private String contentType;

    /** Relative URL served by backend, e.g. /uploads/programs/3/uuid_file.pdf */
    @Column(nullable = false, length = 512)
    private String filePath;

    /** Absolute disk path for deletion */
    @Column(length = 1024)
    private String diskPath;

    private long fileSizeBytes = 0L;

    private int orderIndex = 0;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    public enum FileCategory {
        VIDEO, PDF, DOC, PPT, IMAGE, OTHER;

        public static FileCategory fromMime(String mime) {
            if (mime == null) return OTHER;
            if (mime.startsWith("video/"))       return VIDEO;
            if (mime.equals("application/pdf"))   return PDF;
            if (mime.contains("word") || mime.contains("opendocument.text")) return DOC;
            if (mime.contains("powerpoint") || mime.contains("presentation")) return PPT;
            if (mime.startsWith("image/"))        return IMAGE;
            return OTHER;
        }

        public static FileCategory fromExtension(String name) {
            if (name == null) return OTHER;
            String lower = name.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")) return VIDEO;
            if (lower.endsWith(".pdf")) return PDF;
            if (lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".odt")) return DOC;
            if (lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".odp")) return PPT;
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp")) return IMAGE;
            return OTHER;
        }
    }
}
