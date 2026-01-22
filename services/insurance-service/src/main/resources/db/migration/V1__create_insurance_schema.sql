-- MediTrack Insurance Service - Comprehensive Schema per Technical Specification
-- This migration creates the complete insurance and claims management data model

-- =====================================================
-- INSURANCE POLICIES TABLE (Aggregate Root)
-- =====================================================
CREATE TABLE insurance_policies (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    policy_number VARCHAR(50) NOT NULL UNIQUE,
    payer_id VARCHAR(50) NOT NULL,
    payer_name VARCHAR(255) NOT NULL,
    plan_name VARCHAR(255),
    plan_type VARCHAR(50),
    group_number VARCHAR(50),
    subscriber_id VARCHAR(50) NOT NULL,
    subscriber_name VARCHAR(255),
    subscriber_dob DATE,
    subscriber_ssn VARCHAR(255), -- Encrypted
    relationship VARCHAR(20) NOT NULL,
    effective_date DATE NOT NULL,
    termination_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    coverage_type VARCHAR(50),
    copay_amount DECIMAL(10,2),
    deductible_amount DECIMAL(10,2),
    deductible_met DECIMAL(10,2) DEFAULT 0,
    out_of_pocket_max DECIMAL(10,2),
    out_of_pocket_met DECIMAL(10,2) DEFAULT 0,
    rx_copay DECIMAL(10,2),
    rx_deductible DECIMAL(10,2),
    year_reset_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    CONSTRAINT chk_relationship CHECK (relationship IN ('SELF', 'SPOUSE', 'CHILD', 'PARENT', 'DOMESTIC_PARTNER', 'OTHER')),
    CONSTRAINT chk_coverage_type CHECK (coverage_type IN ('PRIMARY', 'SECONDARY', 'TERTIARY'))
);

-- =====================================================
-- PAYERS (Insurance Companies)
-- =====================================================
CREATE TABLE payers (
    payer_id VARCHAR(50) PRIMARY KEY,
    payer_name VARCHAR(255) NOT NULL,
    payer_type VARCHAR(50),
    address VARCHAR(500),
    phone VARCHAR(20),
    fax VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(255),
    claims_address VARCHAR(500),
    claims_phone VARCHAR(20),
    claims_fax VARCHAR(20),
    electronic_payer_id VARCHAR(50),
    requires_pre_auth BOOLEAN DEFAULT FALSE,
    turnaround_days INTEGER,
    accepts_electronic_claims BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_payer_type CHECK (payer_type IN ('COMMERCIAL', 'MEDICARE', 'MEDICAID', 'TRICARE', 'WORKERS_COMP', 'OTHER'))
);

-- =====================================================
-- PRE-AUTHORIZATIONS
-- =====================================================
CREATE TABLE pre_authorizations (
    preauth_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES insurance_policies(policy_id),
    patient_id UUID NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    service_code VARCHAR(50) NOT NULL,
    service_description TEXT,
    diagnosis_codes TEXT[] NOT NULL,
    requested_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    requested_by VARCHAR(100) NOT NULL,
    requesting_provider_id VARCHAR(50),
    requesting_provider_name VARCHAR(255),
    facility_id VARCHAR(50),
    facility_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) DEFAULT 'ROUTINE',
    decision_date TIMESTAMP WITH TIME ZONE,
    decision_by VARCHAR(100),
    approval_number VARCHAR(50),
    denial_reason TEXT,
    denial_code VARCHAR(20),
    valid_from DATE,
    valid_until DATE,
    units_requested INTEGER,
    units_approved INTEGER,
    estimated_cost DECIMAL(10,2),
    approved_amount DECIMAL(10,2),
    appeal_deadline DATE,
    follow_up_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_preauth_status CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'EXPIRED', 'CANCELLED', 'APPEALED')),
    CONSTRAINT chk_preauth_priority CHECK (priority IN ('STAT', 'URGENT', 'ROUTINE')),
    CONSTRAINT chk_service_type CHECK (service_type IN ('LAB', 'IMAGING', 'SURGERY', 'MEDICATION', 'DME', 'THERAPY', 'OTHER'))
);

