package com.eventbooking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticketCode;

    private String ticketType;

    private String seatNumber;

    private String status;

    private Boolean checkedIn;

    private LocalDateTime checkedInAt;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @PrePersist
    void prePersist() {
        if (status == null) {
            status = "ACTIVE";
        }
        if (checkedIn == null) {
            checkedIn = false;
        }
    }
}
