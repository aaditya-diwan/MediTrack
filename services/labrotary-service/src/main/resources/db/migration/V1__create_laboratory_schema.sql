-- MediTrack Laboratory Service - Comprehensive Schema per Technical Specification
-- This migration creates the complete laboratory data model

-- =====================================================
-- LAB ORDERS TABLE (Aggregate Root)
-- =====================================================
CREATE TABLE lab_orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL,
    mrn VARCHAR(20) NOT NULL,
    ordering_provider_id VARCHAR(50) NOT NULL,
    ordering_provider_name VARCHAR(255) NOT NULL,
    ordering_facility_id VARCHAR(50),
    ordering_facility_name VARCHAR(255),
    order_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    priority VARCHAR(20) NOT NULL DEFAULT 'ROUTINE',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    specimen_collected_at TIMESTAMP WITH TIME ZONE,
    specimen_received_at TIMESTAMP WITH TIME ZONE,
    results_reported_at TIMESTAMP WITH TIME ZONE,
    clinical_notes TEXT,
    fasting_required BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER DEFAULT 0,
    CONSTRAINT chk_priority CHECK (priority IN ('STAT', 'URGENT', 'ROUTINE')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'COLLECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ON_HOLD'))
);

-- =====================================================
-- LAB TESTS (Individual tests within an order)
-- =====================================================
CREATE TABLE lab_tests (
    test_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab_orders(order_id) ON DELETE CASCADE,
    test_code VARCHAR(50) NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    loinc_code VARCHAR(20),
    test_category VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    result_value VARCHAR(500),
    result_unit VARCHAR(50),
    reference_range_low DECIMAL(15,4),
    reference_range_high DECIMAL(15,4),
    reference_range_text VARCHAR(100),
    abnormal_flag VARCHAR(10),
    critical_flag BOOLEAN DEFAULT FALSE,
    result_comments TEXT,
    performed_at TIMESTAMP WITH TIME ZONE,
    performed_by VARCHAR(100),
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(100),
    result_status VARCHAR(20),
    method VARCHAR(100),
    equipment_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_test_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED')),
    CONSTRAINT chk_abnormal_flag CHECK (abnormal_flag IN ('L', 'H', 'LL', 'HH', 'N', 'A', NULL)),
    CONSTRAINT chk_result_status CHECK (result_status IN ('PRELIMINARY', 'FINAL', 'CORRECTED', 'CANCELLED', NULL))
);

-- =====================================================
-- DIAGNOSIS CODES (ICD-10 codes for orders)
-- =====================================================
CREATE TABLE order_diagnoses (
    diagnosis_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab_orders(order_id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    code_system VARCHAR(20) NOT NULL DEFAULT 'ICD-10',
    description TEXT,
    is_primary BOOLEAN DEFAULT FALSE,
    sequence_number INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- SPECIMENS
-- =====================================================
CREATE TABLE specimens (
    specimen_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab_orders(order_id) ON DELETE CASCADE,
    specimen_number VARCHAR(50) UNIQUE NOT NULL,
    specimen_type VARCHAR(50) NOT NULL,
    collection_method VARCHAR(50),
    collection_date TIMESTAMP WITH TIME ZONE,
    collection_site VARCHAR(100),
    collected_by VARCHAR(100),
    received_date TIMESTAMP WITH TIME ZONE,
    received_by VARCHAR(100),
    condition VARCHAR(50),
    condition_notes TEXT,
    volume VARCHAR(50),
    container_type VARCHAR(50),
    number_of_containers INTEGER DEFAULT 1,
    rejected BOOLEAN DEFAULT FALSE,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_specimen_type CHECK (specimen_type IN ('BLOOD', 'SERUM', 'PLASMA', 'URINE', 'TISSUE', 'SWAB', 'CSF', 'STOOL', 'SPUTUM', 'OTHER')),
    CONSTRAINT chk_condition CHECK (condition IN ('ACCEPTABLE', 'HEMOLYZED', 'CLOTTED', 'INSUFFICIENT', 'CONTAMINATED', 'LEAKED', 'OTHER'))
);

-- =====================================================
-- REFERENCE RANGES (Normal value ranges)
-- =====================================================
CREATE TABLE reference_ranges (
    range_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_code VARCHAR(50) NOT NULL,
    loinc_code VARCHAR(20),
    age_min INTEGER, -- in years
    age_max INTEGER, -- in years
    gender VARCHAR(20),
    range_low DECIMAL(15,4),
    range_high DECIMAL(15,4),
    range_text VARCHAR(100),
    unit VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    effective_date DATE,
    end_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_ref_gender CHECK (gender IN ('MALE', 'FEMALE', 'ALL', NULL))
);

-- =====================================================
-- LAB EQUIPMENT
-- =====================================================
CREATE TABLE lab_equipment (
    equipment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipment_code VARCHAR(50) UNIQUE NOT NULL,
    equipment_name VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(255),
    model_number VARCHAR(100),
    serial_number VARCHAR(100),
    equipment_type VARCHAR(50),
    location VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_calibration_date DATE,
    next_calibration_date DATE,
    last_maintenance_date DATE,
    next_maintenance_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT chk_equipment_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'RETIRED'))
);

