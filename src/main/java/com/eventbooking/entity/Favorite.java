package com.eventbooking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}),
        indexes = @jakarta.persistence.Index(name = "idx_favorites_user_event", columnList = "user_id,event_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
