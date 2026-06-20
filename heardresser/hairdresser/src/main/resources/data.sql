-- Seed data: one demo salon in Sofia, Bulgaria
INSERT INTO salons (name, address, lat, lng)
    SELECT 'Salon Elegance', 'bul. Vitosha 42, Sofia', 42.6977, 23.3219
    WHERE NOT EXISTS (SELECT 1 FROM salons WHERE name = 'Salon Elegance');

-- Working hours Mon–Fri 09:00–18:00
INSERT INTO working_hours (salon_id, day_of_week, open_time, close_time)
    SELECT 1, 'MON', '09:00:00', '18:00:00'
    WHERE NOT EXISTS (SELECT 1 FROM working_hours WHERE salon_id = 1 AND day_of_week = 'MON');

INSERT INTO working_hours (salon_id, day_of_week, open_time, close_time)
    SELECT 1, 'TUE', '09:00:00', '18:00:00'
    WHERE NOT EXISTS (SELECT 1 FROM working_hours WHERE salon_id = 1 AND day_of_week = 'TUE');

INSERT INTO working_hours (salon_id, day_of_week, open_time, close_time)
    SELECT 1, 'WED', '09:00:00', '18:00:00'
    WHERE NOT EXISTS (SELECT 1 FROM working_hours WHERE salon_id = 1 AND day_of_week = 'WED');

INSERT INTO working_hours (salon_id, day_of_week, open_time, close_time)
    SELECT 1, 'THU', '09:00:00', '18:00:00'
    WHERE NOT EXISTS (SELECT 1 FROM working_hours WHERE salon_id = 1 AND day_of_week = 'THU');

INSERT INTO working_hours (salon_id, day_of_week, open_time, close_time)
    SELECT 1, 'FRI', '09:00:00', '18:00:00'
    WHERE NOT EXISTS (SELECT 1 FROM working_hours WHERE salon_id = 1 AND day_of_week = 'FRI');

INSERT INTO working_hours (salon_id, day_of_week, open_time, close_time)
    SELECT 1, 'SAT', '10:00:00', '15:00:00'
    WHERE NOT EXISTS (SELECT 1 FROM working_hours WHERE salon_id = 1 AND day_of_week = 'SAT');
