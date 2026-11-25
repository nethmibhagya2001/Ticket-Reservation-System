package com.example.ticketreservation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;

class ReservationServiceTest {

	@Test
	void concurrentCancellationDoesNotReleaseSeatsTwice() {
		UserRepository userRepository = mock(UserRepository.class);
		EventRepository eventRepository = mock(EventRepository.class);
		ReservationRepository reservationRepository = mock(ReservationRepository.class);
		EventService eventService = mock(EventService.class);
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		ReservationService service = new ReservationService(
				userRepository,
				eventRepository,
				reservationRepository,
				eventService,
				mongoTemplate);
		Reservation reservation = Reservation.builder()
				.id("reservation-1")
				.customerUsername("customer")
				.status(ReservationStatus.CONFIRMED)
				.build();

		when(reservationRepository.findById("reservation-1")).thenReturn(Optional.of(reservation));
		when(mongoTemplate.findAndModify(
				any(Query.class),
				any(Update.class),
				any(FindAndModifyOptions.class),
				eq(Reservation.class))).thenReturn(null);
		when(reservationRepository.existsById("reservation-1")).thenReturn(true);

		ApiException exception = assertThrows(
				ApiException.class,
				() -> service.cancelReservation("reservation-1", "customer", false));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
		verify(eventService, never()).releaseSeatsAtomically(anyString(), anyInt());
	}

	@Test
	void failedReservationSaveRestoresReservedSeats() {
		UserRepository userRepository = mock(UserRepository.class);
		EventRepository eventRepository = mock(EventRepository.class);
		ReservationRepository reservationRepository = mock(ReservationRepository.class);
		EventService eventService = mock(EventService.class);
		MongoTemplate mongoTemplate = mock(MongoTemplate.class);
		ReservationService service = new ReservationService(
				userRepository,
				eventRepository,
				reservationRepository,
				eventService,
				mongoTemplate);
		AppUser customer = AppUser.builder()
				.id("customer-1")
				.username("customer")
				.role(Role.CUSTOMER)
				.build();
		Event event = Event.builder()
				.id("event-1")
				.ticketPrice(25.0)
				.build();

		when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
		when(reservationRepository.existsByReservationCode(anyString())).thenReturn(false);
		when(eventService.reserveSeatsAtomically("event-1", 2)).thenReturn(event);
		when(reservationRepository.save(any(Reservation.class)))
				.thenThrow(new IllegalStateException("save failed"));

		assertThrows(
				IllegalStateException.class,
				() -> service.createReservation(new ReservationRequest("event-1", 2), "customer"));

		verify(eventService).releaseSeatsAtomically("event-1", 2);
	}
}
