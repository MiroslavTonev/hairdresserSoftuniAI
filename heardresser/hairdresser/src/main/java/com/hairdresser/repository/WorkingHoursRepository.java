package com.hairdresser.repository;

import com.hairdresser.model.WorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, Long> {

    List<WorkingHours> findBySalonId(Long salonId);

    void deleteBySalonId(Long salonId);
}
