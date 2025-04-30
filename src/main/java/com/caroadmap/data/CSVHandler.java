package com.caroadmap.data;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Slf4j
public class CSVHandler {
    private final String csvPath;

    public CSVHandler(String username, String fileName) {
        File pluginDir = new File(RuneLite.RUNELITE_DIR, "caroadmap");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }

        String cleaned = username.replace(" ", "_");
        File csvFile = new File(pluginDir, String.format("%s_%s.csv", fileName, cleaned));

        if (!csvFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                writer.write(String.join(",", getHeaderNames()) + "\n");
            } catch (IOException e) {
                log.error("Could not create CSV file: ", e);
            }
        }

        this.csvPath = csvFile.getPath();
    }

    private String[] getHeaderNames() {
        CSVColumns[] columns = CSVColumns.values();
        String[] headers = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            headers[i] = columns[i].name();
        }
        return headers;
    }

    public Task getTask(String taskName) {
        try (Reader reader = new FileReader(csvPath, StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader(CSVColumns.class)
                    .setSkipHeaderRecord(true)
                    .get();
            Iterable<CSVRecord> records = format.parse(reader);

            for (CSVRecord record : records) {
                String taskFromFile = record.get(CSVColumns.TASK_NAME);
                if (taskFromFile.equals(taskName)) {
                    Task task = new Task(
                            record.get(CSVColumns.BOSS_NAME),
                            record.get(CSVColumns.TASK_NAME),
                            record.get(CSVColumns.TASK_DESCRIPTION),
                            record.get(CSVColumns.TYPE),
                            record.get(CSVColumns.TIER),
                            record.get(CSVColumns.DONE)
                    );
                    String scoreStr = record.get(CSVColumns.SCORE);
                    if (scoreStr != null && !scoreStr.isBlank()) {
                        task.setScore(Double.parseDouble(scoreStr));
                    }
                    return task;
                }
            }
        } catch (IOException e) {
            log.error("Could not read CSV file: ", e);
        }
        return null;
    }

    public void updateTask(String taskName) {
        boolean isUpdated = false;
        File tmp = new File("tmp.csv");
        File csvFile = new File(csvPath);

        try (Reader reader = new FileReader(csvPath, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(new FileWriter(tmp))) {

            CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader(CSVColumns.class)
                    .setSkipHeaderRecord(true)
                    .get();
            Iterable<CSVRecord> records = format.parse(reader);
            Iterator<CSVRecord> iterator = records.iterator();

            writer.write(String.join(",", getHeaderNames()) + "\n");

            while (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                Task task = new Task(
                        record.get(CSVColumns.BOSS_NAME),
                        record.get(CSVColumns.TASK_NAME),
                        record.get(CSVColumns.TASK_DESCRIPTION),
                        record.get(CSVColumns.TYPE),
                        record.get(CSVColumns.TIER),
                        record.get(CSVColumns.DONE)
                );

                String scoreStr = record.get(CSVColumns.SCORE);
                double score = 0.0;
                if (scoreStr != null && !scoreStr.isBlank()) {
                    try {
                        score = Double.parseDouble(scoreStr);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid score for task '{}': '{}'", task.getTaskName(), scoreStr);
                    }
                }
                task.setScore(score);

                if (task.getTaskName().equals(taskName)) {
                    task.setDone(true);
                    isUpdated = true;
                }

                writer.write(String.format("%s,%f", task.toString(), task.getScore()));
                writer.newLine();
            }
        } catch (IOException e) {
            log.error("Could not read or write to CSV file: {}", csvPath, e);
        }

        if (isUpdated) {
            try (FileInputStream in = new FileInputStream(tmp);
                 FileOutputStream out = new FileOutputStream(csvFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
            } catch (IOException ce) {
                log.error("Manual file copy also failed: ", ce);
            }

            if (!tmp.delete()) {
                log.warn("Could not delete temp file: {}", tmp.getName());
            }
        }
    }

    public void createTask(Task task) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath, true))) {
            writer.write(String.format("%s,%f", task.toString(), task.getScore()));
            writer.newLine();
        } catch (IOException e) {
            log.error("Could not write to CSV file: {}", csvPath, e);
        }
    }

    public String getCsvPath() {
        return csvPath;
    }
}
