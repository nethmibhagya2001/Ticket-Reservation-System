package com.example.ticketreservation.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.UserRepository;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

	private final UserRepository userRepository;
	private final EventRepository eventRepository;
	private final PasswordEncoder passwordEncoder;
	private final Environment environment;

	public DataLoader(
			UserRepository userRepository,
			EventRepository eventRepository,
			PasswordEncoder passwordEncoder,
			Environment environment) {
		this.userRepository = userRepository;
		this.eventRepository = eventRepository;
		this.passwordEncoder = passwordEncoder;
		this.environment = environment;
	}

	@Override
	public void run(String... args) {
		ensureUser("admin", "System Admin", Role.ADMIN, "APP_SEED_ADMIN_PASSWORD");
		AppUser organizer = ensureUser(
				"organizer",
				"Sample Organizer",
				Role.ORGANIZER,
				"APP_SEED_ORGANIZER_PASSWORD");
		ensureUser("customer", "Sample Customer", Role.CUSTOMER, "APP_SEED_CUSTOMER_PASSWORD");

		seedSampleEvent(
				"Harbor Lights Jazz Festival",
				"An evening of live jazz featuring local and touring musicians.",
				"Harborfront Amphitheatre",
				LocalDateTime.now().plusDays(30).withHour(19).withMinute(0).withSecond(0).withNano(0),
				850,
				48.50,
				organizer);
		seedSampleEvent(
				"Riverside Spring Food Festival",
				"A weekend celebration of regional food, craft producers, and live entertainment.",
				"Riverside Exhibition Grounds",
				LocalDateTime.now().plusDays(90).withHour(11).withMinute(0).withSecond(0).withNano(0),
				1200,
				22.00,
				organizer);
	}

	private AppUser ensureUser(
			String username,
			String fullName,
			Role role,
			String passwordEnvironmentVariable) {
		return userRepository.findByUsername(username)
				.orElseGet(() -> {
					String password = environment.getProperty(passwordEnvironmentVariable);
					if (password == null || password.isBlank()) {
						throw new IllegalStateException(
								"Set " + passwordEnvironmentVariable
										+ " to seed the " + username + " account");
					}
					return userRepository.save(AppUser.builder()
							.username(username)
							.passwordHash(passwordEncoder.encode(password))
							.fullName(fullName)
							.role(role)
							.build());
				});
	}

	private void seedSampleEvent(
			String name,
			String description,
			String venue,
			LocalDateTime eventDate,
			int totalSeats,
			double ticketPrice,
			AppUser organizer) {
		boolean exists = eventRepository.findByNameContainingIgnoreCase(name).stream()
				.anyMatch(event -> name.equalsIgnoreCase(event.getName()));
		if (exists) {
			return;
		}

		eventRepository.save(Event.builder()
				.name(name)
				.description(description)
				.venue(venue)
				.eventDate(eventDate)
				.totalSeats(totalSeats)
				.availableSeats(totalSeats)
				.ticketPrice(ticketPrice)
				.organizerId(organizer.getId())
				.organizerUsername(organizer.getUsername())
				.build());
	}
}
