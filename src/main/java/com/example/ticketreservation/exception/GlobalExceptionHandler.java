package com.example.ticketreservation.exception;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.ticketreservation.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(
			ApiException exception,
			HttpServletRequest request) {
		return createResponse(exception.getStatus(), exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		List<String> errors = exception.getBindingResult().getAllErrors().stream()
				.map(error -> {
					String message = error.getDefaultMessage() == null
							? "invalid value"
							: error.getDefaultMessage();
					if (error instanceof FieldError fieldError) {
						return fieldError.getField() + ": " + message;
					}
					return message;
				})
				.toList();
		String message = errors.isEmpty()
				? "Request validation failed"
				: String.join("; ", errors);
		return createResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request) {
		String message = exception.getConstraintViolations().stream()
				.map(this::formatConstraintViolation)
				.collect(Collectors.joining("; "));
		if (message.isBlank()) {
			message = "Request validation failed";
		}
		return createResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthenticationException(
			AuthenticationException exception,
			HttpServletRequest request) {
		return createResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", request);
	}

	@ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(
			AccessDeniedException exception,
			HttpServletRequest request) {
		return createResponse(HttpStatus.FORBIDDEN, "Access denied", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(
			Exception exception,
			HttpServletRequest request) {
		return createResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred",
				request);
	}

	private String formatConstraintViolation(ConstraintViolation<?> violation) {
		return violation.getPropertyPath() + ": " + violation.getMessage();
	}

	private ResponseEntity<ErrorResponse> createResponse(
			HttpStatus status,
			String message,
			HttpServletRequest request) {
		ErrorResponse body = new ErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