-- =====================================================
-- TEST CATALOG (Available tests)
-- =====================================================
CREATE TABLE test_catalog (
    catalog_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_code VARCHAR(50) UNIQUE NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    loinc_code VARCHAR(20),
    test_category VARCHAR(50),
    description TEXT,
    specimen_type VARCHAR(50),
    specimen_volume VARCHAR(50),
    turnaround_time_hours INTEGER,
    requires_fasting BOOLEAN DEFAULT FALSE,
    special_instructions TEXT,
    cost DECIMAL(10,2),
    cpt_code VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =====================================================
-- ORDER AUDIT LOG
-- =====================================================
CREATE TABLE lab_order_audit (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES lab_orders(order_id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    changed_by VARCHAR(100) NOT NULL,
    change_reason TEXT,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ip_address VARCHAR(50),
    user_agent TEXT
);

-- =====================================================
-- QUALITY CONTROL RESULTS
-- =====================================================
CREATE TABLE quality_control (
    qc_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_code VARCHAR(50) NOT NULL,
    equipment_id UUID REFERENCES lab_equipment(equipment_id),
    qc_level VARCHAR(20),
    control_lot VARCHAR(50),
    expected_value DECIMAL(15,4),
    measured_value DECIMAL(15,4),
    unit VARCHAR(50),
    within_range BOOLEAN,
    performed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    performed_by VARCHAR(100),
    comments TEXT,
    CONSTRAINT chk_qc_level CHECK (qc_level IN ('LOW', 'NORMAL', 'HIGH'))
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Lab orders indexes
CREATE INDEX idx_lab_orders_patient ON lab_orders(patient_id);
CREATE INDEX idx_lab_orders_mrn ON lab_orders(mrn);
CREATE INDEX idx_lab_orders_status ON lab_orders(status);
CREATE INDEX idx_lab_orders_priority ON lab_orders(priority);
CREATE INDEX idx_lab_orders_date ON lab_orders(order_date DESC);
CREATE INDEX idx_lab_orders_provider ON lab_orders(ordering_provider_id);
CREATE INDEX idx_lab_orders_facility ON lab_orders(ordering_facility_id);

-- Lab tests indexes
CREATE INDEX idx_lab_tests_order ON lab_tests(order_id);
CREATE INDEX idx_lab_tests_code ON lab_tests(test_code);
CREATE INDEX idx_lab_tests_status ON lab_tests(status);
CREATE INDEX idx_lab_tests_critical ON lab_tests(critical_flag) WHERE critical_flag = TRUE;
CREATE INDEX idx_lab_tests_loinc ON lab_tests(loinc_code);

-- Diagnosis indexes
CREATE INDEX idx_order_diagnoses_order ON order_diagnoses(order_id);
CREATE INDEX idx_order_diagnoses_code ON order_diagnoses(code);

-- Specimen indexes
CREATE INDEX idx_specimens_order ON specimens(order_id);
CREATE INDEX idx_specimens_number ON specimens(specimen_number);
CREATE INDEX idx_specimens_type ON specimens(specimen_type);
CREATE INDEX idx_specimens_received ON specimens(received_date DESC);

-- Reference range indexes
CREATE INDEX idx_reference_ranges_test ON reference_ranges(test_code);
CREATE INDEX idx_reference_ranges_active ON reference_ranges(is_active) WHERE is_active = TRUE;

-- Equipment indexes
CREATE INDEX idx_equipment_code ON lab_equipment(equipment_code);
CREATE INDEX idx_equipment_status ON lab_equipment(status);

-- Catalog indexes
CREATE INDEX idx_test_catalog_code ON test_catalog(test_code);
CREATE INDEX idx_test_catalog_active ON test_catalog(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_test_catalog_category ON test_catalog(test_category);

-- Audit indexes
CREATE INDEX idx_lab_audit_order ON lab_order_audit(order_id);
CREATE INDEX idx_lab_audit_date ON lab_order_audit(changed_at DESC);

-- QC indexes
CREATE INDEX idx_qc_test ON quality_control(test_code);
CREATE INDEX idx_qc_equipment ON quality_control(equipment_id);
CREATE INDEX idx_qc_date ON quality_control(performed_at DESC);

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
CREATE TRIGGER update_lab_orders_updated_at BEFORE UPDATE ON lab_orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_lab_tests_updated_at BEFORE UPDATE ON lab_tests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_specimens_updated_at BEFORE UPDATE ON specimens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reference_ranges_updated_at BEFORE UPDATE ON reference_ranges
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_lab_equipment_updated_at BEFORE UPDATE ON lab_equipment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_test_catalog_updated_at BEFORE UPDATE ON test_catalog
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to audit order status changes
CREATE OR REPLACE FUNCTION audit_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        INSERT INTO lab_order_audit (order_id, action, old_status, new_status, changed_by)
        VALUES (NEW.order_id, 'STATUS_CHANGE', OLD.status, NEW.status, NEW.updated_by);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_lab_order_status AFTER UPDATE ON lab_orders
    FOR EACH ROW EXECUTE FUNCTION audit_order_status_change();

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================
COMMENT ON TABLE lab_orders IS 'Laboratory test orders from providers';
COMMENT ON TABLE lab_tests IS 'Individual tests within a lab order with results';
COMMENT ON TABLE order_diagnoses IS 'ICD-10 diagnosis codes associated with lab orders';
COMMENT ON TABLE specimens IS 'Physical specimens collected for testing';
COMMENT ON TABLE reference_ranges IS 'Normal value ranges for lab tests by demographics';
COMMENT ON TABLE lab_equipment IS 'Laboratory equipment and instrument tracking';
COMMENT ON TABLE test_catalog IS 'Catalog of available laboratory tests';
COMMENT ON TABLE lab_order_audit IS 'Audit trail of all order changes';
COMMENT ON TABLE quality_control IS 'Quality control test results for equipment validation';

-- =====================================================
-- SEED DATA (Development Only)
-- =====================================================

-- Insert sample test catalog
INSERT INTO test_catalog (test_code, test_name, loinc_code, test_category, specimen_type, turnaround_time_hours, cpt_code)
VALUES
    ('CBC', 'Complete Blood Count', '58410-2', 'HEMATOLOGY', 'BLOOD', 4, '85025'),
    ('BMP', 'Basic Metabolic Panel', '51990-0', 'CHEMISTRY', 'BLOOD', 4, '80048'),
    ('CMP', 'Comprehensive Metabolic Panel', '24323-8', 'CHEMISTRY', 'BLOOD', 4, '80053'),
    ('LIPID', 'Lipid Panel', '24331-1', 'CHEMISTRY', 'BLOOD', 4, '80061'),
    ('HBA1C', 'Hemoglobin A1c', '4548-4', 'CHEMISTRY', 'BLOOD', 2, '83036'),
    ('TSH', 'Thyroid Stimulating Hormone', '3016-3', 'CHEMISTRY', 'BLOOD', 24, '84443'),
    ('UA', 'Urinalysis', '24356-8', 'URINALYSIS', 'URINE', 2, '81001'),
    ('CULTURE', 'Blood Culture', '600-7', 'MICROBIOLOGY', 'BLOOD', 72, '87040');

-- Insert reference ranges for common tests
INSERT INTO reference_ranges (test_code, gender, range_low, range_high, range_text, unit)
VALUES
    ('CBC', 'MALE', 4.5, 5.5, '4.5-5.5', 'M/uL'),
    ('CBC', 'FEMALE', 4.0, 5.0, '4.0-5.0', 'M/uL'),
    ('HBA1C', 'ALL', 4.0, 5.6, '4.0-5.6', '%'),
    ('TSH', 'ALL', 0.4, 4.0, '0.4-4.0', 'mIU/L');

-- Insert sample equipment
INSERT INTO lab_equipment (equipment_code, equipment_name, manufacturer, equipment_type, status)
VALUES
    ('CBC-001', 'Hematology Analyzer XYZ', 'Sysmex', 'HEMATOLOGY', 'ACTIVE'),
    ('CHEM-001', 'Chemistry Analyzer ABC', 'Roche', 'CHEMISTRY', 'ACTIVE'),
    ('MICRO-001', 'Blood Culture System', 'BD', 'MICROBIOLOGY', 'ACTIVE');

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Laboratory Service schema created successfully';
END $$;
