package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_seats_event_number", columnNames = {"event_id", "seat_number"}),
        indexes = {
                @Index(name = "idx_seats_event", columnList = "event_id"),
                @Index(name = "idx_seats_hold_expiry", columnList = "status,held_until")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "tier_id")
    private TicketTier tier;

    @Column(name = "seat_number")
    private String seatNumber;

    @Column(name = "seat_row")
    private String row;

    @Column(name = "seat_col")
    private Integer col;

    private String status;

    private LocalDateTime heldUntil;

    private Long heldByUserId;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = "AVAILABLE";
        }
    }
}
