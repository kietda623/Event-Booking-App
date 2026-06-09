package com.eventbooking.dto.tier;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketTierRequest {
    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0.0")
    private Double price;

    @NotNull
    @Min(0)
    private Integer totalQuantity;

    private String description;
}
