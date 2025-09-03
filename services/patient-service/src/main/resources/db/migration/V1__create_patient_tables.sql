CREATE TABLE patients (
    id UUID PRIMARY KEY,
    mrn VARCHAR(255) UNIQUE,
    ssn VARCHAR(255) UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    date_of_birth DATE,
    email VARCHAR(255),
    phone_number VARCHAR(255),
    address VARCHAR(255),
    insurance_provider VARCHAR(255),
    insurance_policy_number VARCHAR(255)
);

CREATE TABLE medical_records (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    diagnosis VARCHAR(255),
    treatment VARCHAR(255),
    date DATE,
    FOREIGN KEY (patient_id) REFERENCES patients(id)
);
