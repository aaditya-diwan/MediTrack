-- MediTrack Patient Service - Comprehensive Schema per Technical Specification
-- This migration enhances the patient schema to match production requirements

-- Drop existing simplified tables
DROP TABLE IF EXISTS medical_records CASCADE;
DROP TABLE IF EXISTS patients CASCADE;

-- =====================================================
-- PATIENTS TABLE (Aggregate Root)
-- =====================================================
CREATE TABLE patients (
    patient_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    mrn VARCHAR(20) UNIQUE NOT NULL,
    ssn VARCHAR(255), -- Encrypted in application layer
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN'))
);

-- =====================================================
-- PATIENT ADDRESSES (Value Object)
-- =====================================================
CREATE TABLE patient_addresses (
    address_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    address_type VARCHAR(20) NOT NULL,
    street_line1 VARCHAR(255) NOT NULL,
    street_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    country VARCHAR(50) DEFAULT 'USA',
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_address_type CHECK (address_type IN ('HOME', 'WORK', 'BILLING', 'MAILING'))
);

-- =====================================================
-- PATIENT INSURANCE
-- =====================================================
CREATE TABLE patient_insurance (
    insurance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    policy_number VARCHAR(50) NOT NULL,
    payer_id VARCHAR(50) NOT NULL,
    payer_name VARCHAR(255) NOT NULL,
    group_number VARCHAR(50),
    policy_holder_name VARCHAR(255),
    relationship_to_insured VARCHAR(20),
    effective_date DATE NOT NULL,
    termination_date DATE,
    is_primary BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    copay_amount DECIMAL(10,2),
    deductible_amount DECIMAL(10,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_relationship CHECK (relationship_to_insured IN ('SELF', 'SPOUSE', 'CHILD', 'PARENT', 'OTHER'))
);

-- =====================================================
-- MEDICAL RECORDS
-- =====================================================
CREATE TABLE medical_records (
    record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    record_type VARCHAR(50) NOT NULL,
    record_date TIMESTAMP WITH TIME ZONE NOT NULL,
    provider_id VARCHAR(50) NOT NULL,
    provider_name VARCHAR(255),
    facility_id VARCHAR(50),
    facility_name VARCHAR(255),
    chief_complaint TEXT,
    diagnosis_codes TEXT[],
    procedure_codes TEXT[],
    notes TEXT,
    attachments JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100),
    CONSTRAINT chk_record_type CHECK (record_type IN ('VISIT', 'LAB', 'IMAGING', 'PROCEDURE', 'MEDICATION', 'IMMUNIZATION', 'ALLERGY', 'DISCHARGE'))
);

