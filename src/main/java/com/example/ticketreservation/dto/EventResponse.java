package com.example.ticketreservation.dto;

import java.time.LocalDateTime;

public record EventResponse(
		String id,
		String name,
		String description,
		String venue,
		LocalDateTime eventDate,
		int totalSeats,
		int availableSeats,
		double ticketPrice,
		String organizerUsername) {
}
