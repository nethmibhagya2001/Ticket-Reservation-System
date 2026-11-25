package com.example.ticketreservation.service;

import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

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
	private final MongoTemplate mongoTemplate;

	public ReservationService(
			UserRepository userRepository,
			EventRepository eventRepository,
			ReservationRepository reservationRepository,
			EventService eventService,
			MongoTemplate mongoTemplate) {
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.reservationRepository = reservationRepository;
		this.eventService = eventService;
		this.mongoTemplate = mongoTemplate;
	}

	public ReservationResponse createReservation(
			ReservationRequest request,
			String customerUsername) {
		AppUser customer = userRepository.findByUsername(customerUsername)
				.orElseThrow(() -> ApiException.notFound("Customer not found"));
		String reservationCode = generateUniqueReservationCode();
		Event event = eventService.reserveSeatsAtomically(request.eventId(), request.seats());

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
		Reservation savedReservation;
		try {
			savedReservation = reservationRepository.save(reservation);
		} catch (DuplicateKeyException saveFailure) {
			releaseSeatsAfterFailedSave(event.getId(), request.seats(), saveFailure);
			throw ApiException.conflict("Reservation code collision; please retry");
		} catch (RuntimeException saveFailure) {
			releaseSeatsAfterFailedSave(event.getId(), request.seats(), saveFailure);
			throw saveFailure;
		}
		return toReservationResponse(savedReservation);
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

		Query query = Query.query(Criteria.where("_id").is(reservationId)
				.and("status").is(ReservationStatus.CONFIRMED));
		Reservation cancelledReservation = mongoTemplate.findAndModify(
				query,
				new Update().set("status", ReservationStatus.CANCELLED),
				FindAndModifyOptions.options().returnNew(true),
				Reservation.class);
		if (cancelledReservation == null) {
			if (!reservationRepository.existsById(reservationId)) {
				throw ApiException.notFound("Reservation not found");
			}
			throw ApiException.conflict("Reservation is no longer confirmed");
		}
		eventService.releaseSeatsAtomically(
				cancelledReservation.getEventId(),
				cancelledReservation.getSeats());
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

	private void releaseSeatsAfterFailedSave(String eventId, int seats, RuntimeException failure) {
		try {
			eventService.releaseSeatsAtomically(eventId, seats);
		} catch (RuntimeException releaseFailure) {
			failure.addSuppressed(releaseFailure);
		}
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
