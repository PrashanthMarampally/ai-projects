-- ============================================================
-- Test seed data for local development
-- Vehicles and charging stations along the three UI presets:
--   Hyderabad → Bangalore  (NH-44 corridor)
--   Mumbai → Pune          (NH-48 corridor)
--   Delhi → Agra           (NH-19 corridor)
-- ============================================================

-- Vehicles
-- (the fake routing provider always reports 275 / 260 / 290 km,
--  so choose a vehicle with a range shorter than that to see
--  charging stops in action – e.g. Tata Nexon EV Max at 80 % SOC)

INSERT INTO vehicle
    (name, battery_capacity_kwh, usable_battery_capacity_kwh,
     range_km, consumption_kwh_per_100km, default_connector_type)
VALUES
    ('Tata Nexon EV Max',   40.5, 40.5, 437, 14.5, 'CCS2'),
    ('MG ZS EV',            50.3, 44.5, 461, 15.0, 'CCS2'),
    ('Hyundai Ioniq 5',     72.6, 68.0, 631, 15.5, 'CCS2'),
    ('BYD Atto 3',          60.5, 58.0, 521, 15.5, 'CCS2');


-- ============================================================
-- Hyderabad → Bangalore  (NH-44)
-- Fake route geometry:
--   Hyd (17.385, 78.487) → mid (15.178, 78.041) → Blr (12.972, 77.595)
-- Stations must be within 50 km of those two line segments.
-- ============================================================

INSERT INTO charging_station
    (name, latitude, longitude, location, city, state, country, operator, status)
VALUES
    ('Jadcherla EV Hub',
     16.763, 77.993,
     ST_SetSRID(ST_MakePoint(77.993, 16.763), 4326),
     'Jadcherla', 'Telangana', 'India', 'Tata Power EV', 'AVAILABLE'),

    ('Kurnool Fast Charge',
     15.838, 78.047,
     ST_SetSRID(ST_MakePoint(78.047, 15.838), 4326),
     'Kurnool', 'Andhra Pradesh', 'India', 'Tata Power EV', 'AVAILABLE'),

    ('Anantapur EVCS',
     14.682, 77.600,
     ST_SetSRID(ST_MakePoint(77.600, 14.682), 4326),
     'Anantapur', 'Andhra Pradesh', 'India', 'ChargeGrid', 'AVAILABLE'),

    ('Hindupur Charging Point',
     13.825, 77.491,
     ST_SetSRID(ST_MakePoint(77.491, 13.825), 4326),
     'Hindupur', 'Andhra Pradesh', 'India', 'ChargeGrid', 'AVAILABLE');

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2',  150.00, 2, 2 FROM charging_station WHERE name = 'Jadcherla EV Hub';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Jadcherla EV Hub';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2',  150.00, 2, 2 FROM charging_station WHERE name = 'Kurnool Fast Charge';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Kurnool Fast Charge';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2',  150.00, 2, 2 FROM charging_station WHERE name = 'Anantapur EVCS';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Anantapur EVCS';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2',  100.00, 2, 2 FROM charging_station WHERE name = 'Hindupur Charging Point';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Hindupur Charging Point';


-- ============================================================
-- Mumbai → Pune  (NH-48 / Expressway)
-- Fake route geometry:
--   Mumbai (19.076, 72.878) → mid (18.798, 73.367) → Pune (18.520, 73.857)
-- ============================================================

INSERT INTO charging_station
    (name, latitude, longitude, location, city, state, country, operator, status)
VALUES
    ('Khopoli Supercharger',
     18.779, 73.343,
     ST_SetSRID(ST_MakePoint(73.343, 18.779), 4326),
     'Khopoli', 'Maharashtra', 'India', 'Tesla / Tata Power', 'AVAILABLE'),

    ('Lonavala EVCS',
     18.753, 73.413,
     ST_SetSRID(ST_MakePoint(73.413, 18.753), 4326),
     'Lonavala', 'Maharashtra', 'India', 'ChargeGrid', 'AVAILABLE');

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2',  150.00, 4, 4 FROM charging_station WHERE name = 'Khopoli Supercharger';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Khopoli Supercharger';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2',  60.00, 2, 2 FROM charging_station WHERE name = 'Lonavala EVCS';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2', 22.00, 2, 2 FROM charging_station WHERE name = 'Lonavala EVCS';


-- ============================================================
-- Delhi → Agra  (NH-19 / Yamuna Expressway)
-- Fake route geometry:
--   Delhi (28.614, 77.209) → mid (27.895, 77.609) → Agra (27.177, 78.008)
-- ============================================================

INSERT INTO charging_station
    (name, latitude, longitude, location, city, state, country, operator, status)
VALUES
    ('Faridabad EV Hub',
     28.408, 77.313,
     ST_SetSRID(ST_MakePoint(77.313, 28.408), 4326),
     'Faridabad', 'Haryana', 'India', 'BPCL EV', 'AVAILABLE'),

    ('Mathura Fast Charge',
     27.492, 77.673,
     ST_SetSRID(ST_MakePoint(77.673, 27.492), 4326),
     'Mathura', 'Uttar Pradesh', 'India', 'BPCL EV', 'AVAILABLE');

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2',  150.00, 2, 2 FROM charging_station WHERE name = 'Faridabad EV Hub';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Faridabad EV Hub';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2',  150.00, 2, 2 FROM charging_station WHERE name = 'Mathura Fast Charge';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Mathura Fast Charge';
