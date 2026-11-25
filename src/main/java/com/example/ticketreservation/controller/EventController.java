package com.example.ticketreservation.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.service.EventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping({"", "/"})
	public ResponseEntity<List<EventResponse>> getAllEvents(
			@RequestParam(required = false) String name) {
		return ResponseEntity.ok(eventService.getAllEvents(Optional.ofNullable(name)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<EventResponse> getEventById(@PathVariable String id) {
		return ResponseEntity.ok(eventService.getEventById(id));
	}

	@PostMapping({"", "/"})
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
	public ResponseEntity<EventResponse> createEvent(
			@Valid @RequestBody EventRequest request,
			Authentication authentication) {
		EventResponse response = eventService.createEvent(request, authentication.getName());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
	public ResponseEntity<EventResponse> updateEvent(
			@PathVariable String id,
			@Valid @RequestBody EventRequest request,
			Authentication authentication) {
		return ResponseEntity.ok(eventService.updateEvent(
				id,
				request,
				authentication.getName(),
				isAdmin(authentication)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
		eventService.deleteEvent(id);
		return ResponseEntity.noContent().build();
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}
}
