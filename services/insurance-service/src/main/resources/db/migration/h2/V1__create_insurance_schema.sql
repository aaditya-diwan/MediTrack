-- Insurance Service schema - H2-compatible version for local development
-- PostgreSQL-specific syntax (arrays, triggers, PL/pgSQL) is omitted here.
-- The full schema with triggers/functions lives in db/migration/V1__create_insurance_schema.sql
-- and is applied when running under the docker or prod profile.

CREATE TABLE IF NOT EXISTS insurance_policies (
    policy_id         UUID         NOT NULL DEFAULT RANDOM_UUID() PRIMARY KEY,
    patient_id        UUID         NOT NULL,
    policy_number     VARCHAR(50)  NOT NULL UNIQUE,
    payer_id          VARCHAR(50)  NOT NULL,
    payer_name        VARCHAR(255) NOT NULL,
    plan_name         VARCHAR(255),
    group_number      VARCHAR(50),
    subscriber_id     VARCHAR(50)  NOT NULL,
    subscriber_name   VARCHAR(255),
    relationship      VARCHAR(20)  NOT NULL,
    effective_date    DATE         NOT NULL,
    termination_date  DATE,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    copay_amount      DECIMAL(10,2),
    deductible_amount DECIMAL(10,2),
    deductible_met    DECIMAL(10,2) NOT NULL DEFAULT 0,
    out_of_pocket_max DECIMAL(10,2),
    out_of_pocket_met DECIMAL(10,2) NOT NULL DEFAULT 0,
    notes             TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version           INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_policies_patient     ON insurance_policies(patient_id);
CREATE INDEX IF NOT EXISTS idx_policies_payer       ON insurance_policies(payer_id);
CREATE INDEX IF NOT EXISTS idx_policies_active      ON insurance_policies(is_active);
CREATE INDEX IF NOT EXISTS idx_policies_effective   ON insurance_policies(effective_date);
