package com.rediscoveru.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name = "homepage_videos") @Data
public class HomepageVideo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String youtubeUrl;

    private boolean active = true;

    private int displayOrder = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    /** Converts any youtu.be or youtube.com/watch?v= URL to embed format. */
    @Transient
    public String getEmbedUrl() {
        if (youtubeUrl == null) return "";
        String id = extractVideoId(youtubeUrl);
        return id != null ? "https://www.youtube.com/embed/" + id : youtubeUrl;
    }

    private String extractVideoId(String url) {
        // youtu.be/ID
        if (url.contains("youtu.be/")) {
            String after = url.substring(url.indexOf("youtu.be/") + 9);
            return after.split("[?&]")[0];
        }
        // youtube.com/watch?v=ID
        if (url.contains("v=")) {
            String after = url.substring(url.indexOf("v=") + 2);
            return after.split("[?&]")[0];
        }
        // already embed format
        if (url.contains("/embed/")) {
            String after = url.substring(url.indexOf("/embed/") + 7);
            return after.split("[?&]")[0];
        }
        return null;
    }
}
