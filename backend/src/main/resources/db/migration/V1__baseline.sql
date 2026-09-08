-- AgriPulse foundation baseline (pre-release, no production data yet).
-- Normalized schema for the modular monolith. PostGIS enabled for future geo queries.
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  phone VARCHAR(32),
  phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
  verification_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE farmer_profiles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
  full_name VARCHAR(255) NOT NULL,
  phone VARCHAR(32),
  region VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE farms (
  id BIGSERIAL PRIMARY KEY,
  farmer_profile_id BIGINT NOT NULL REFERENCES farmer_profiles(id),
  name VARCHAR(255) NOT NULL,
  region VARCHAR(255) NOT NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  area_acres DOUBLE PRECISION,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE buyer_profiles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
  org_name VARCHAR(255) NOT NULL,
  region VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE fpo_profiles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
  fpo_name VARCHAR(255) NOT NULL,
  region VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE crops (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(64) UNIQUE NOT NULL,
  unit VARCHAR(16) NOT NULL DEFAULT 'tonne',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE supply_reports (
  id BIGSERIAL PRIMARY KEY,
  farm_id BIGINT NOT NULL REFERENCES farms(id),
  crop_id BIGINT NOT NULL REFERENCES crops(id),
  reporter_id BIGINT NOT NULL REFERENCES users(id),
  quantity_min_tonnes DOUBLE PRECISION NOT NULL,
  quantity_max_tonnes DOUBLE PRECISION NOT NULL,
  harvest_start DATE NOT NULL,
  harvest_end DATE NOT NULL,
  quality VARCHAR(32),
  region VARCHAR(255) NOT NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE supply_evidence (
  id BIGSERIAL PRIMARY KEY,
  supply_report_id BIGINT NOT NULL REFERENCES supply_reports(id),
  evidence_type VARCHAR(64) NOT NULL,
  reference VARCHAR(1024),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE buyer_requirements (
  id BIGSERIAL PRIMARY KEY,
  buyer_profile_id BIGINT NOT NULL REFERENCES buyer_profiles(id),
  crop_id BIGINT NOT NULL REFERENCES crops(id),
  quantity_tonnes DOUBLE PRECISION NOT NULL,
  quality VARCHAR(32),
  required_date DATE NOT NULL,
  region VARCHAR(255) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
  data_source VARCHAR(16) NOT NULL DEFAULT 'REAL',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE demand_estimates (
  id BIGSERIAL PRIMARY KEY,
  crop_id BIGINT NOT NULL REFERENCES crops(id),
  region VARCHAR(255) NOT NULL,
  confirmed_demand_tonnes DOUBLE PRECISION NOT NULL,
  absorption_min_tonnes DOUBLE PRECISION NOT NULL,
  absorption_max_tonnes DOUBLE PRECISION NOT NULL,
  estimated_min_tonnes DOUBLE PRECISION NOT NULL,
  estimated_max_tonnes DOUBLE PRECISION NOT NULL,
  confidence DOUBLE PRECISION NOT NULL,
  data_source VARCHAR(16) NOT NULL DEFAULT 'ESTIMATED',
  computed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE markets (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  region VARCHAR(255) NOT NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  absorption_min_tonnes DOUBLE PRECISION NOT NULL DEFAULT 0,
  absorption_max_tonnes DOUBLE PRECISION NOT NULL DEFAULT 0,
  market_type VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE storage_facilities (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  region VARCHAR(255) NOT NULL,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  capacity_tonnes DOUBLE PRECISION NOT NULL,
  occupied_tonnes DOUBLE PRECISION NOT NULL DEFAULT 0,
  crop_compatibility VARCHAR(256),
  cost_per_day DOUBLE PRECISION,
  available_from DATE,
  available_to DATE,
  operator_id BIGINT REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE transport_resources (
  id BIGSERIAL PRIMARY KEY,
  owner_id BIGINT REFERENCES users(id),
  capacity_tonnes DOUBLE PRECISION NOT NULL,
  available_from DATE,
  available_to DATE,
  origin_region VARCHAR(255) NOT NULL,
  dest_region VARCHAR(255),
  status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
  cost_per_km DOUBLE PRECISION,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE verification_records (
  id BIGSERIAL PRIMARY KEY,
  subject_user_id BIGINT NOT NULL REFERENCES users(id),
  verifier_type VARCHAR(16) NOT NULL,
  method VARCHAR(64) NOT NULL,
  decision VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  notes VARCHAR(1024),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE trust_scores (
  id BIGSERIAL PRIMARY KEY,
  supply_report_id BIGINT UNIQUE NOT NULL REFERENCES supply_reports(id),
  score DOUBLE PRECISION NOT NULL,
  signals VARCHAR(2000),
  computed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  recipient_id BIGINT NOT NULL REFERENCES users(id),
  title VARCHAR(255) NOT NULL,
  message VARCHAR(2000) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
  id BIGSERIAL PRIMARY KEY,
  actor_id BIGINT,
  action VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64),
  entity_id VARCHAR(64),
  details VARCHAR(2000),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE scenarios (
  id BIGSERIAL PRIMARY KEY,
  owner_id BIGINT NOT NULL REFERENCES users(id),
  name VARCHAR(255) NOT NULL,
  params_json VARCHAR(4000) NOT NULL,
  result_json VARCHAR(4000),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- MVP seed: the single in-scope crop.
INSERT INTO crops (name, unit) VALUES ('Tomato', 'tonne');
