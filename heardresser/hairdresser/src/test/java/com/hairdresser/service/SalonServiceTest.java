package com.hairdresser.service;

import com.hairdresser.model.Salon;
import com.hairdresser.repository.SalonRepository;
import com.hairdresser.repository.WorkingHoursRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalonServiceTest {

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private WorkingHoursRepository workingHoursRepository;

    @InjectMocks
    private SalonService salonService;

    // ── findAll ─────────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsSalonsFromRepository() {
        Salon s1 = new Salon("Alpha", "addr1", 1.0, 2.0);
        Salon s2 = new Salon("Beta",  "addr2", 3.0, 4.0);
        when(salonRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Salon> result = salonService.findAll();

        assertThat(result).hasSize(2).extracting(Salon::getName).containsExactly("Alpha", "Beta");
    }

    // ── findById ────────────────────────────────────────────────────────────────

    @Test
    void findById_existingSalon_returnsSalon() {
        Salon salon = new Salon("Elegance", "Sofia", 42.0, 23.0);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        Salon result = salonService.findById(1L);

        assertThat(result.getName()).isEqualTo("Elegance");
    }

    @Test
    void findById_missingSalon_throwsNoSuchElementException() {
        when(salonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salonService.findById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── create ──────────────────────────────────────────────────────────────────

    @Test
    void create_persistsAndReturnsSalon() {
        Salon input = new Salon("New", "Street 1", 10.0, 20.0);
        when(salonRepository.save(any(Salon.class))).thenReturn(input);

        Salon result = salonService.create(input);

        assertThat(result.getName()).isEqualTo("New");
        verify(salonRepository).save(input);
    }
}
