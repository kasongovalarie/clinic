package com.hologram.clinic.service;

import com.hologram.clinic.exception.InvalidTicketStatusException;
import com.hologram.clinic.exception.TicketNotFoundException;
import com.hologram.clinic.model.QueueTicket;
import com.hologram.clinic.storage.FileQueueStorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class QueueManager {
    private final List<QueueTicket> tickets = new ArrayList<>();
    private final FileQueueStorage storage;
    private int nextTicketNumber = 1;

    public QueueManager(FileQueueStorage storage) {
        this.storage = storage;
        loadSavedTickets();
    }

    public QueueTicket registerPatient(String patientId, String name, boolean urgent) {
        if (patientId == null || patientId.isBlank()) {
            throw new IllegalArgumentException("Patient ID is required.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Patient name is required.");
        }

        QueueTicket ticket;
        if (urgent) {
            ticket = new QueueTicket(nextTicketNumber, new com.hologram.clinic.model.Patient(patientId, name), true);
        } else {
            ticket = new QueueTicket(nextTicketNumber, new com.hologram.clinic.model.Patient(patientId, name), false);
        }

        tickets.add(ticket);
        nextTicketNumber++;
        saveTickets();
        return ticket;
    }

    public List<QueueTicket> getTickets() {
        return tickets.stream()
                .sorted(Comparator
                        .comparing((QueueTicket ticket) -> !ticket.isUrgent())
                        .thenComparingInt(QueueTicket::getTicketNumber))
                .collect(Collectors.toList());
    }

    public QueueTicket getTicketByNumber(int ticketNumber) throws TicketNotFoundException {
        return tickets.stream()
                .filter(ticket -> ticket.getTicketNumber() == ticketNumber)
                .findFirst()
                .orElseThrow(() -> new TicketNotFoundException("Ticket " + ticketNumber + " was not found."));
    }

    public void updateTicketStatus(int ticketNumber, String statusText) throws TicketNotFoundException, InvalidTicketStatusException {
        QueueTicket ticket = getTicketByNumber(ticketNumber);
        QueueTicket.Status status = parseStatus(statusText);
        ticket.setStatus(status);
        saveTickets();
    }

    public void setUrgent(int ticketNumber, boolean urgent) throws TicketNotFoundException {
        QueueTicket ticket = getTicketByNumber(ticketNumber);
        ticket.setUrgent(urgent);
        saveTickets();
    }

    public void editTicket(int ticketNumber, String patientId, String name, boolean urgent) throws TicketNotFoundException {
        QueueTicket ticket = getTicketByNumber(ticketNumber);
        if (patientId == null || patientId.isBlank()) {
            throw new IllegalArgumentException("Patient ID is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Patient name is required.");
        }

        ticket.getPatient().setPatientId(patientId);
        ticket.getPatient().setName(name);
        ticket.setUrgent(urgent);
        saveTickets();
    }

    public void cancelTicket(int ticketNumber) throws TicketNotFoundException {
        QueueTicket ticket = getTicketByNumber(ticketNumber);
        ticket.setStatus(QueueTicket.Status.CANCELLED);
        saveTickets();
    }

    public void deleteTicket(int ticketNumber) throws TicketNotFoundException {
        QueueTicket ticket = getTicketByNumber(ticketNumber);
        tickets.remove(ticket);
        saveTickets();
    }

    public QueueTicket getNextTicket() {
        if (tickets.isEmpty()) {
            return null;
        }

        return getTickets().get(0);
    }

    public QueueTicket.Status parseStatus(String statusText) throws InvalidTicketStatusException {
        if (statusText == null || statusText.isBlank()) {
            throw new InvalidTicketStatusException("Ticket status is required.");
        }

        String normalized = statusText.trim().replace('-', '_').replace(' ', '_').toUpperCase();

        if (normalized.equals("BEING_ATTENDED_TO")) {
            return QueueTicket.Status.BEING_ATTENDED_TO;
        }

        try {
            return QueueTicket.Status.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new InvalidTicketStatusException("Invalid status: " + statusText);
        }
    }

    private void loadSavedTickets() {
        if (storage == null) {
            return;
        }

        List<QueueTicket> savedTickets = storage.load();
        if (savedTickets != null && !savedTickets.isEmpty()) {
            tickets.addAll(savedTickets);
            nextTicketNumber = tickets.stream()
                    .mapToInt(QueueTicket::getTicketNumber)
                    .max()
                    .orElse(0) + 1;
        }
    }

    private void saveTickets() {
        if (storage != null) {
            storage.save(tickets);
        }
    }
}
