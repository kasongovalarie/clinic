package com.hologram.clinic;

import com.google.gson.Gson;
import com.hologram.clinic.exception.InvalidTicketStatusException;
import com.hologram.clinic.exception.TicketNotFoundException;
import com.hologram.clinic.model.Patient;
import com.hologram.clinic.model.QueueTicket;
import com.hologram.clinic.service.QueueManager;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClinicQueueApp {
    private final QueueManager queueManager;
    private final Gson gson = new Gson();

    public ClinicQueueApp(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    public void start(int port) {
        Javalin app = Javalin.create(config -> {
            config.plugins.enableDevLogging();
        });

        app.get("/", ctx -> {
            ctx.contentType("text/html");
            ctx.result(loadHtml());
        });

        app.get("/api/tickets", ctx -> {
            List<QueueTicket> tickets = queueManager.getTickets();
            ctx.contentType("application/json");
            ctx.result(toJson(tickets));
        });

        app.get("/api/tickets/next", ctx -> {
            QueueTicket next = queueManager.getNextTicket();
            if (next == null) {
                ctx.status(404).result("No tickets in queue.");
                return;
            }
            ctx.contentType("application/json");
            ctx.result(toJson(next));
        });

        app.post("/api/tickets", ctx -> {
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);
            String patientId = String.valueOf(body.getOrDefault("patientId", ""));
            String name = String.valueOf(body.getOrDefault("name", ""));
            boolean urgent = Boolean.parseBoolean(String.valueOf(body.getOrDefault("urgent", false)));

            QueueTicket created = queueManager.registerPatient(patientId, name, urgent);
            ctx.status(201);
            ctx.contentType("application/json");
            ctx.result(toJson(created));
        });

        app.put("/api/tickets/{ticketNumber}/status", ctx -> {
            int ticketNumber = Integer.parseInt(ctx.pathParam("ticketNumber"));
            String statusText = ctx.body();
            try {
                queueManager.updateTicketStatus(ticketNumber, statusText);
                ctx.contentType("application/json");
                ctx.result(toJson(queueManager.getTicketByNumber(ticketNumber)));
            } catch (TicketNotFoundException | InvalidTicketStatusException e) {
                ctx.status(400);
                ctx.contentType("application/json");
                ctx.result(toJson(errorResponse(e.getMessage())));
            }
        });

        app.put("/api/tickets/{ticketNumber}/urgent", ctx -> {
            int ticketNumber = Integer.parseInt(ctx.pathParam("ticketNumber"));
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);
            boolean urgent = Boolean.parseBoolean(String.valueOf(body.getOrDefault("urgent", false)));
            try {
                queueManager.setUrgent(ticketNumber, urgent);
                ctx.contentType("application/json");
                ctx.result(toJson(queueManager.getTicketByNumber(ticketNumber)));
            } catch (TicketNotFoundException e) {
                ctx.status(404);
                ctx.contentType("application/json");
                ctx.result(toJson(errorResponse(e.getMessage())));
            }
        });

        app.put("/api/tickets/{ticketNumber}/edit", ctx -> {
            int ticketNumber = Integer.parseInt(ctx.pathParam("ticketNumber"));
            Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);
            String patientId = String.valueOf(body.getOrDefault("patientId", ""));
            String name = String.valueOf(body.getOrDefault("name", ""));
            boolean urgent = Boolean.parseBoolean(String.valueOf(body.getOrDefault("urgent", false)));
            try {
                queueManager.editTicket(ticketNumber, patientId, name, urgent);
                ctx.contentType("application/json");
                ctx.result(toJson(queueManager.getTicketByNumber(ticketNumber)));
            } catch (IllegalArgumentException | TicketNotFoundException e) {
                ctx.status(400);
                ctx.contentType("application/json");
                ctx.result(toJson(errorResponse(e.getMessage())));
            }
        });

        app.delete("/api/tickets/{ticketNumber}", ctx -> {
            int ticketNumber = Integer.parseInt(ctx.pathParam("ticketNumber"));
            try {
                queueManager.deleteTicket(ticketNumber);
                ctx.contentType("application/json");
                ctx.result(toJson(Map.of("message", "Ticket deleted successfully")));
            } catch (TicketNotFoundException e) {
                ctx.status(404);
                ctx.contentType("application/json");
                ctx.result(toJson(errorResponse(e.getMessage())));
            }
        });

        app.start(port);
    }

    String toJson(Object value) {
        return gson.toJson(value);
    }

    private String loadHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Clinic Queue Management</title>
                <style>
                    :root {
                        --bg: #eef4fb;
                        --bg-strong: #eaf6f1;
                        --panel: #ffffff;
                        --primary: #0f5bd9;
                        --primary-deep: #0a3ea3;
                        --primary-soft: #e8f0ff;
                        --success: #15803d;
                        --warning: #d97706;
                        --danger: #b91c1c;
                        --text: #122033;
                        --muted: #5d6b7b;
                        --line: #dfe8f2;
                        --shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
                    }

                    * { box-sizing: border-box; }

                    body {
                        margin: 0;
                        background: linear-gradient(135deg, var(--bg) 0%, #f9fbff 50%, var(--bg-strong) 100%);
                        font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
                        color: var(--text);
                    }

                    .shell {
                        max-width: 1180px;
                        margin: 36px auto;
                        padding: 20px;
                    }

                    .topbar {
                        background: linear-gradient(135deg, var(--primary) 0%, var(--primary-deep) 100%);
                        color: #ffffff;
                        border-radius: 22px;
                        padding: 28px 30px;
                        box-shadow: var(--shadow);
                        margin-bottom: 24px;
                        position: relative;
                        overflow: hidden;
                    }

                    .topbar::after {
                        content: "";
                        position: absolute;
                        inset: 0 0 auto auto;
                        width: 220px;
                        height: 220px;
                        background: rgba(255,255,255,0.08);
                        border-radius: 50%;
                        transform: translate(30%, -25%);
                    }

                    .topbar h1 {
                        margin: 0;
                        font-size: clamp(2rem, 3vw, 2.9rem);
                        letter-spacing: -0.04em;
                        position: relative;
                        z-index: 1;
                    }

                    .topbar p {
                        margin: 10px 0 0;
                        color: rgba(255,255,255,0.85);
                        font-size: 1rem;
                        max-width: 680px;
                        position: relative;
                        z-index: 1;
                    }

                    .panel {
                        background: var(--panel);
                        border: 1px solid var(--line);
                        border-radius: 20px;
                        box-shadow: var(--shadow);
                        padding: 24px;
                        margin-bottom: 22px;
                    }

                    .panel h2 {
                        margin: 0 0 18px;
                        font-size: 1.45rem;
                        letter-spacing: -0.02em;
                    }

                    .section-head {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        gap: 12px;
                        margin-bottom: 18px;
                    }

                    .section-head h2 {
                        margin: 0;
                    }

                    .form-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                        gap: 14px;
                        align-items: end;
                    }

                    input, select, button {
                        border-radius: 12px;
                        border: 1px solid var(--line);
                        font-size: 1rem;
                        font-family: inherit;
                    }

                    input, select {
                        width: 100%;
                        padding: 12px 14px;
                        background: #ffffff;
                        color: var(--text);
                        outline: none;
                        transition: border-color 0.2s ease, box-shadow 0.2s ease;
                    }

                    input:focus, select:focus {
                        border-color: rgba(15, 91, 217, 0.45);
                        box-shadow: 0 0 0 4px rgba(15, 91, 217, 0.08);
                    }

                    .check-box {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                        background: #f8fbff;
                        border: 1px solid var(--line);
                        border-radius: 12px;
                        height: 48px;
                        padding: 0 14px;
                        font-weight: 700;
                        color: var(--text);
                    }

                    .check-box input {
                        width: 18px;
                        height: 18px;
                        accent-color: var(--primary);
                    }

                    button {
                        background: linear-gradient(180deg, var(--primary) 0%, var(--primary-deep) 100%);
                        color: white;
                        border: none;
                        padding: 12px 18px;
                        cursor: pointer;
                        font-weight: 700;
                        transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.2s ease;
                        box-shadow: 0 10px 18px rgba(15, 91, 217, 0.16);
                    }

                    button:hover {
                        transform: translateY(-1px);
                        filter: brightness(1.04);
                    }

                    .secondary-btn {
                        background: #edf2f9;
                        color: var(--text);
                        border: 1px solid var(--line);
                        box-shadow: none;
                    }

                    .stats-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
                        gap: 16px;
                        margin-bottom: 22px;
                    }

                    .stat-card {
                        background: var(--panel);
                        border: 1px solid var(--line);
                        border-radius: 18px;
                        padding: 20px 18px;
                        box-shadow: var(--shadow);
                        min-height: 118px;
                    }

                    .stat-card span {
                        display: block;
                        color: var(--muted);
                        font-size: 0.9rem;
                        margin-bottom: 8px;
                        font-weight: 600;
                    }

                    .stat-card strong {
                        font-size: clamp(1.8rem, 2.8vw, 2.3rem);
                        color: var(--text);
                        letter-spacing: -0.04em;
                    }

                    .toolbar {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 10px;
                        margin-bottom: 18px;
                    }

                    .filter-btn {
                        background: #edf2f9;
                        color: var(--text);
                        border: 1px solid var(--line);
                        box-shadow: none;
                    }

                    .filter-btn.active {
                        background: linear-gradient(180deg, var(--primary) 0%, var(--primary-deep) 100%);
                        color: white;
                        border-color: transparent;
                    }

                    .patient-list {
                        list-style: none;
                        padding: 0;
                        margin: 0;
                        display: grid;
                        gap: 14px;
                    }

                    .patient-item {
                        background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
                        border: 1px solid var(--line);
                        border-left: 6px solid var(--primary);
                        border-radius: 16px;
                        padding: 18px 18px 16px;
                        display: grid;
                        gap: 12px;
                        transition: box-shadow 0.2s ease, transform 0.2s ease;
                    }

                    .patient-item:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 10px 18px rgba(15, 23, 42, 0.05);
                    }

                    .patient-item.urgent {
                        border-left-color: var(--warning);
                        background: linear-gradient(180deg, #fffdf8 0%, #fffaf0 100%);
                    }

                    .patient-head {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        flex-wrap: wrap;
                        gap: 12px;
                    }

                    .patient-name {
                        font-size: 1.32rem;
                        font-weight: 700;
                        letter-spacing: -0.02em;
                    }

                    .badge-row {
                        display: flex;
                        gap: 8px;
                        flex-wrap: wrap;
                    }

                    .badge {
                        display: inline-block;
                        padding: 6px 10px;
                        border-radius: 999px;
                        font-size: 0.77rem;
                        font-weight: 700;
                        letter-spacing: 0.01em;
                    }

                    .badge.urgent { background: #fef3c7; color: #92400e; }
                    .badge.standard { background: var(--primary-soft); color: var(--primary); }
                    .badge.waiting { background: #dbeafe; color: #1d4ed8; }
                    .badge.attending { background: #fef3c7; color: #92400e; }
                    .badge.completed { background: #dcfce7; color: var(--success); }
                    .badge.cancelled { background: #fee2e2; color: var(--danger); }

                    .patient-meta {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 12px;
                        color: var(--muted);
                        font-size: 0.95rem;
                    }

                    .patient-actions {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 10px;
                        align-items: center;
                    }

                    .patient-actions select {
                        min-width: 185px;
                    }

                    .empty-state {
                        border: 1px dashed var(--line);
                        border-radius: 14px;
                        background: #f8fafc;
                        padding: 30px;
                        text-align: center;
                        color: var(--muted);
                        font-weight: 700;
                    }

                    .modal-overlay {
                        position: fixed;
                        inset: 0;
                        background: rgba(15, 23, 42, 0.45);
                        display: none;
                        align-items: center;
                        justify-content: center;
                        padding: 18px;
                        z-index: 999;
                    }

                    .modal-overlay.visible {
                        display: flex;
                    }

                    .modal-card {
                        width: min(100%, 500px);
                        background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
                        border-radius: 24px;
                        border: 1px solid rgba(148, 163, 184, 0.36);
                        box-shadow: 0 30px 60px rgba(15, 23, 42, 0.22);
                        padding: 24px;
                        position: relative;
                    }

                    .modal-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        gap: 14px;
                        margin-bottom: 18px;
                        padding-bottom: 12px;
                        border-bottom: 1px solid var(--line);
                    }

                    .modal-header h3 {
                        margin: 0;
                        font-size: 1.45rem;
                        letter-spacing: -0.02em;
                    }

                    .close-modal-btn {
                        width: 38px;
                        height: 38px;
                        border-radius: 50%;
                        background: #eef2ff;
                        color: var(--text);
                        border: 1px solid var(--line);
                        box-shadow: none;
                        font-size: 1.25rem;
                        line-height: 1;
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        padding: 0;
                    }

                    .modal-form {
                        display: grid;
                        gap: 16px;
                    }

                    .modal-form label {
                        display: grid;
                        gap: 8px;
                        font-weight: 600;
                        color: var(--text);
                    }

                    .modal-form input {
                        background: #ffffff;
                        border: 1px solid #dfe8f2;
                    }

                    .modal-actions {
                        display: flex;
                        justify-content: flex-end;
                        gap: 10px;
                        margin-top: 22px;
                    }

                    .confirm-card {
                        width: min(100%, 420px);
                    }

                    .confirm-body {
                        color: var(--muted);
                        font-size: 1rem;
                        line-height: 1.6;
                        margin: 8px 0 22px;
                    }

                    .danger-btn {
                        background: linear-gradient(180deg, #dc2626 0%, #b91c1c 100%);
                        box-shadow: 0 10px 18px rgba(185, 28, 28, 0.18);
                    }

                    @media (max-width: 640px) {
                        .shell { padding: 12px; }
                        .panel, .topbar { padding: 18px; }
                        .patient-head { align-items: flex-start; }
                        .section-head { display: block; }
                    }
                </style>
            </head>
            <body>
            <div class="shell">
                <header class="topbar">
                    <h1>Clinic Queue Management</h1>
                    <p>Track patients, prioritize urgent cases, and update clinic progress in real time.</p>
                </header>

                <section class="panel">
                    <h2>Register Patient</h2>
                    <div class="form-grid">
                        <input id="patientId" type="text" placeholder="Patient ID" />
                        <input id="patientName" type="text" placeholder="Patient Name" />
                        <label class="check-box"><input type="checkbox" id="urgent" /> Mark as Urgent</label>
                        <button type="button" onclick="registerPatient()">Add to Queue</button>
                    </div>
                </section>

                <div class="stats-grid">
                    <div class="stat-card">
                        <span>Total Patients</span>
                        <strong id="totalCount">0</strong>
                    </div>
                    <div class="stat-card">
                        <span>Waiting</span>
                        <strong id="waitingCount">0</strong>
                    </div>
                    <div class="stat-card">
                        <span>Urgent</span>
                        <strong id="urgentCount">0</strong>
                    </div>
                    <div class="stat-card">
                        <span>Next Ticket</span>
                        <strong id="nextTicket">-</strong>
                    </div>
                </div>

                <section class="panel">
                    <div class="section-head">
                        <h2>Patient Queue</h2>
                        <button type="button" class="secondary-btn" onclick="loadQueue()">Refresh</button>
                    </div>

                    <div class="toolbar">
                        <button type="button" class="filter-btn active" data-filter="all">All</button>
                        <button type="button" class="filter-btn" data-filter="WAITING">Waiting</button>
                        <button type="button" class="filter-btn" data-filter="urgent">Urgent</button>
                    </div>

                    <ul id="queueList" class="patient-list"></ul>
                </section>
            </div>

            <div id="editModal" class="modal-overlay" aria-hidden="true">
                <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="editTicketTitle">
                    <div class="modal-header">
                        <h3 id="editTicketTitle">Edit Ticket</h3>
                        <button type="button" class="close-modal-btn" aria-label="Close edit modal" onclick="closeEditModal()">×</button>
                    </div>

                    <div class="modal-form">
                        <label>
                            Patient ID
                            <input id="editPatientId" type="text" placeholder="Patient ID" />
                        </label>

                        <label>
                            Patient Name
                            <input id="editPatientName" type="text" placeholder="Patient Name" />
                        </label>

                        <label class="check-box">
                            <input id="editUrgent" type="checkbox" />
                            Mark as Urgent
                        </label>
                    </div>

                    <div class="modal-actions">
                        <button type="button" onclick="submitEditTicket()">Save Changes</button>
                        <button type="button" class="secondary-btn" onclick="closeEditModal()">Cancel</button>
                    </div>
                </div>
            </div>

            <div id="deleteModal" class="modal-overlay" aria-hidden="true">
                <div class="modal-card confirm-card" role="dialog" aria-modal="true" aria-labelledby="deleteTicketTitle">
                    <div class="modal-header">
                        <h3 id="deleteTicketTitle">Delete Ticket</h3>
                        <button type="button" class="close-modal-btn" aria-label="Close delete modal" onclick="closeDeleteModal()">×</button>
                    </div>

                    <div class="confirm-body">
                        This action will permanently remove this patient ticket from the queue. Continue?
                    </div>

                    <div class="modal-actions">
                        <button type="button" class="danger-btn" onclick="confirmDeleteTicket()">Delete</button>
                        <button type="button" class="secondary-btn" onclick="closeDeleteModal()">Cancel</button>
                    </div>
                </div>
            </div>

            <script>
                let currentFilter = 'all';
                let activeTicketNumber = null;
                let ticketToDelete = null;

                document.querySelectorAll('.filter-btn').forEach(btn => {
                    btn.addEventListener('click', () => {
                        currentFilter = btn.dataset.filter;
                        document.querySelectorAll('.filter-btn').forEach(b => b.classList.toggle('active', b === btn));
                        loadQueue();
                    });
                });

                async function loadQueue() {
                    const response = await fetch('/api/tickets');
                    const tickets = await response.json();
                    const queueList = document.getElementById('queueList');
                    const totalCount = document.getElementById('totalCount');
                    const waitingCount = document.getElementById('waitingCount');
                    const urgentCount = document.getElementById('urgentCount');
                    const nextTicket = document.getElementById('nextTicket');

                    totalCount.textContent = tickets.length;
                    waitingCount.textContent = tickets.filter(t => t.status === 'WAITING').length;
                    urgentCount.textContent = tickets.filter(t => t.urgent).length;
                    nextTicket.textContent = tickets.length ? tickets[0].ticketNumber : '-';

                    let filtered = tickets;
                    if (currentFilter === 'WAITING') {
                        filtered = tickets.filter(t => t.status === 'WAITING');
                    } else if (currentFilter === 'urgent') {
                        filtered = tickets.filter(t => t.urgent);
                    }

                    queueList.innerHTML = '';

                    if (!filtered.length) {
                        queueList.innerHTML = '<li class="empty-state">No patients match this view.</li>';
                        return;
                    }

                    filtered.forEach(ticket => {
                        const item = document.createElement('li');
                        item.className = `patient-item ${ticket.urgent ? 'urgent' : ''}`;
                        const statusClass = ticket.status === 'WAITING' ? 'waiting' : ticket.status === 'BEING_ATTENDED_TO' ? 'attending' : ticket.status === 'COMPLETED' ? 'completed' : 'cancelled';
                        const statusText = ticket.status.replace(/_/g, ' ');

                        item.innerHTML = `
                            <div class="patient-head">
                                <div class="patient-name">${ticket.patient.name}</div>
                                <div class="badge-row">
                                    <span class="badge ${ticket.urgent ? 'urgent' : 'standard'}">${ticket.urgent ? 'Urgent' : 'Standard'}</span>
                                    <span class="badge ${statusClass}">${statusText}</span>
                                </div>
                            </div>

                            <div class="patient-meta">
                                <span>Ticket #${ticket.ticketNumber}</span>
                                <span>ID: ${ticket.patient.patientId}</span>
                            </div>

                            <div class="patient-actions">
                                <select id="status-${ticket.ticketNumber}">
                                    <option value="WAITING" ${ticket.status === 'WAITING' ? 'selected' : ''}>Waiting</option>
                                    <option value="BEING_ATTENDED_TO" ${ticket.status === 'BEING_ATTENDED_TO' ? 'selected' : ''}>Being Attended To</option>
                                    <option value="COMPLETED" ${ticket.status === 'COMPLETED' ? 'selected' : ''}>Completed</option>
                                    <option value="CANCELLED" ${ticket.status === 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                                </select>
                                <button type="button" onclick="updateStatus(${ticket.ticketNumber})">Update</button>
                                <button type="button" class="secondary-btn" onclick="toggleUrgent(${ticket.ticketNumber}, ${!ticket.urgent})">${ticket.urgent ? 'Standard' : 'Urgent'}</button>
                                <button type="button" class="secondary-btn" onclick="openEditModal(${ticket.ticketNumber}, '${ticket.patient.patientId.replace(/'/g, "\\'")}', '${ticket.patient.name.replace(/'/g, "\\'")}', ${ticket.urgent})">Edit</button>
                                <button type="button" style="background: #b91c1c; box-shadow: none;" onclick="deleteTicket(${ticket.ticketNumber})">Delete</button>
                            </div>
                        `;
                        queueList.appendChild(item);
                    });
                }

                async function registerPatient() {
                    const patientId = document.getElementById('patientId').value.trim();
                    const name = document.getElementById('patientName').value.trim();
                    const urgent = document.getElementById('urgent').checked;

                    if (!patientId || !name) {
                        openInlineMessage('Please enter both a patient ID and patient name.');
                        return;
                    }

                    await fetch('/api/tickets', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ patientId, name, urgent })
                    });

                    document.getElementById('patientId').value = '';
                    document.getElementById('patientName').value = '';
                    document.getElementById('urgent').checked = false;
                    loadQueue();
                }

                async function updateStatus(ticketNumber) {
                    const statusValue = document.getElementById(`status-${ticketNumber}`).value;
                    await fetch(`/api/tickets/${ticketNumber}/status`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'text/plain' },
                        body: statusValue
                    });
                    loadQueue();
                }

                async function toggleUrgent(ticketNumber, urgent) {
                    await fetch(`/api/tickets/${ticketNumber}/urgent`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ urgent })
                    });
                    loadQueue();
                }

                function openEditModal(ticketNumber, patientId, name, urgent) {
                    activeTicketNumber = ticketNumber;
                    document.getElementById('editPatientId').value = patientId || '';
                    document.getElementById('editPatientName').value = name || '';
                    document.getElementById('editUrgent').checked = Boolean(urgent);
                    document.getElementById('editModal').classList.add('visible');
                    document.getElementById('editModal').setAttribute('aria-hidden', 'false');
                }

                function closeEditModal() {
                    activeTicketNumber = null;
                    document.getElementById('editModal').classList.remove('visible');
                    document.getElementById('editModal').setAttribute('aria-hidden', 'true');
                }

                async function submitEditTicket() {
                    if (!activeTicketNumber) return;

                    const patientId = document.getElementById('editPatientId').value.trim();
                    const name = document.getElementById('editPatientName').value.trim();
                    const urgent = document.getElementById('editUrgent').checked;

                    if (!patientId || !name) {
                        openInlineMessage('Please enter both a patient ID and patient name.');
                        return;
                    }

                    await fetch(`/api/tickets/${activeTicketNumber}/edit`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ patientId, name, urgent })
                    });

                    closeEditModal();
                    loadQueue();
                }

                function openInlineMessage(message) {
                    const modal = document.getElementById('deleteModal');
                    const title = document.getElementById('deleteTicketTitle');
                    const body = modal.querySelector('.confirm-body');
                    const deleteButton = modal.querySelector('.danger-btn');
                    const cancelButton = modal.querySelector('.secondary-btn');

                    title.textContent = 'Notice';
                    body.textContent = message;
                    deleteButton.style.display = 'none';
                    cancelButton.textContent = 'Close';
                    ticketToDelete = null;
                    modal.classList.add('visible');
                    modal.setAttribute('aria-hidden', 'false');
                }

                function closeDeleteModal() {
                    const modal = document.getElementById('deleteModal');
                    const title = document.getElementById('deleteTicketTitle');
                    const body = modal.querySelector('.confirm-body');
                    const deleteButton = modal.querySelector('.danger-btn');
                    const cancelButton = modal.querySelector('.secondary-btn');

                    title.textContent = 'Delete Ticket';
                    body.textContent = 'This action will permanently remove this patient ticket from the queue. Continue?';
                    deleteButton.style.display = 'inline-block';
                    cancelButton.textContent = 'Cancel';
                    ticketToDelete = null;
                    modal.classList.remove('visible');
                    modal.setAttribute('aria-hidden', 'true');
                }

                function deleteTicket(ticketNumber) {
                    ticketToDelete = ticketNumber;
                    document.getElementById('deleteModal').classList.add('visible');
                    document.getElementById('deleteModal').setAttribute('aria-hidden', 'false');
                }

                function confirmDeleteTicket() {
                    if (!ticketToDelete) return;

                    fetch(`/api/tickets/${ticketToDelete}`, {
                        method: 'DELETE'
                    }).then(() => {
                        closeDeleteModal();
                        loadQueue();
                    });
                }

                loadQueue();
                setInterval(loadQueue, 5000);
            </script>
            </body>
            </html>
            """;
    }

    private Map<String, String> errorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}
