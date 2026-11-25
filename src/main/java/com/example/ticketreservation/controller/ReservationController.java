package com.example.ticketreservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.service.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping({"", "/"})
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ReservationResponse> createReservation(
			@Valid @RequestBody ReservationRequest request,
			Authentication authentication) {
		ReservationResponse response = reservationService.createReservation(
				request,
				authentication.getName());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/my")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<List<ReservationResponse>> getMyReservations(
			Authentication authentication) {
		return ResponseEntity.ok(
				reservationService.getMyReservations(authentication.getName()));
	}

	@GetMapping({"", "/"})
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
	public ResponseEntity<List<ReservationResponse>> getReservations(
			Authentication authentication) {
		return ResponseEntity.ok(reservationService.getReservations(
				authentication.getName(),
				isAdmin(authentication)));
	}

	@PutMapping("/{id}/cancel")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
	public ResponseEntity<ReservationResponse> cancelReservation(
			@PathVariable String id,
			Authentication authentication) {
		return cancel(id, authentication);
	}

	@PatchMapping("/{id}/cancel")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
	public ResponseEntity<ReservationResponse> patchCancelReservation(
			@PathVariable String id,
			Authentication authentication) {
		return cancel(id, authentication);
	}

	private ResponseEntity<ReservationResponse> cancel(String id, Authentication authentication) {
		ReservationResponse response = reservationService.cancelReservation(
				id,
				authentication.getName(),
				isAdmin(authentication));
		return ResponseEntity.ok(response);
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}
}
