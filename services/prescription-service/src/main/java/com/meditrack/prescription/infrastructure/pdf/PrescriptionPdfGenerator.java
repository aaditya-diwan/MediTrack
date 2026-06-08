package com.meditrack.prescription.infrastructure.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.meditrack.prescription.domain.model.Prescription;
import com.meditrack.prescription.domain.model.PrescriptionLabOrder;
import com.meditrack.prescription.domain.model.PrescriptionMedication;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Component
public class PrescriptionPdfGenerator {

    public byte[] generatePdf(Prescription prescription) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("MediTrack Hospital").setBold().setFontSize(18));
            doc.add(new Paragraph("PRESCRIPTION").setBold().setFontSize(14));
            doc.add(new Paragraph("Prescription ID: " + prescription.getId()));
            doc.add(new Paragraph("Patient ID: " + prescription.getPatientId()));
            doc.add(new Paragraph("Doctor ID: " + prescription.getDoctorId()));
            if (prescription.getIssuedAt() != null) {
                doc.add(new Paragraph("Date: " + prescription.getIssuedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))));
            }

            if (prescription.getConsultationNotes() != null && !prescription.getConsultationNotes().isEmpty()) {
                doc.add(new Paragraph("Clinical Notes: " + prescription.getConsultationNotes()));
            }

            if (prescription.getMedications() != null && !prescription.getMedications().isEmpty()) {
                doc.add(new Paragraph("MEDICATIONS").setBold().setFontSize(12));
                Table medTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2}));
                medTable.setWidth(UnitValue.createPercentValue(100));
                medTable.addHeaderCell("Medication");
                medTable.addHeaderCell("Dosage");
                medTable.addHeaderCell("Frequency");
                medTable.addHeaderCell("Duration");
                medTable.addHeaderCell("Route");
                for (PrescriptionMedication m : prescription.getMedications()) {
                    medTable.addCell(m.getMedicationName());
                    medTable.addCell(m.getDosage());
                    medTable.addCell(m.getFrequency());
                    medTable.addCell(m.getDuration() != null ? m.getDuration() : "");
                    medTable.addCell(m.getRoute() != null ? m.getRoute() : "Oral");
                }
                doc.add(medTable);
            }

            if (prescription.getLabOrders() != null && !prescription.getLabOrders().isEmpty()) {
                doc.add(new Paragraph("LAB INVESTIGATIONS").setBold().setFontSize(12));
                Table labTable = new Table(UnitValue.createPercentArray(new float[]{2, 3, 3, 2}));
                labTable.setWidth(UnitValue.createPercentValue(100));
                labTable.addHeaderCell("Test Code");
                labTable.addHeaderCell("Test Name");
                labTable.addHeaderCell("Clinical Indication");
                labTable.addHeaderCell("Urgency");
                for (PrescriptionLabOrder l : prescription.getLabOrders()) {
                    labTable.addCell(l.getTestCode());
                    labTable.addCell(l.getTestName());
                    labTable.addCell(l.getClinicalIndication() != null ? l.getClinicalIndication() : "");
                    labTable.addCell(l.getUrgency() != null ? l.getUrgency().name() : "ROUTINE");
                }
                doc.add(labTable);
            }

            doc.add(new Paragraph("Doctor Signature: ________________________").setMarginTop(30));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate prescription PDF", e);
        }
        return baos.toByteArray();
    }
}
