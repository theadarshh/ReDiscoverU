package com.rediscoveru.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity @Table(name = "live_sessions") @Data
public class LiveSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private Program program;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String meetingLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.ONE_TIME;

    /** For ONE_TIME / SPECIFIC_DATE_RANGE: exact date of occurrence */
    private LocalDate specificDate;

    /** For CUSTOM_DAYS: comma-separated e.g. "MON,WED,FRI" */
    private String recurrenceDays;

    /** Session start time, e.g. 05:30 */
    private LocalTime startTime;

    /** Session end time, e.g. 06:30 */
    private LocalTime endTime;

    /** Timezone label e.g. "Asia/Kolkata" */
    @Column(length = 60)
    private String timezone = "Asia/Kolkata";

    /** Recurrence valid from date */
    private LocalDate validFrom;

    /** Recurrence valid to date */
    private LocalDate validTo;

    private boolean active = true;

    /** Legacy field kept for backward compatibility */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum RecurrenceType {
        ONE_TIME, DAILY, WEEKDAYS, CUSTOM_DAYS, SPECIFIC_DATE_RANGE
    }
}
