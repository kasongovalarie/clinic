package com.hologram.clinic.model;

public class StandardTicket extends QueueTicket {
    public StandardTicket(int ticketNumber, Patient patient) {
        super(ticketNumber, patient, false);
    }
}
