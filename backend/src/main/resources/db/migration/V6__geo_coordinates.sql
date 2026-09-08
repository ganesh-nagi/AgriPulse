ALTER TABLE processing_facilities ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE processing_facilities ADD COLUMN longitude DOUBLE PRECISION;

-- Reference node coordinates (Nashik cluster, Maharashtra). Zone-level
-- public infrastructure only; never farmer locations.
UPDATE markets SET latitude = 19.9975, longitude = 73.7898
WHERE name = 'Nashik Reference Mandi';

INSERT INTO storage_facilities
  (name, region, latitude, longitude, capacity_tonnes, occupied_tonnes,
   crop_compatibility, available_from, available_to)
VALUES
  ('Nashik Reference Cold Store', 'Nashik', 20.0059, 73.7910, 500, 120, 'Tomato',
   CURRENT_DATE - 30, CURRENT_DATE + 180);

INSERT INTO processing_facilities
  (name, region, crop_id, capacity_tonnes, occupied_tonnes,
   latitude, longitude, available_from, available_to)
SELECT 'Nashik Reference Grading Unit', 'Nashik', c.id, 200, 40,
   19.9900, 73.7800, CURRENT_DATE - 30, CURRENT_DATE + 180
FROM crops c WHERE c.name = 'Tomato';
