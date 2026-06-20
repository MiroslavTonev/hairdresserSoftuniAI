package com.hairdresser.repository;

import com.hairdresser.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findBySalonIdAndBookingDate(Long salonId, LocalDate bookingDate);

    boolean existsBySalonIdAndBookingDateAndBookingTimeAndStatus(
            Long salonId, LocalDate bookingDate, LocalTime bookingTime, Booking.Status status);
}
