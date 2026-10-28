package com.hologram.clinic.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hologram.clinic.model.QueueTicket;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileQueueStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;

    public FileQueueStorage(String filePath) {
        this.filePath = Paths.get(filePath);
        createParentDirectoryIfNeeded();
    }

    public void save(List<QueueTicket> tickets) {
        try {
            String json = GSON.toJson(tickets);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save tickets to file.", e);
        }
    }

    public List<QueueTicket> load() {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try {
            String content = Files.readString(filePath);
            if (content == null || content.isBlank()) {
                return new ArrayList<>();
            }

            Type listType = new TypeToken<ArrayList<QueueTicket>>() {}.getType();
            return GSON.fromJson(content, listType);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read tickets from file.", e);
        }
    }

    private void createParentDirectoryIfNeeded() {
        Path parent = filePath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create storage directory.", e);
            }
        }
    }
}