-- =====================================================
-- ELIGIBILITY VERIFICATIONS
-- =====================================================
CREATE TABLE eligibility_verifications (
    verification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES insurance_policies(policy_id),
    patient_id UUID NOT NULL,
    verification_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    verified_by VARCHAR(100),
    service_type VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    is_eligible BOOLEAN,
    coverage_level VARCHAR(50),
    effective_date DATE,
    termination_date DATE,
    deductible_amount DECIMAL(10,2),
    deductible_met DECIMAL(10,2),
    out_of_pocket_max DECIMAL(10,2),
    out_of_pocket_met DECIMAL(10,2),
    copay_amount DECIMAL(10,2),
    coinsurance_percentage DECIMAL(5,2),
    coverage_percentage DECIMAL(5,2),
    additional_notes TEXT,
    payer_reference VARCHAR(100),
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_verif_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'TERMINATED', 'ERROR'))
);

-- =====================================================
-- INSURANCE CLAIMS
-- =====================================================
CREATE TABLE insurance_claims (
    claim_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES insurance_policies(policy_id),
    patient_id UUID NOT NULL,
    claim_number VARCHAR(50) UNIQUE,
    claim_type VARCHAR(20) NOT NULL,
    filing_code VARCHAR(20),
    service_date DATE NOT NULL,
    service_end_date DATE,
    provider_id VARCHAR(50) NOT NULL,
    provider_name VARCHAR(255) NOT NULL,
    provider_npi VARCHAR(20),
    rendering_provider_id VARCHAR(50),
    rendering_provider_name VARCHAR(255),
    facility_id VARCHAR(50),
    facility_name VARCHAR(255),
    place_of_service VARCHAR(10),
    diagnosis_codes TEXT[] NOT NULL,
    procedure_codes TEXT[] NOT NULL,
    billed_amount DECIMAL(10,2) NOT NULL,
    allowed_amount DECIMAL(10,2),
    paid_amount DECIMAL(10,2),
    adjustment_amount DECIMAL(10,2),
    deductible_amount DECIMAL(10,2),
    coinsurance_amount DECIMAL(10,2),
    copay_amount DECIMAL(10,2),
    patient_responsibility DECIMAL(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submission_method VARCHAR(20),
    submission_date TIMESTAMP WITH TIME ZONE,
    acknowledged_date TIMESTAMP WITH TIME ZONE,
    processed_date TIMESTAMP WITH TIME ZONE,
    payment_date TIMESTAMP WITH TIME ZONE,
    check_number VARCHAR(50),
    eob_date DATE,
    denial_reason TEXT,
    denial_code VARCHAR(20),
    appeal_deadline DATE,
    appealed_date DATE,
    corrected_claim_id UUID,
    original_claim_id UUID,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    CONSTRAINT chk_claim_status CHECK (status IN ('DRAFT', 'READY', 'SUBMITTED', 'ACKNOWLEDGED', 'PROCESSING', 'PAID', 'PARTIALLY_PAID', 'DENIED', 'APPEALED', 'CANCELLED')),
    CONSTRAINT chk_claim_type CHECK (claim_type IN ('PROFESSIONAL', 'INSTITUTIONAL', 'DENTAL', 'PHARMACY')),
    CONSTRAINT chk_submission_method CHECK (submission_method IN ('ELECTRONIC', 'PAPER', 'CLEARINGHOUSE', NULL))
);

-- =====================================================
-- CLAIM LINE ITEMS (Detailed charges)
-- =====================================================
CREATE TABLE claim_line_items (
    line_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES insurance_claims(claim_id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    service_date DATE NOT NULL,
    place_of_service VARCHAR(10),
    procedure_code VARCHAR(20) NOT NULL,
    procedure_description VARCHAR(255),
    modifier_1 VARCHAR(5),
    modifier_2 VARCHAR(5),
    modifier_3 VARCHAR(5),
    modifier_4 VARCHAR(5),
    diagnosis_pointer VARCHAR(10),
    units DECIMAL(10,2) NOT NULL,
    charge_amount DECIMAL(10,2) NOT NULL,
    allowed_amount DECIMAL(10,2),
    paid_amount DECIMAL(10,2),
    adjustment_amount DECIMAL(10,2),
    adjustment_reason_codes TEXT[],
    remark_codes TEXT[],
    revenue_code VARCHAR(10),
    ndc_code VARCHAR(20),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- CLAIM ADJUSTMENTS (Payment adjustments)
-- =====================================================
CREATE TABLE claim_adjustments (
    adjustment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES insurance_claims(claim_id) ON DELETE CASCADE,
    adjustment_type VARCHAR(50) NOT NULL,
    adjustment_code VARCHAR(20),
    adjustment_reason TEXT,
    adjustment_amount DECIMAL(10,2) NOT NULL,
    adjustment_date DATE NOT NULL,
    processed_by VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_adjustment_type CHECK (adjustment_type IN ('CONTRACTUAL', 'COINSURANCE', 'DEDUCTIBLE', 'COPAY', 'COB', 'DENIAL', 'WRITE_OFF', 'OTHER'))
);

-- =====================================================
-- CLAIM PAYMENTS
-- =====================================================
CREATE TABLE claim_payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES insurance_claims(claim_id),
    payment_date DATE NOT NULL,
    payment_method VARCHAR(20),
    check_number VARCHAR(50),
    check_date DATE,
    payment_amount DECIMAL(10,2) NOT NULL,
    eob_number VARCHAR(50),
    posted_date DATE,
    posted_by VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CHECK', 'EFT', 'CREDIT_CARD', 'CASH', 'OTHER'))
);

-- =====================================================
-- CLAIM STATUS HISTORY
-- =====================================================
CREATE TABLE claim_status_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES insurance_claims(claim_id) ON DELETE CASCADE,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    status_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    changed_by VARCHAR(100),
    change_reason TEXT,
    notes TEXT
);

-- =====================================================
-- COORDINATION OF BENEFITS (COB)
-- =====================================================
CREATE TABLE coordination_of_benefits (
    cob_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    primary_policy_id UUID REFERENCES insurance_policies(policy_id),
    secondary_policy_id UUID REFERENCES insurance_policies(policy_id),
    tertiary_policy_id UUID REFERENCES insurance_policies(policy_id),
    effective_date DATE NOT NULL,
    end_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Policy indexes
CREATE INDEX idx_policies_patient ON insurance_policies(patient_id);
CREATE INDEX idx_policies_policy_number ON insurance_policies(policy_number);
CREATE INDEX idx_policies_payer ON insurance_policies(payer_id);
CREATE INDEX idx_policies_subscriber ON insurance_policies(subscriber_id);
CREATE INDEX idx_policies_active ON insurance_policies(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_policies_effective ON insurance_policies(effective_date, termination_date);

-- Payer indexes
CREATE INDEX idx_payers_name ON payers(payer_name);
CREATE INDEX idx_payers_active ON payers(is_active) WHERE is_active = TRUE;

-- Pre-authorization indexes
CREATE INDEX idx_preauth_policy ON pre_authorizations(policy_id);
CREATE INDEX idx_preauth_patient ON pre_authorizations(patient_id);
CREATE INDEX idx_preauth_status ON pre_authorizations(status);
CREATE INDEX idx_preauth_date ON pre_authorizations(requested_date DESC);
CREATE INDEX idx_preauth_approval ON pre_authorizations(approval_number);

-- Eligibility indexes
CREATE INDEX idx_eligibility_policy ON eligibility_verifications(policy_id);
CREATE INDEX idx_eligibility_patient ON eligibility_verifications(patient_id);
CREATE INDEX idx_eligibility_date ON eligibility_verifications(verification_date DESC);

-- Claims indexes
CREATE INDEX idx_claims_policy ON insurance_claims(policy_id);
CREATE INDEX idx_claims_patient ON insurance_claims(patient_id);
CREATE INDEX idx_claims_number ON insurance_claims(claim_number);
CREATE INDEX idx_claims_status ON insurance_claims(status);
CREATE INDEX idx_claims_service_date ON insurance_claims(service_date DESC);
CREATE INDEX idx_claims_provider ON insurance_claims(provider_id);
CREATE INDEX idx_claims_submission ON insurance_claims(submission_date DESC);

-- Claim line items indexes
CREATE INDEX idx_line_items_claim ON claim_line_items(claim_id);
CREATE INDEX idx_line_items_procedure ON claim_line_items(procedure_code);

-- Claim adjustments indexes
CREATE INDEX idx_adjustments_claim ON claim_adjustments(claim_id);
CREATE INDEX idx_adjustments_type ON claim_adjustments(adjustment_type);

-- Claim payments indexes
CREATE INDEX idx_payments_claim ON claim_payments(claim_id);
CREATE INDEX idx_payments_date ON claim_payments(payment_date DESC);

-- Claim history indexes
CREATE INDEX idx_claim_history_claim ON claim_status_history(claim_id);
CREATE INDEX idx_claim_history_date ON claim_status_history(status_date DESC);

-- COB indexes
CREATE INDEX idx_cob_patient ON coordination_of_benefits(patient_id);
CREATE INDEX idx_cob_primary ON coordination_of_benefits(primary_policy_id);

-- =====================================================
-- FUNCTIONS AND TRIGGERS
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_policies_updated_at BEFORE UPDATE ON insurance_policies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payers_updated_at BEFORE UPDATE ON payers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_preauth_updated_at BEFORE UPDATE ON pre_authorizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_claims_updated_at BEFORE UPDATE ON insurance_claims
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cob_updated_at BEFORE UPDATE ON coordination_of_benefits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to track claim status changes
CREATE OR REPLACE FUNCTION track_claim_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        INSERT INTO claim_status_history (claim_id, old_status, new_status, changed_by, status_date)
        VALUES (NEW.claim_id, OLD.status, NEW.status, NEW.updated_by, NOW());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER track_claim_status AFTER UPDATE ON insurance_claims
    FOR EACH ROW EXECUTE FUNCTION track_claim_status_change();

-- Function to calculate patient responsibility
CREATE OR REPLACE FUNCTION calculate_patient_responsibility()
RETURNS TRIGGER AS $$
BEGIN
    NEW.patient_responsibility = COALESCE(NEW.deductible_amount, 0) +
                                 COALESCE(NEW.coinsurance_amount, 0) +
                                 COALESCE(NEW.copay_amount, 0);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER calc_patient_responsibility BEFORE INSERT OR UPDATE ON insurance_claims
    FOR EACH ROW EXECUTE FUNCTION calculate_patient_responsibility();

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================
COMMENT ON TABLE insurance_policies IS 'Patient insurance policies and coverage information';
COMMENT ON TABLE payers IS 'Insurance companies and payer information';
COMMENT ON TABLE pre_authorizations IS 'Pre-authorization requests and approvals';
COMMENT ON TABLE eligibility_verifications IS 'Real-time eligibility verification results';
COMMENT ON TABLE insurance_claims IS 'Insurance claims for services rendered';
COMMENT ON TABLE claim_line_items IS 'Individual line items and charges within claims';
COMMENT ON TABLE claim_adjustments IS 'Claim payment adjustments and write-offs';
COMMENT ON TABLE claim_payments IS 'Claim payment tracking';
COMMENT ON TABLE claim_status_history IS 'Audit trail of claim status changes';
COMMENT ON TABLE coordination_of_benefits IS 'Primary/secondary/tertiary insurance coordination';

-- =====================================================
-- SEED DATA (Development Only)
-- =====================================================

-- Insert sample payers
INSERT INTO payers (payer_id, payer_name, payer_type, electronic_payer_id, accepts_electronic_claims, turnaround_days)
VALUES
    ('PAYER-001', 'Blue Cross Blue Shield', 'COMMERCIAL', '00590', TRUE, 14),
    ('PAYER-002', 'Aetna', 'COMMERCIAL', '60054', TRUE, 14),
    ('PAYER-003', 'UnitedHealthcare', 'COMMERCIAL', '87726', TRUE, 14),
    ('PAYER-004', 'Medicare', 'MEDICARE', 'MEDICARE', TRUE, 21),
    ('PAYER-005', 'Medicaid', 'MEDICAID', 'MEDICAID', TRUE, 30),
    ('PAYER-006', 'Cigna', 'COMMERCIAL', '62308', TRUE, 14),
    ('PAYER-007', 'Humana', 'COMMERCIAL', '66614', TRUE, 14);

-- Insert sample policies for test patients
INSERT INTO insurance_policies (patient_id, policy_number, payer_id, payer_name, subscriber_id, relationship, effective_date, is_active, deductible_amount, out_of_pocket_max)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'POL-TEST-001', 'PAYER-001', 'Blue Cross Blue Shield', 'SUB-001', 'SELF', '2024-01-01', TRUE, 1500.00, 5000.00),
    ('b1ffcd99-9c0b-4ef8-bb6d-6bb9bd380a22', 'POL-TEST-002', 'PAYER-003', 'UnitedHealthcare', 'SUB-002', 'SELF', '2024-01-01', TRUE, 2000.00, 6000.00);

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Insurance Service schema created successfully';
END $$;
