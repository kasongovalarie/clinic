package com.hologram.clinic.model;

public class UrgentTicket extends QueueTicket {
    public UrgentTicket(int ticketNumber, Patient patient) {
        super(ticketNumber, patient, true);
    }
}
