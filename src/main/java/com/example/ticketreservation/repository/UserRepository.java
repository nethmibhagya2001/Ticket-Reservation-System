package com.example.ticketreservation.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.ticketreservation.model.AppUser;

public interface UserRepository extends MongoRepository<AppUser, String> {

	Optional<AppUser> findByUsername(String username);

	boolean existsByUsername(String username);
}
