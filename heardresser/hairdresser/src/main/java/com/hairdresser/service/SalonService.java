package com.hairdresser.service;

import com.hairdresser.model.Salon;
import com.hairdresser.model.WorkingHours;
import com.hairdresser.repository.SalonRepository;
import com.hairdresser.repository.WorkingHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SalonService {

    private final SalonRepository salonRepository;
    private final WorkingHoursRepository workingHoursRepository;

    public SalonService(SalonRepository salonRepository, WorkingHoursRepository workingHoursRepository) {
        this.salonRepository = salonRepository;
        this.workingHoursRepository = workingHoursRepository;
    }

    public List<Salon> findAll() {
        return salonRepository.findAll();
    }

    public Salon findById(Long id) {
        return salonRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Salon not found: " + id));
    }

    @Transactional
    public Salon create(Salon salon) {
        if (salon.getWorkingHours() != null) {
            salon.getWorkingHours().forEach(wh -> wh.setSalon(salon));
        }
        return salonRepository.save(salon);
    }

    @Transactional
    public Salon update(Long id, Salon incoming) {
        Salon existing = findById(id);
        existing.setName(incoming.getName());
        existing.setAddress(incoming.getAddress());
        existing.setLat(incoming.getLat());
        existing.setLng(incoming.getLng());

        // Replace working hours
        workingHoursRepository.deleteBySalonId(id);
        workingHoursRepository.flush();

        if (incoming.getWorkingHours() != null) {
            List<WorkingHours> updated = incoming.getWorkingHours().stream()
                    .peek(wh -> wh.setSalon(existing))
                    .toList();
            existing.getWorkingHours().clear();
            existing.getWorkingHours().addAll(updated);
        }

        return salonRepository.save(existing);
    }
}
