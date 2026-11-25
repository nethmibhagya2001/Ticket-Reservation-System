package com.example.ticketreservation.dto;

import java.time.LocalDateTime;

import com.example.ticketreservation.model.ReservationStatus;

public record ReservationResponse(
		String id,
		String reservationCode,
		String eventId,
		String eventName,
		String customerUsername,
		int seats,
		double totalAmount,
		ReservationStatus status,
		LocalDateTime createdAt) {
}
