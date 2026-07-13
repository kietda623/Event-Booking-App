package com.eventbooking.service.impl;

import com.eventbooking.dto.tier.TicketTierRequest;
import com.eventbooking.dto.tier.TicketTierResponse;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.TicketTier;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.TicketTierRepository;
import com.eventbooking.service.TicketTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketTierServiceImpl implements TicketTierService {
    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public TicketTierResponse create(Long eventId, TicketTierRequest request) {
        Event event = findEvent(eventId);
        TicketTier tier = new TicketTier();
        tier.setEvent(event);
        apply(tier, request);
        tier.setSoldQuantity(0);
        return toResponse(ticketTierRepository.save(tier));
    }

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public TicketTierResponse update(Long eventId, Long tierId, TicketTierRequest request) {
        TicketTier tier = findTier(eventId, tierId);
        int sold = tier.getSoldQuantity() != null ? tier.getSoldQuantity() : 0;
        if (request.getTotalQuantity() < sold) {
            throw new BusinessException("TIER_QUANTITY_BELOW_SOLD",
                    "Total quantity cannot be lower than sold quantity",
                    HttpStatus.CONFLICT);
        }
        apply(tier, request);
        return toResponse(ticketTierRepository.save(tier));
    }

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public void delete(Long eventId, Long tierId) {
        TicketTier tier = findTier(eventId, tierId);
        int sold = tier.getSoldQuantity() != null ? tier.getSoldQuantity() : 0;
        if (sold > 0) {
            throw new BusinessException("TIER_ALREADY_SOLD",
                    "Ticket tier cannot be deleted after tickets are sold",
                    HttpStatus.CONFLICT);
        }
        ticketTierRepository.delete(tier);
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("EVENT_NOT_FOUND", "Event not found"));
    }

    private TicketTier findTier(Long eventId, Long tierId) {
        return ticketTierRepository.findByIdAndEventId(tierId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("TIER_NOT_FOUND", "Ticket tier not found"));
    }

    private void apply(TicketTier tier, TicketTierRequest request) {
        tier.setName(request.getName());
        tier.setPrice(request.getPrice());
        tier.setTotalQuantity(request.getTotalQuantity());
        tier.setDescription(request.getDescription());
    }

    private TicketTierResponse toResponse(TicketTier tier) {
        int total = tier.getTotalQuantity() != null ? tier.getTotalQuantity() : 0;
        int sold = tier.getSoldQuantity() != null ? tier.getSoldQuantity() : 0;
        return new TicketTierResponse(
                tier.getId(),
                tier.getEvent().getId(),
                tier.getName(),
                tier.getPrice(),
                total,
                sold,
                Math.max(total - sold, 0),
                tier.getDescription(),
                tier.getCreatedAt()
        );
    }
}
