package com.hologram.clinic.model;

public class QueueTicket {
    public enum Status {
        WAITING,
        BEING_ATTENDED_TO,
        COMPLETED,
        CANCELLED
    }

    private int ticketNumber;
    private Patient patient;
    private Status status;
    private boolean urgent;

    public QueueTicket() {
    }

    public QueueTicket(int ticketNumber, Patient patient, boolean urgent) {
        this.ticketNumber = ticketNumber;
        this.patient = patient;
        this.status = Status.WAITING;
        this.urgent = urgent;
    }

    public int getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(int ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getPriorityLabel() {
        return urgent ? "Urgent" : "Standard";
    }
}
