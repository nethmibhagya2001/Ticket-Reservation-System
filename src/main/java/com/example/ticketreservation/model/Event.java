package com.example.ticketreservation.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "events")
public class Event {

	@Id
	private String id;

	private String name;
	private String description;
	private String venue;
	private LocalDateTime eventDate;
	private int totalSeats;
	private int availableSeats;
	private double ticketPrice;
	private String organizerId;
	private String organizerUsername;
}
