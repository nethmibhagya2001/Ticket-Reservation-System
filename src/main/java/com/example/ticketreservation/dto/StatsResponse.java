package com.example.ticketreservation.dto;

public record StatsResponse(
		long totalUsers,
		long totalEvents,
		long totalTicketsSold,
		double totalRevenue) {
}
