CREATE TABLE processing_facilities (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  region VARCHAR(255) NOT NULL,
  crop_id BIGINT NOT NULL REFERENCES crops(id),
  capacity_tonnes DOUBLE PRECISION NOT NULL,
  occupied_tonnes DOUBLE PRECISION NOT NULL DEFAULT 0,
  cost_per_day DOUBLE PRECISION,
  available_from DATE,
  available_to DATE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE transport_resources ADD COLUMN travel_estimate_days DOUBLE PRECISION;
