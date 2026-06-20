package com.hairdresser.service;

import com.hairdresser.model.Booking;
import com.hairdresser.model.Salon;
import com.hairdresser.model.WorkingHours;
import com.hairdresser.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final int SLOT_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final SalonService salonService;

    public BookingService(BookingRepository bookingRepository, SalonService salonService) {
        this.bookingRepository = bookingRepository;
        this.salonService = salonService;
    }

    /**
     * Returns list of available time strings (HH:mm) for the given salon and date.
     */
    public List<String> getAvailableSlots(Long salonId, LocalDate date) {
        Salon salon = salonService.findById(salonId);

        String dayName = date.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                .toUpperCase(); // e.g. "MON"

        WorkingHours hours = salon.getWorkingHours().stream()
                .filter(wh -> wh.getDayOfWeek().equalsIgnoreCase(dayName))
                .findFirst()
                .orElse(null);

        if (hours == null) {
            return List.of(); // closed that day
        }

        // Booked times for this date
        Set<LocalTime> bookedTimes = bookingRepository
                .findBySalonIdAndBookingDate(salonId, date)
                .stream()
                .filter(b -> b.getStatus() == Booking.Status.ACTIVE)
                .map(Booking::getBookingTime)
                .collect(Collectors.toSet());

        // Generate all slots
        List<String> available = new ArrayList<>();
        LocalTime cursor = hours.getOpenTime();
        while (cursor.plusMinutes(SLOT_MINUTES).compareTo(hours.getCloseTime()) <= 0) {
            if (!bookedTimes.contains(cursor)) {
                available.add(cursor.toString());
            }
            cursor = cursor.plusMinutes(SLOT_MINUTES);
        }
        return available;
    }

    /**
     * Creates a booking after verifying the slot is still free.
     * Throws IllegalStateException if the slot is already taken.
     */
    public Booking createBooking(Long salonId, LocalDate date, LocalTime time,
                                 String clientName, String clientContact) {
        boolean taken = bookingRepository.existsBySalonIdAndBookingDateAndBookingTimeAndStatus(
                salonId, date, time, Booking.Status.ACTIVE);
        if (taken) {
            throw new IllegalStateException("Slot already booked: " + date + " " + time);
        }
        Salon salon = salonService.findById(salonId);
        Booking booking = new Booking(salon, date, time, clientName, clientContact);
        return bookingRepository.save(booking);
    }

    /**
     * Returns all bookings for a salon on a given date (owner view).
     */
    public List<Booking> getBookingsForDate(Long salonId, LocalDate date) {
        return bookingRepository.findBySalonIdAndBookingDate(salonId, date);
    }
}
