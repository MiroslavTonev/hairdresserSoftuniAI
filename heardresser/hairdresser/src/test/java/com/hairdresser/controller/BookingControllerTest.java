package com.hairdresser.controller;

import com.hairdresser.model.Booking;
import com.hairdresser.model.Salon;
import com.hairdresser.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private BookingController.BookingRequest request(String date, String time) {
        return new BookingController.BookingRequest(1L, date, time, "Alice", "alice@mail.com");
    }

    // ── POST /api/bookings ───────────────────────────────────────────────────────

    @Test
    void createBooking_validRequest_returns201() {
        Salon salon = new Salon("S", "A", 0.0, 0.0);
        Booking booking = new Booking(salon,
                LocalDate.of(2026, 6, 22), LocalTime.of(9, 0), "Alice", "alice@mail.com");

        when(bookingService.createBooking(
                eq(1L),
                eq(LocalDate.of(2026, 6, 22)),
                eq(LocalTime.of(9, 0)),
                eq("Alice"),
                eq("alice@mail.com")))
                .thenReturn(booking);

        ResponseEntity<?> response = bookingController.createBooking(request("2026-06-22", "09:00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(Booking.class);
    }

    @Test
    void createBooking_slotAlreadyTaken_returns409() {
        when(bookingService.createBooking(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("already booked"));

        ResponseEntity<?> response = bookingController.createBooking(request("2026-06-22", "09:00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createBooking_salonNotFound_returns404() {
        when(bookingService.createBooking(any(), any(), any(), any(), any()))
                .thenThrow(new NoSuchElementException("salon not found"));

        ResponseEntity<?> response = bookingController.createBooking(request("2026-06-22", "09:00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
