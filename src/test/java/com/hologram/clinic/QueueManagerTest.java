package com.hologram.clinic;

import com.hologram.clinic.model.QueueTicket;
import com.hologram.clinic.service.QueueManager;
import com.hologram.clinic.storage.FileQueueStorage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueueManagerTest {

    @Test
    void shouldSerializeQueueToJsonWithoutJavaObjectMapper() {
        Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "clinic-queue-json-test.json");
        QueueManager manager = new QueueManager(new FileQueueStorage(tempFile.toString()));

        manager.registerPatient("P-3001", "Dana", true);
        String json = new com.google.gson.Gson().toJson(manager.getTickets());

        assertTrue(json.contains("Dana"));
        assertTrue(json.contains("ticketNumber"));
    }

    @Test
    void shouldRegisterPatientAndReturnTicket() throws Exception {
        Path tempFile = Files.createTempFile("clinic-queue", ".json");
        QueueManager manager = new QueueManager(new FileQueueStorage(tempFile.toString()));

        QueueTicket ticket = manager.registerPatient("P-1001", "Alice", false);

        assertEquals(1, ticket.getTicketNumber());
        assertEquals("Alice", ticket.getPatient().getName());
        assertEquals(QueueTicket.Status.WAITING, ticket.getStatus());
        assertFalse(ticket.isUrgent());
    }

    @Test
    void shouldPrioritizeUrgentTickets() throws Exception {
        Path tempFile = Files.createTempFile("clinic-queue", ".json");
        QueueManager manager = new QueueManager(new FileQueueStorage(tempFile.toString()));

        manager.registerPatient("P-2001", "Bob", false);
        manager.registerPatient("P-2002", "Carol", true);

        List<QueueTicket> queue = manager.getTickets();
        assertTrue(queue.get(0).isUrgent());
        assertEquals("Carol", queue.get(0).getPatient().getName());
    }

    @Test
    void shouldEditTicketPatientDetails() throws Exception {
        Path tempFile = Files.createTempFile("clinic-queue", ".json");
        QueueManager manager = new QueueManager(new FileQueueStorage(tempFile.toString()));

        manager.registerPatient("P-3001", "Diana", false);
        manager.editTicket(1, "P-9001", "Diana Smith", true);

        QueueTicket updated = manager.getTicketByNumber(1);
        assertEquals("P-9001", updated.getPatient().getPatientId());
        assertEquals("Diana Smith", updated.getPatient().getName());
        assertTrue(updated.isUrgent());
    }

    @Test
    void shouldDeleteTicketFromQueue() throws Exception {
        Path tempFile = Files.createTempFile("clinic-queue", ".json");
        QueueManager manager = new QueueManager(new FileQueueStorage(tempFile.toString()));

        manager.registerPatient("P-4001", "Ethan", false);
        manager.registerPatient("P-4002", "Faith", false);
        manager.deleteTicket(1);

        List<QueueTicket> queue = manager.getTickets();
        assertEquals(1, queue.size());
        assertEquals("Faith", queue.get(0).getPatient().getName());
    }
}
