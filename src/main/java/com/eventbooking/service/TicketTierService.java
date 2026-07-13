package com.eventbooking.service;

import com.eventbooking.dto.tier.TicketTierRequest;
import com.eventbooking.dto.tier.TicketTierResponse;

public interface TicketTierService {
    TicketTierResponse create(Long eventId, TicketTierRequest request);
    TicketTierResponse update(Long eventId, Long tierId, TicketTierRequest request);
    void delete(Long eventId, Long tierId);
}
