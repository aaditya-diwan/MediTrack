package com.meditrack.appointment.infrastructure.persistence.mapper;

import com.meditrack.appointment.domain.model.Appointment;
import com.meditrack.appointment.domain.model.AppointmentStatus;
import com.meditrack.appointment.domain.model.AppointmentType;
import com.meditrack.appointment.infrastructure.persistence.entity.AppointmentEntity;
import org.springframework.stereotype.Component;

@Component
public class AppointmentPersistenceMapper {

    public AppointmentEntity toEntity(Appointment a) {
        AppointmentEntity e = new AppointmentEntity();
        e.setId(a.getId());
        e.setPatientId(a.getPatientId());
        e.setDoctorId(a.getDoctorId());
        e.setStatus(a.getStatus().name());
        e.setType(a.getType().name());
        e.setReasonForVisit(a.getReasonForVisit());
        e.setNotes(a.getNotes());
        e.setScheduledAt(a.getScheduledAt());
        e.setActualStartAt(a.getActualStartAt());
        e.setActualEndAt(a.getActualEndAt());
        return e;
    }

    public Appointment toDomain(AppointmentEntity e) {
        return Appointment.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .doctorId(e.getDoctorId())
                .status(AppointmentStatus.valueOf(e.getStatus()))
                .type(AppointmentType.valueOf(e.getType()))
                .reasonForVisit(e.getReasonForVisit())
                .notes(e.getNotes())
                .scheduledAt(e.getScheduledAt())
                .actualStartAt(e.getActualStartAt())
                .actualEndAt(e.getActualEndAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