-- =====================================================
-- PATIENT TIMELINE (Care Coordination)
-- =====================================================
CREATE TABLE patient_timeline (
    timeline_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_date TIMESTAMP WITH TIME ZONE NOT NULL,
    event_title VARCHAR(255) NOT NULL,
    event_description TEXT,
    source_system VARCHAR(50),
    source_id VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- LAB ORDERS (from Patient Service)
-- =====================================================
CREATE TABLE lab_orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    ordering_provider_id VARCHAR(50) NOT NULL,
    ordering_provider_name VARCHAR(255),
    order_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',
    test_codes TEXT[] NOT NULL,
    diagnosis_codes TEXT[],
    clinical_notes TEXT,
    facility_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_lab_order_status CHECK (status IN ('PENDING', 'SENT', 'ACKNOWLEDGED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_lab_priority CHECK (priority IN ('STAT', 'URGENT', 'ROUTINE'))
);

-- =====================================================
-- PATIENT ALLERGIES
-- =====================================================
CREATE TABLE patient_allergies (
    allergy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    allergen VARCHAR(255) NOT NULL,
    allergen_type VARCHAR(50) NOT NULL,
    reaction VARCHAR(255),
    severity VARCHAR(20),
    onset_date DATE,
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_allergen_type CHECK (allergen_type IN ('MEDICATION', 'FOOD', 'ENVIRONMENTAL', 'OTHER')),
    CONSTRAINT chk_severity CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE', 'LIFE_THREATENING'))
);

-- =====================================================
-- PATIENT MEDICATIONS
-- =====================================================
CREATE TABLE patient_medications (
    medication_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    medication_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    route VARCHAR(50),
    prescribing_provider_id VARCHAR(50),
    prescribing_provider_name VARCHAR(255),
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- PATIENT EMERGENCY CONTACTS
-- =====================================================
CREATE TABLE patient_emergency_contacts (
    contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    contact_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    phone_primary VARCHAR(20) NOT NULL,
    phone_secondary VARCHAR(20),
    email VARCHAR(255),
    address VARCHAR(500),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Patient indexes
CREATE INDEX idx_patients_mrn ON patients(mrn);
CREATE INDEX idx_patients_ssn ON patients(ssn) WHERE ssn IS NOT NULL;
CREATE INDEX idx_patients_tenant ON patients(tenant_id);
CREATE INDEX idx_patients_name ON patients(last_name, first_name);
CREATE INDEX idx_patients_dob ON patients(date_of_birth);
CREATE INDEX idx_patients_deleted ON patients(is_deleted) WHERE is_deleted = FALSE;

-- Address indexes
CREATE INDEX idx_addresses_patient ON patient_addresses(patient_id);
CREATE INDEX idx_addresses_primary ON patient_addresses(patient_id, is_primary) WHERE is_primary = TRUE;

-- Insurance indexes
CREATE INDEX idx_insurance_patient ON patient_insurance(patient_id);
CREATE INDEX idx_insurance_policy ON patient_insurance(policy_number);
CREATE INDEX idx_insurance_active ON patient_insurance(patient_id, is_active) WHERE is_active = TRUE;

-- Medical records indexes
CREATE INDEX idx_medical_records_patient ON medical_records(patient_id);
CREATE INDEX idx_medical_records_date ON medical_records(record_date DESC);
CREATE INDEX idx_medical_records_type ON medical_records(record_type);
CREATE INDEX idx_medical_records_provider ON medical_records(provider_id);

-- Timeline indexes
CREATE INDEX idx_timeline_patient ON patient_timeline(patient_id);
CREATE INDEX idx_timeline_date ON patient_timeline(event_date DESC);
CREATE INDEX idx_timeline_type ON patient_timeline(event_type);

-- Lab order indexes
CREATE INDEX idx_lab_orders_patient ON lab_orders(patient_id);
CREATE INDEX idx_lab_orders_status ON lab_orders(status);
CREATE INDEX idx_lab_orders_date ON lab_orders(order_date DESC);

-- Allergy indexes
CREATE INDEX idx_allergies_patient ON patient_allergies(patient_id);
CREATE INDEX idx_allergies_active ON patient_allergies(patient_id, is_active) WHERE is_active = TRUE;

-- Medication indexes
CREATE INDEX idx_medications_patient ON patient_medications(patient_id);
CREATE INDEX idx_medications_active ON patient_medications(patient_id, is_active) WHERE is_active = TRUE;

-- Emergency contact indexes
CREATE INDEX idx_emergency_contacts_patient ON patient_emergency_contacts(patient_id);

-- =====================================================
-- ROW LEVEL SECURITY (Multi-tenancy)
-- =====================================================
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see patients in their tenant
CREATE POLICY tenant_isolation ON patients
    FOR ALL
    TO PUBLIC
    USING (tenant_id = current_setting('app.current_tenant', TRUE));

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
CREATE TRIGGER update_patients_updated_at BEFORE UPDATE ON patients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_patient_addresses_updated_at BEFORE UPDATE ON patient_addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_patient_insurance_updated_at BEFORE UPDATE ON patient_insurance
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_lab_orders_updated_at BEFORE UPDATE ON lab_orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_patient_allergies_updated_at BEFORE UPDATE ON patient_allergies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_patient_medications_updated_at BEFORE UPDATE ON patient_medications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_emergency_contacts_updated_at BEFORE UPDATE ON patient_emergency_contacts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================
COMMENT ON TABLE patients IS 'Core patient demographic and identification information';
COMMENT ON TABLE patient_addresses IS 'Patient addresses with support for multiple address types';
COMMENT ON TABLE patient_insurance IS 'Patient insurance policies and coverage information';
COMMENT ON TABLE medical_records IS 'Comprehensive medical records including visits, procedures, and results';
COMMENT ON TABLE patient_timeline IS 'Unified timeline of all patient healthcare events for care coordination';
COMMENT ON TABLE lab_orders IS 'Lab test orders initiated from patient service';
COMMENT ON TABLE patient_allergies IS 'Patient allergy and adverse reaction information';
COMMENT ON TABLE patient_medications IS 'Current and historical medication list';
COMMENT ON TABLE patient_emergency_contacts IS 'Patient emergency contact information';

-- =====================================================
-- SEED DATA (Development Only)
-- =====================================================
-- Insert sample patient for testing
INSERT INTO patients (patient_id, tenant_id, mrn, ssn, first_name, last_name, date_of_birth, gender, phone, email)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'default', 'MRN-TEST-001', '123-45-6789', 'John', 'Doe', '1980-01-15', 'MALE', '555-1234', 'john.doe@example.com'),
    ('b1ffcd99-9c0b-4ef8-bb6d-6bb9bd380a22', 'default', 'MRN-TEST-002', '987-65-4321', 'Jane', 'Smith', '1990-05-20', 'FEMALE', '555-5678', 'jane.smith@example.com');

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Patient Service comprehensive schema created successfully';
END $$;
