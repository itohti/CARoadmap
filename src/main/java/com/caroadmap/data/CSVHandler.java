package com.caroadmap.data;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static com.caroadmap.data.CsvUtils.*;

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
                writer.write(String.join(",", getHeaderNames()));
                writer.newLine();
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
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine(); // skip header

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = parseCsvLine(line);
                if (values.length < CSVColumns.values().length) continue;

                if (values[CSVColumns.TASK_NAME.ordinal()].equals(taskName)) {
                    return parseTask(values);
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

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tmp))) {

            String headerLine = reader.readLine();
            writer.write(headerLine);
            writer.newLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = parseCsvLine(line);
                if (values.length < CSVColumns.values().length) continue;

                Task task = parseTask(values);

                if (task.getTaskName().equals(taskName)) {
                    task.setDone(true);
                    isUpdated = true;
                }

                writeTask(writer, task);
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
            writeTask(writer, task);
        } catch (IOException e) {
            log.error("Could not write to CSV file: {}", csvPath, e);
        }
    }

    private void writeTask(BufferedWriter writer, Task task) throws IOException {
        String[] values = new String[]{
                escapeCsv(task.getBoss()),
                escapeCsv(task.getTaskName()),
                escapeCsv(task.getTaskDescription()),
                escapeCsv(String.valueOf(task.getType())),
                escapeCsv(String.valueOf(task.getTier())),
                escapeCsv(String.valueOf(task.isDone())),
                escapeCsv(String.valueOf(task.getScore()))
        };
        writer.write(String.join(",", values));
        writer.newLine();
    }

    private Task parseTask(String[] values) {
        Task task = new Task(
                values[CSVColumns.BOSS_NAME.ordinal()],
                values[CSVColumns.TASK_NAME.ordinal()],
                values[CSVColumns.TASK_DESCRIPTION.ordinal()],
                values[CSVColumns.TYPE.ordinal()],
                values[CSVColumns.TIER.ordinal()],
                values[CSVColumns.DONE.ordinal()]
        );

        if (CSVColumns.SCORE.ordinal() < values.length) {
            try {
                task.setScore(Double.parseDouble(values[CSVColumns.SCORE.ordinal()]));
            } catch (NumberFormatException e) {
                log.warn("Invalid score for task '{}': '{}'", task.getTaskName(), values[CSVColumns.SCORE.ordinal()]);
            }
        }
        return task;
    }

    public String getCsvPath() {
        return csvPath;
    }
}
