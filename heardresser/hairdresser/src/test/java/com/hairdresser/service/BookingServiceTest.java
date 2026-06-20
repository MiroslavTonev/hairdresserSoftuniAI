package com.hairdresser.service;

import com.hairdresser.model.Booking;
import com.hairdresser.model.Salon;
import com.hairdresser.model.WorkingHours;
import com.hairdresser.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SalonService salonService;

    @InjectMocks
    private BookingService bookingService;

    // ── helpers ────────────────────────────────────────────────────────────────

    private Salon salonWithHours(String day, LocalTime open, LocalTime close) {
        Salon salon = new Salon("Test Salon", "addr", 0.0, 0.0);
        WorkingHours wh = new WorkingHours(salon, day, open, close);
        salon.getWorkingHours().add(wh);
        return salon;
    }

    // Monday 2026-06-22 → day name "MON"
    private static final LocalDate MONDAY = LocalDate.of(2026, 6, 22);

    // ── getAvailableSlots ───────────────────────────────────────────────────────

    @Test
    void getAvailableSlots_noExistingBookings_returnsAllSlots() {
        Salon salon = salonWithHours("MON", LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(salonService.findById(1L)).thenReturn(salon);
        when(bookingRepository.findBySalonIdAndBookingDate(1L, MONDAY)).thenReturn(List.of());

        List<String> slots = bookingService.getAvailableSlots(1L, MONDAY);

        assertThat(slots).containsExactly("09:00", "09:30");
    }

    @Test
    void getAvailableSlots_slotAlreadyBooked_omitsBookedSlot() {
        Salon salon = salonWithHours("MON", LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(salonService.findById(1L)).thenReturn(salon);

        Booking existing = new Booking(salon, MONDAY, LocalTime.of(9, 0), "Alice", "123");
        when(bookingRepository.findBySalonIdAndBookingDate(1L, MONDAY)).thenReturn(List.of(existing));

        List<String> slots = bookingService.getAvailableSlots(1L, MONDAY);

        assertThat(slots).containsExactly("09:30");
        assertThat(slots).doesNotContain("09:00");
    }

    @Test
    void getAvailableSlots_closedDay_returnsEmptyList() {
        // Salon only has TUE hours; querying on Monday (no entry)
        Salon salon = salonWithHours("TUE", LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(salonService.findById(1L)).thenReturn(salon);

        List<String> slots = bookingService.getAvailableSlots(1L, MONDAY);

        assertThat(slots).isEmpty();
    }

    // ── createBooking ───────────────────────────────────────────────────────────

    @Test
    void createBooking_slotFree_savesAndReturnsBooking() {
        Salon salon = salonWithHours("MON", LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(salonService.findById(1L)).thenReturn(salon);
        when(bookingRepository.existsBySalonIdAndBookingDateAndBookingTimeAndStatus(
                1L, MONDAY, LocalTime.of(9, 0), Booking.Status.ACTIVE)).thenReturn(false);
        Booking saved = new Booking(salon, MONDAY, LocalTime.of(9, 0), "Bob", "bob@mail.com");
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        Booking result = bookingService.createBooking(1L, MONDAY, LocalTime.of(9, 0), "Bob", "bob@mail.com");

        assertThat(result.getClientName()).isEqualTo("Bob");
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_slotAlreadyTaken_throwsIllegalStateException() {
        when(bookingRepository.existsBySalonIdAndBookingDateAndBookingTimeAndStatus(
                1L, MONDAY, LocalTime.of(9, 0), Booking.Status.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() ->
                bookingService.createBooking(1L, MONDAY, LocalTime.of(9, 0), "Eve", "eve@mail.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already booked");

        verify(bookingRepository, never()).save(any());
    }
}
