package com.hairdresser.controller;

import com.hairdresser.model.Salon;
import com.hairdresser.model.WorkingHours;
import com.hairdresser.service.BookingService;
import com.hairdresser.service.SalonService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/salons")
public class SalonController {

    private final SalonService salonService;
    private final BookingService bookingService;

    public SalonController(SalonService salonService, BookingService bookingService) {
        this.salonService = salonService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<Salon> getAll() {
        return salonService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Salon> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(salonService.findById(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Salon> create(@RequestBody Salon salon) {
        return ResponseEntity.status(201).body(salonService.create(salon));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Salon> update(@PathVariable Long id, @RequestBody Salon salon) {
        try {
            return ResponseEntity.ok(salonService.update(id, salon));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<String>> getAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ResponseEntity.ok(bookingService.getAvailableSlots(id, date));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<?> getBookings(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ResponseEntity.ok(bookingService.getBookingsForDate(id, date));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
