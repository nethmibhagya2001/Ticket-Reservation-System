package com.example.ticketreservation.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.ticketreservation.dto.AuthResponse;
import com.example.ticketreservation.dto.LoginRequest;
import com.example.ticketreservation.dto.RegisterRequest;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.security.JwtService;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw ApiException.conflict("Username already exists");
		}

		boolean admin = isAuthenticatedAdmin();
		if (!admin && (request.role() == Role.ADMIN || request.role() == Role.ORGANIZER)) {
			throw ApiException.forbidden("Only an admin can assign elevated roles");
		}

		Role role = admin && request.role() != null ? request.role() : Role.CUSTOMER;
		AppUser user = AppUser.builder()
				.username(request.username())
				.passwordHash(passwordEncoder.encode(request.password()))
				.fullName(request.fullName())
				.role(role)
				.build();
		AppUser savedUser;
		try {
			savedUser = userRepository.save(user);
		} catch (DuplicateKeyException exception) {
			throw ApiException.conflict("Username already exists");
		}
		return createAuthResponse(savedUser);
	}

	public AuthResponse login(LoginRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));

		AppUser user = userRepository.findByUsername(request.username())
				.orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
		return createAuthResponse(user);
	}

	public UserResponse me(String username) {
		AppUser user = userRepository.findByUsername(username)
				.orElseThrow(() -> ApiException.notFound("User not found"));
		return toUserResponse(user);
	}

	public List<UserResponse> getAllUsers() {
		if (!isAuthenticatedAdmin()) {
			throw ApiException.forbidden("Admin access is required");
		}
		return userRepository.findAll().stream()
				.map(this::toUserResponse)
				.toList();
	}

	private AuthResponse createAuthResponse(AppUser user) {
		return new AuthResponse(
				jwtService.generateToken(user),
				"Bearer",
				jwtService.getExpirationMs(),
				user.getUsername(),
				user.getFullName(),
				user.getRole());
	}

	private UserResponse toUserResponse(AppUser user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getFullName(),
				user.getRole());
	}

	private boolean isAuthenticatedAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.isAuthenticated()
				&& authentication.getAuthorities().stream()
						.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}
}
