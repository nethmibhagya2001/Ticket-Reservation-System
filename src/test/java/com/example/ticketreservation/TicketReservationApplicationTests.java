package com.example.ticketreservation;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "app.seed.enabled=false")
class TicketReservationApplicationTests {

	@DynamicPropertySource
	static void configureJwtSecret(DynamicPropertyRegistry registry) {
		byte[] secret = new byte[64];
		new SecureRandom().nextBytes(secret);
		registry.add(
				"app.jwt.secret",
				() -> Base64.getEncoder().encodeToString(secret));
	}

	@Test
	void contextLoads() {
	}
}
