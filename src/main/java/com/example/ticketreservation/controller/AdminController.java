package com.example.ticketreservation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.service.AdminService;
import com.example.ticketreservation.service.AuthService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AuthService authService;
	private final AdminService adminService;

	public AdminController(AuthService authService, AdminService adminService) {
		this.authService = authService;
		this.adminService = adminService;
	}

	@GetMapping("/users")
	public ResponseEntity<List<UserResponse>> getUsers() {
		return ResponseEntity.ok(authService.getAllUsers());
	}

	@GetMapping("/stats")
	public ResponseEntity<StatsResponse> getStats() {
		return ResponseEntity.ok(adminService.getStats());
	}
}
