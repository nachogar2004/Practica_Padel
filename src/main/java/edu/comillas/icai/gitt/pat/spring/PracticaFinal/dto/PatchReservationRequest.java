package edu.comillas.icai.gitt.pat.spring.PracticaFinal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class PatchReservationRequest {

    // En JSON: "date": "2026-02-20"
    private LocalDate date;

    // En JSON: "startTime": "19:00"
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    // En JSON: "durationMinutes": 60
    private Integer durationMinutes;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
}
