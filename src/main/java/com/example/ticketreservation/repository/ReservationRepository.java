package com.example.ticketreservation.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;

public interface ReservationRepository extends MongoRepository<Reservation, String> {

	List<Reservation> findByCustomerUsernameOrderByCreatedAtDesc(String customerUsername);

	List<Reservation> findByEventIdIn(List<String> eventIds);

	boolean existsByEventIdAndStatus(String eventId, ReservationStatus status);

	boolean existsByReservationCode(String reservationCode);
}
