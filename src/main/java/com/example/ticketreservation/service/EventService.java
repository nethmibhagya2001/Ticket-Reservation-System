package com.example.ticketreservation.service;

import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;

@Service
public class EventService {

	private final EventRepository eventRepository;
	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final MongoTemplate mongoTemplate;

	public EventService(
			EventRepository eventRepository,
			ReservationRepository reservationRepository,
			UserRepository userRepository,
			MongoTemplate mongoTemplate) {
		this.eventRepository = eventRepository;
		this.reservationRepository = reservationRepository;
		this.userRepository = userRepository;
		this.mongoTemplate = mongoTemplate;
	}

	public EventResponse createEvent(EventRequest request, String organizerUsername) {
		AppUser organizer = userRepository.findByUsername(organizerUsername)
				.orElseThrow(() -> ApiException.notFound("Organizer not found"));
		Event event = Event.builder()
				.name(request.name())
				.description(request.description())
				.venue(request.venue())
				.eventDate(request.eventDate())
				.totalSeats(request.totalSeats())
				.availableSeats(request.totalSeats())
				.ticketPrice(request.ticketPrice())
				.organizerId(organizer.getId())
				.organizerUsername(organizer.getUsername())
				.build();
		return toEventResponse(eventRepository.save(event));
	}

	public List<EventResponse> getAllEvents(Optional<String> name) {
		List<Event> events = name.filter(value -> !value.isBlank())
				.map(eventRepository::findByNameContainingIgnoreCase)
				.orElseGet(eventRepository::findAll);
		return events.stream().map(this::toEventResponse).toList();
	}

	public EventResponse getEventById(String id) {
		return toEventResponse(findEventById(id));
	}

	public EventResponse updateEvent(
			String id,
			EventRequest request,
			String currentUsername,
			boolean isAdmin) {
		Event event = findEventById(id);
		if (!isAdmin && !event.getOrganizerUsername().equals(currentUsername)) {
			throw ApiException.forbidden("You do not own this event");
		}

		int soldSeats = Math.max(0, event.getTotalSeats() - event.getAvailableSeats());
		if (request.totalSeats() < soldSeats) {
			throw ApiException.conflict("Total seats cannot be less than seats already reserved");
		}

		event.setName(request.name());
		event.setDescription(request.description());
		event.setVenue(request.venue());
		event.setEventDate(request.eventDate());
		event.setTicketPrice(request.ticketPrice());
		event.setTotalSeats(request.totalSeats());
		event.setAvailableSeats(request.totalSeats() - soldSeats);

		return toEventResponse(eventRepository.save(event));
	}

	public void deleteEvent(String id) {
		Event event = findEventById(id);
		if (reservationRepository.existsByEventIdAndStatus(id, ReservationStatus.CONFIRMED)) {
			throw ApiException.conflict("Cannot delete an event with confirmed reservations");
		}
		eventRepository.delete(event);
	}

	public Event reserveSeatsAtomically(String eventId, int seats) {
		if (seats < 1) {
			throw ApiException.badRequest("Seats must be greater than zero");
		}

		Query query = Query.query(Criteria.where("_id").is(eventId)
				.and("availableSeats").gte(seats));
		Update update = new Update().inc("availableSeats", -seats);
		Event updatedEvent = mongoTemplate.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().returnNew(true),
				Event.class);

		if (updatedEvent == null) {
			if (!eventRepository.existsById(eventId)) {
				throw ApiException.notFound("Event not found");
			}
			throw ApiException.conflict("Not enough available seats");
		}
		return updatedEvent;
	}

	public Event releaseSeatsAtomically(String eventId, int seats) {
		if (seats < 1) {
			throw ApiException.badRequest("Seats must be greater than zero");
		}

		Query query = Query.query(Criteria.where("_id").is(eventId));
		AggregationUpdate update = AggregationUpdate.from(List.of(
				Aggregation.stage(new Document("$set", new Document(
						"availableSeats",
						new Document("$min", List.of(
								new Document("$add", List.of("$availableSeats", seats)),
								"$totalSeats")))))));
		Event updatedEvent = mongoTemplate.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().returnNew(true),
				Event.class);
		if (updatedEvent == null) {
			throw ApiException.notFound("Event not found");
		}
		return updatedEvent;
	}

	private Event findEventById(String id) {
		return eventRepository.findById(id)
				.orElseThrow(() -> ApiException.notFound("Event not found"));
	}

	private EventResponse toEventResponse(Event event) {
		return new EventResponse(
				event.getId(),
				event.getName(),
				event.getDescription(),
				event.getVenue(),
				event.getEventDate(),
				event.getTotalSeats(),
				event.getAvailableSeats(),
				event.getTicketPrice(),
				event.getOrganizerUsername());
	}
}
