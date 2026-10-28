# Clinic Queue Management System

This project is a Java + Javalin backend for managing patient tickets in a clinic queue.

## Problem
Small clinics still manage queues by hand, which causes confusion, delays, and poor communication between patients and staff.

## Features
- Register patients and issue queue tickets
- Track ticket status: Waiting, Being Attended To, Completed, Cancelled
- Prioritize urgent tickets
- Save queue data to a file so records persist across restarts
- Expose REST endpoints for a simple front-end app

## Main classes
- Patient
- QueueTicket
- StandardTicket
- UrgentTicket
- QueueManager
- FileQueueStorage
- Main

## Run the project
1. Install Java 17 and Maven.
2. Open the project folder.
3. Run:
   ```bash
   mvn clean compile exec:java
   ```
   Or use:
   ```bash
   mvn clean package
   java -jar target/clinic-queue-system-1.0.0.jar
   ```

The app starts on port 7000.
