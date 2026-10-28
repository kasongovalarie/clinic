package com.hologram.clinic;

import com.hologram.clinic.service.QueueManager;
import com.hologram.clinic.storage.FileQueueStorage;

public class Main {
    public static void main(String[] args) {
        FileQueueStorage storage = new FileQueueStorage("queue-data.json");
        QueueManager queueManager = new QueueManager(storage);

        ClinicQueueApp app = new ClinicQueueApp(queueManager);
        app.start(7000);
    }
}
