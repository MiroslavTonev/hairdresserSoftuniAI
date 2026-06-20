package com.hairdresser.controller;

import com.hairdresser.model.Booking;
import com.hairdresser.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/bookings
     * Body: { salonId, date, time, clientName, clientContact }
     */
    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        try {
            LocalDate date = LocalDate.parse(request.date());
            LocalTime time = LocalTime.parse(request.time());
            Booking booking = bookingService.createBooking(
                    request.salonId(), date, time, request.clientName(), request.clientContact());
            return ResponseEntity.status(201).body(booking);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    public record BookingRequest(Long salonId, String date, String time,
                                  String clientName, String clientContact) {}
}
