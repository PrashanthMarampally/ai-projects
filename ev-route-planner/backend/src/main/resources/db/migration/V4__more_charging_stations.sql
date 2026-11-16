-- ============================================================
-- V4: More charging stations
--
-- Gap-fillers on existing corridors + 2 new corridors:
--   Delhi → Jaipur  (NH-48, ~270 km)
--   Bangalore → Chennai  (NH-48 / NH-44, ~350 km)
--
-- All coordinates verified to be within the 50 km station-
-- corridor radius of the fake route LineStrings used in local
-- development.  They also lie on or near the real highway so
-- they will be found once Google routing is wired in.
-- ============================================================


-- ============================================================
-- Hyderabad → Bangalore (NH-44) gap fillers
-- Existing: Jadcherla, Kurnool, Anantapur, Hindupur
-- Adding:   Nandyal (between Kurnool and Anantapur),
--           Dharmavaram (between Anantapur and Hindupur),
--           Tumkur (between Hindupur and Bangalore)
-- ============================================================

INSERT INTO charging_station
    (name, latitude, longitude, location, city, state, country, operator, status)
VALUES
    ('Nandyal EV Hub',
     15.478, 78.484,
     ST_SetSRID(ST_MakePoint(78.484, 15.478), 4326),
     'Nandyal', 'Andhra Pradesh', 'India', 'Tata Power EV', 'AVAILABLE'),

    ('Dharmavaram Fast Charge',
     14.414, 77.731,
     ST_SetSRID(ST_MakePoint(77.731, 14.414), 4326),
     'Dharmavaram', 'Andhra Pradesh', 'India', 'ChargeGrid', 'AVAILABLE'),

    ('Tumkur Charging Hub',
     13.342, 77.491,
     ST_SetSRID(ST_MakePoint(77.491, 13.342), 4326),
     'Tumkur', 'Karnataka', 'India', 'ChargeGrid', 'AVAILABLE');

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 150.00, 2, 2 FROM charging_station WHERE name = 'Nandyal EV Hub';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Nandyal EV Hub';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 100.00, 2, 2 FROM charging_station WHERE name = 'Dharmavaram Fast Charge';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Dharmavaram Fast Charge';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 150.00, 2, 2 FROM charging_station WHERE name = 'Tumkur Charging Hub';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Tumkur Charging Hub';


-- ============================================================
-- Mumbai → Pune (NH-48) gap filler
-- Adding: Talegaon (between Lonavala and Pune)
-- ============================================================

INSERT INTO charging_station
    (name, latitude, longitude, location, city, state, country, operator, status)
VALUES
    ('Talegaon Fast Charge',
     18.737, 73.653,
     ST_SetSRID(ST_MakePoint(73.653, 18.737), 4326),
     'Talegaon', 'Maharashtra', 'India', 'Tata Power EV', 'AVAILABLE');

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 150.00, 4, 4 FROM charging_station WHERE name = 'Talegaon Fast Charge';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Talegaon Fast Charge';


-- ============================================================
-- Delhi → Agra (NH-19) gap fillers
-- Adding: Palwal (between Faridabad and Mathura),
--         Vrindavan (between Mathura and Agra)
-- ============================================================

INSERT INTO charging_station
    (name, latitude, longitude, location, city, state, country, operator, status)
VALUES
    ('Palwal EV Station',
     28.144, 77.330,
     ST_SetSRID(ST_MakePoint(77.330, 28.144), 4326),
     'Palwal', 'Haryana', 'India', 'BPCL EV', 'AVAILABLE'),

    ('Vrindavan Charge Point',
     27.577, 77.701,
     ST_SetSRID(ST_MakePoint(77.701, 27.577), 4326),
     'Vrindavan', 'Uttar Pradesh', 'India', 'BPCL EV', 'AVAILABLE');

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 150.00, 2, 2 FROM charging_station WHERE name = 'Palwal EV Station';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Palwal EV Station';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 100.00, 2, 2 FROM charging_station WHERE name = 'Vrindavan Charge Point';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Vrindavan Charge Point';


-- ============================================================
-- NEW: Delhi → Jaipur (NH-48, ~270 km)
-- Fake route geometry:
--   Delhi (28.614, 77.209) → mid (27.763, 76.498) → Jaipur (26.912, 75.787)
-- ============================================================

INSERT INTO charging_station
    (name, latitude, longitude, location, city, state, country, operator, status)
VALUES
    ('Rewari Fast Charge',
     28.195, 76.617,
     ST_SetSRID(ST_MakePoint(76.617, 28.195), 4326),
     'Rewari', 'Haryana', 'India', 'ChargeGrid', 'AVAILABLE'),

    ('Alwar Charging Hub',
     27.564, 76.598,
     ST_SetSRID(ST_MakePoint(76.598, 27.564), 4326),
     'Alwar', 'Rajasthan', 'India', 'Tata Power EV', 'AVAILABLE');

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 150.00, 2, 2 FROM charging_station WHERE name = 'Rewari Fast Charge';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Rewari Fast Charge';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 100.00, 2, 2 FROM charging_station WHERE name = 'Alwar Charging Hub';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Alwar Charging Hub';


-- ============================================================
-- NEW: Bangalore → Chennai (NH-48 / NH-44, ~350 km)
-- Fake route geometry:
--   Bangalore (12.972, 77.595) → mid (13.027, 78.933) → Chennai (13.082, 80.270)
-- ============================================================

INSERT INTO charging_station
    (name, latitude, longitude, location, city, state, country, operator, status)
VALUES
    ('Ambur EV Station',
     12.793, 78.717,
     ST_SetSRID(ST_MakePoint(78.717, 12.793), 4326),
     'Ambur', 'Tamil Nadu', 'India', 'ChargeGrid', 'AVAILABLE'),

    ('Vellore Fast Charge',
     12.916, 79.132,
     ST_SetSRID(ST_MakePoint(79.132, 12.916), 4326),
     'Vellore', 'Tamil Nadu', 'India', 'Tata Power EV', 'AVAILABLE'),

    ('Ranipet Charge Hub',
     12.922, 79.333,
     ST_SetSRID(ST_MakePoint(79.333, 12.922), 4326),
     'Ranipet', 'Tamil Nadu', 'India', 'Tata Power EV', 'AVAILABLE');

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 100.00, 2, 2 FROM charging_station WHERE name = 'Ambur EV Station';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Ambur EV Station';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 150.00, 2, 2 FROM charging_station WHERE name = 'Vellore Fast Charge';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Vellore Fast Charge';

INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'CCS2', 150.00, 2, 2 FROM charging_station WHERE name = 'Ranipet Charge Hub';
INSERT INTO charging_connector (charging_station_id, connector_type, power_kw, quantity, available_count)
SELECT id, 'Type2',  22.00, 2, 2 FROM charging_station WHERE name = 'Ranipet Charge Hub';
