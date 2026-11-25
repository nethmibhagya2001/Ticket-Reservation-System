package com.example.ticketreservation.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reservations")
public class Reservation {

	@Id
	private String id;

	@Indexed(unique = true)
	private String reservationCode;

	private String eventId;
	private String customerId;
	private String customerUsername;
	private int seats;
	private double totalAmount;
	private ReservationStatus status;
	private LocalDateTime createdAt;
}
