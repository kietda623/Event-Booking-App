package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ticket_tiers",
        indexes = @Index(name = "idx_ticket_tiers_event", columnList = "event_id")
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    private String name;

    private Double price;

    private Integer totalQuantity;

    private Integer soldQuantity;

    @Column(length = 1000)
    private String description;

    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (soldQuantity == null) {
            soldQuantity = 0;
        }
        createdAt = LocalDateTime.now();
    }
}
