-- Adds an optional reference to the originating record in another service
-- (e.g. the prescriptionId from prescription-service) so that event consumption
-- can be made idempotent (skip if an order for prescriptionId+testCode exists).

ALTER TABLE lab_orders ADD COLUMN external_reference VARCHAR(64);

CREATE INDEX idx_lab_orders_external_ref ON lab_orders(external_reference);

COMMENT ON COLUMN lab_orders.external_reference IS 'Originating record id in another service (e.g. source prescriptionId)';
