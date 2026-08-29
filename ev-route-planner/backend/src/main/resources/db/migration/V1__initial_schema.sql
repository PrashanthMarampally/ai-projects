CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE vehicle (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         battery_capacity_kwh NUMERIC(8,2),
                         usable_battery_capacity_kwh NUMERIC(8,2),
                         range_km NUMERIC(8,2) NOT NULL,
                         default_connector_type VARCHAR(50),
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE charging_station (
                                  id BIGSERIAL PRIMARY KEY,

                                  external_id VARCHAR(150),
                                  name VARCHAR(255) NOT NULL,

                                  latitude NUMERIC(10,7) NOT NULL,
                                  longitude NUMERIC(10,7) NOT NULL,

                                  location GEOMETRY(Point, 4326) NOT NULL,

                                  address VARCHAR(500),
                                  city VARCHAR(100),
                                  state VARCHAR(100),
                                  country VARCHAR(100),

                                  operator VARCHAR(255),

                                  status VARCHAR(50),

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_charging_station_location
    ON charging_station
    USING GIST (location);

CREATE TABLE charging_connector (
                                    id BIGSERIAL PRIMARY KEY,

                                    charging_station_id BIGINT NOT NULL,

                                    connector_type VARCHAR(50) NOT NULL,
                                    power_kw NUMERIC(8,2),

                                    quantity INTEGER NOT NULL DEFAULT 1,
                                    available_count INTEGER,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_connector_station
                                        FOREIGN KEY (charging_station_id)
                                            REFERENCES charging_station(id)
                                            ON DELETE CASCADE
);

CREATE INDEX idx_connector_station
    ON charging_connector(charging_station_id);

CREATE TABLE route_request (
                               id BIGSERIAL PRIMARY KEY,

                               source_latitude NUMERIC(10,7) NOT NULL,
                               source_longitude NUMERIC(10,7) NOT NULL,

                               destination_latitude NUMERIC(10,7) NOT NULL,
                               destination_longitude NUMERIC(10,7) NOT NULL,

                               vehicle_range_km NUMERIC(8,2) NOT NULL,
                               battery_percentage NUMERIC(5,2) NOT NULL,
                               reserve_percentage NUMERIC(5,2) NOT NULL,

                               requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE route_option (
                              id BIGSERIAL PRIMARY KEY,

                              route_request_id BIGINT NOT NULL,

                              provider VARCHAR(100),

                              distance_km NUMERIC(10,2),
                              duration_minutes INTEGER,

                              estimated_energy_kwh NUMERIC(10,2),

                              total_charging_time_minutes INTEGER,

                              score NUMERIC(8,2),

                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_route_option_request
                                  FOREIGN KEY (route_request_id)
                                      REFERENCES route_request(id)
                                      ON DELETE CASCADE
);

CREATE INDEX idx_route_option_request
    ON route_option(route_request_id);

CREATE TABLE charging_stop (
                               id BIGSERIAL PRIMARY KEY,

                               route_option_id BIGINT NOT NULL,
                               charging_station_id BIGINT NOT NULL,

                               sequence_number INTEGER NOT NULL,

                               arrival_soc NUMERIC(5,2),
                               departure_soc NUMERIC(5,2),

                               energy_added_kwh NUMERIC(10,2),

                               charging_time_minutes INTEGER,

                               CONSTRAINT fk_stop_route
                                   FOREIGN KEY (route_option_id)
                                       REFERENCES route_option(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_stop_station
                                   FOREIGN KEY (charging_station_id)
                                       REFERENCES charging_station(id)
);

CREATE INDEX idx_charging_stop_route
    ON charging_stop(route_option_id);

CREATE INDEX idx_charging_stop_station
    ON charging_stop(charging_station_id);

