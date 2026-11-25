package com.example.ticketreservation.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record EventRequest(
		@NotBlank String name,
		String description,
		@NotBlank String venue,
		@NotNull @Future LocalDateTime eventDate,
		@Min(1) int totalSeats,
		@PositiveOrZero double ticketPrice) {
}
