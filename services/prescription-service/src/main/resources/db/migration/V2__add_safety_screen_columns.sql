-- AI drug-safety screen outcome captured when a prescription is issued.
ALTER TABLE prescriptions ADD COLUMN safety_check_performed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE prescriptions ADD COLUMN safety_severity VARCHAR(30);
ALTER TABLE prescriptions ADD COLUMN safety_summary TEXT;
ALTER TABLE prescriptions ADD COLUMN safety_overridden BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE prescriptions ADD COLUMN safety_override_reason TEXT;
