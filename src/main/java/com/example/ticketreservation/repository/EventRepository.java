package com.example.ticketreservation.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.ticketreservation.model.Event;

public interface EventRepository extends MongoRepository<Event, String> {

	List<Event> findByNameContainingIgnoreCase(String name);

	List<Event> findByOrganizerId(String organizerId);

	List<Event> findByOrganizerUsername(String organizerUsername);
}
