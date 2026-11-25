package com.example.ticketreservation.service;

import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;

@Service
public class ReservationService {

	private static final int RESERVATION_CODE_ATTEMPTS = 10;

	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final ReservationRepository reservationRepository;
	private final EventService eventService;

	public ReservationService(
			UserRepository userRepository,
			EventRepository eventRepository,
			ReservationRepository reservationRepository,
			EventService eventService) {
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.reservationRepository = reservationRepository;
		this.eventService = eventService;
	}

	public ReservationResponse createReservation(
			ReservationRequest request,
			String customerUsername) {
		AppUser customer = userRepository.findByUsername(customerUsername)
				.orElseThrow(() -> ApiException.notFound("Customer not found"));
		Event event = eventService.reserveSeatsAtomically(request.eventId(), request.seats());
		String reservationCode = generateUniqueReservationCode();

		Reservation reservation = Reservation.builder()
				.reservationCode(reservationCode)
				.eventId(event.getId())
				.customerId(customer.getId())
				.customerUsername(customer.getUsername())
				.seats(request.seats())
				.totalAmount(event.getTicketPrice() * request.seats())
				.status(ReservationStatus.CONFIRMED)
				.createdAt(LocalDateTime.now())
				.build();
		return toReservationResponse(reservationRepository.save(reservation));
	}

	public List<ReservationResponse> getMyReservations(String customerUsername) {
		return reservationRepository.findByCustomerUsernameOrderByCreatedAtDesc(customerUsername)
				.stream()
				.map(this::toReservationResponse)
				.toList();
	}

	public List<ReservationResponse> getReservations(String currentUsername, boolean isAdmin) {
		List<Reservation> reservations;
		if (isAdmin) {
			reservations = reservationRepository.findAll();
		} else {
			AppUser currentUser = userRepository.findByUsername(currentUsername)
					.orElseThrow(() -> ApiException.notFound("User not found"));
			if (currentUser.getRole() != Role.ORGANIZER) {
				throw ApiException.forbidden("Only admins and organizers can list these reservations");
			}

			List<String> eventIds = eventRepository.findByOrganizerUsername(currentUsername).stream()
					.map(Event::getId)
					.toList();
			reservations = eventIds.isEmpty()
					? List.of()
					: reservationRepository.findByEventIdIn(eventIds);
		}
		return reservations.stream().map(this::toReservationResponse).toList();
	}

	public ReservationResponse cancelReservation(
			String reservationId,
			String currentUsername,
			boolean isAdmin) {
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> ApiException.notFound("Reservation not found"));
		if (!isAdmin && !reservation.getCustomerUsername().equals(currentUsername)) {
			throw ApiException.forbidden("You do not own this reservation");
		}
		if (reservation.getStatus() == ReservationStatus.CANCELLED) {
			throw ApiException.conflict("Reservation is already cancelled");
		}

		reservation.setStatus(ReservationStatus.CANCELLED);
		Reservation cancelledReservation = reservationRepository.save(reservation);
		eventService.releaseSeatsAtomically(
				reservation.getEventId(),
				reservation.getSeats());
		return toReservationResponse(cancelledReservation);
	}

	private String generateUniqueReservationCode() {
		for (int attempt = 0; attempt < RESERVATION_CODE_ATTEMPTS; attempt++) {
			byte[] randomBytes = new byte[4];
			ThreadLocalRandom.current().nextBytes(randomBytes);
			String code = "TKT-" + HexFormat.of().withUpperCase().formatHex(randomBytes);
			if (!reservationRepository.existsByReservationCode(code)) {
				return code;
			}
		}
		throw ApiException.conflict("Unable to generate a unique reservation code");
	}

	private ReservationResponse toReservationResponse(Reservation reservation) {
		String eventName = eventRepository.findById(reservation.getEventId())
				.map(Event::getName)
				.orElse("Event no longer available");
		return new ReservationResponse(
				reservation.getId(),
				reservation.getReservationCode(),
				reservation.getEventId(),
				eventName,
				reservation.getCustomerUsername(),
				reservation.getSeats(),
				reservation.getTotalAmount(),
				reservation.getStatus(),
				reservation.getCreatedAt());
	}
}
