package com.example.ticketreservation.service;

import org.springframework.stereotype.Service;

import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;

@Service
public class AdminService {

	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final ReservationRepository reservationRepository;

	public AdminService(
			UserRepository userRepository,
			EventRepository eventRepository,
			ReservationRepository reservationRepository) {
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.reservationRepository = reservationRepository;
	}

	public StatsResponse getStats() {
		var confirmedReservations = reservationRepository.findAll().stream()
				.filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
				.toList();
		long totalTicketsSold = confirmedReservations.stream()
				.mapToLong(reservation -> reservation.getSeats())
				.sum();
		double totalRevenue = confirmedReservations.stream()
				.mapToDouble(reservation -> reservation.getTotalAmount())
				.sum();

		return new StatsResponse(
				userRepository.count(),
				eventRepository.count(),
				totalTicketsSold,
				totalRevenue);
	}
}
