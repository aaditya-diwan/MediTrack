-- Migration: Add lab_results table for storing test results
-- This complements the existing lab_tests table with detailed result tracking

-- =====================================================
-- LAB RESULTS TABLE
-- =====================================================
CREATE TABLE lab_results (
    result_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    test_code VARCHAR(50) NOT NULL,
    test_name VARCHAR(255) NOT NULL,
    loinc_code VARCHAR(20),
    result_value VARCHAR(500) NOT NULL,
    result_unit VARCHAR(50),
    reference_range VARCHAR(100),
    abnormal_flag VARCHAR(20),
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(100),
    verified_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_result_order FOREIGN KEY (order_id)
        REFERENCES lab_orders(order_id) ON DELETE CASCADE,
    CONSTRAINT chk_result_status CHECK (status IN ('PRELIMINARY', 'FINAL', 'CORRECTED', 'AMENDED')),
    CONSTRAINT chk_abnormal_flag_result CHECK (abnormal_flag IN ('NORMAL', 'LOW', 'HIGH', 'CRITICALLY_LOW', 'CRITICALLY_HIGH', 'ABNORMAL'))
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================
CREATE INDEX idx_lab_results_order ON lab_results(order_id);
CREATE INDEX idx_lab_results_test_code ON lab_results(test_code);
CREATE INDEX idx_lab_results_status ON lab_results(status);
CREATE INDEX idx_lab_results_abnormal_flag ON lab_results(abnormal_flag);
CREATE INDEX idx_lab_results_performed_at ON lab_results(performed_at DESC);

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================
COMMENT ON TABLE lab_results IS 'Stores completed lab test results with full result details';
COMMENT ON COLUMN lab_results.result_id IS 'Unique identifier for the result';
COMMENT ON COLUMN lab_results.order_id IS 'Reference to the lab order';
COMMENT ON COLUMN lab_results.abnormal_flag IS 'Indicates if result is normal, abnormal, or critical';
COMMENT ON COLUMN lab_results.status IS 'Result lifecycle status (PRELIMINARY, FINAL, CORRECTED, AMENDED)';
COMMENT ON COLUMN lab_results.verified_at IS 'When the result was verified by a supervisor/pathologist';
