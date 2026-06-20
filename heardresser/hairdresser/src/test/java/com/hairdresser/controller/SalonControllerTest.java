package com.hairdresser.controller;

import com.hairdresser.model.Salon;
import com.hairdresser.service.BookingService;
import com.hairdresser.service.SalonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalonControllerTest {

    @Mock
    private SalonService salonService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private SalonController salonController;

    // ── GET /api/salons ─────────────────────────────────────────────────────────

    @Test
    void getAll_returns200WithSalonList() {
        Salon s1 = new Salon("Alpha", "addr1", 1.0, 2.0);
        Salon s2 = new Salon("Beta",  "addr2", 3.0, 4.0);
        when(salonService.findAll()).thenReturn(List.of(s1, s2));

        List<Salon> result = salonController.getAll();

        assertThat(result).hasSize(2);
    }

    // ── GET /api/salons/{id} ─────────────────────────────────────────────────────

    @Test
    void getById_existingSalon_returns200() {
        Salon salon = new Salon("Elegance", "Sofia", 42.0, 23.0);
        when(salonService.findById(1L)).thenReturn(salon);

        ResponseEntity<Salon> response = salonController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Elegance");
    }

    @Test
    void getById_missingSalon_returns404() {
        when(salonService.findById(99L)).thenThrow(new NoSuchElementException("not found"));

        ResponseEntity<Salon> response = salonController.getById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── GET /api/salons/{id}/availability ────────────────────────────────────────

    @Test
    void getAvailability_returnsSlotsFor200() {
        LocalDate date = LocalDate.of(2026, 6, 22);
        when(bookingService.getAvailableSlots(1L, date)).thenReturn(List.of("09:00", "09:30"));

        ResponseEntity<List<String>> response = salonController.getAvailability(1L, date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("09:00", "09:30");
    }

    @Test
    void getAvailability_salonNotFound_returns404() {
        LocalDate date = LocalDate.of(2026, 6, 22);
        when(bookingService.getAvailableSlots(99L, date)).thenThrow(new NoSuchElementException());

        ResponseEntity<List<String>> response = salonController.getAvailability(99L, date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
