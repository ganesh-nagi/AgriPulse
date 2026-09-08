CREATE TABLE market_observations (
  id BIGSERIAL PRIMARY KEY,
  market_id BIGINT NOT NULL REFERENCES markets(id),
  crop_id BIGINT NOT NULL REFERENCES crops(id),
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  absorption_min_tonnes DOUBLE PRECISION NOT NULL,
  absorption_max_tonnes DOUBLE PRECISION NOT NULL,
  data_source VARCHAR(16) NOT NULL DEFAULT 'REAL',
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Illustrative MVP seed, honestly labelled SIMULATED. Real deployments
-- replace these rows with imported mandi arrival/trade records (REAL).
INSERT INTO markets (name, region, market_type) VALUES ('Nashik Reference Mandi', 'Nashik', 'REFERENCE');

INSERT INTO market_observations
  (market_id, crop_id, period_start, period_end,
   absorption_min_tonnes, absorption_max_tonnes, data_source)
SELECT m.id, c.id, DATE '2026-07-01', DATE '2026-07-31', 50, 70, 'SIMULATED'
FROM markets m, crops c
WHERE m.name = 'Nashik Reference Mandi' AND c.name = 'Tomato';

INSERT INTO market_observations
  (market_id, crop_id, period_start, period_end,
   absorption_min_tonnes, absorption_max_tonnes, data_source)
SELECT m.id, c.id, DATE '2026-08-01', DATE '2026-08-31', 55, 75, 'SIMULATED'
FROM markets m, crops c
WHERE m.name = 'Nashik Reference Mandi' AND c.name = 'Tomato';
