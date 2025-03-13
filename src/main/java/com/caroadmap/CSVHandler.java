package com.caroadmap;

import net.runelite.client.RuneLite;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class CSVHandler {
    private String csvPath;
    /**
     * Creates a CSV file if it doesn't already exist.
     */
    public CSVHandler() {
        File pluginDir = new File(RuneLite.RUNELITE_DIR, "caroadmap");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }

        File csvFile = new File(pluginDir, "combat_achievements_checklist.csv");
        if (!csvFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                writer.write("Boss,Task Name,Task Description,Type,Tier,Done\n");
            } catch (IOException e) {
                System.err.println("Could not create reader and writer for csv file.");
            }
        }

        this.csvPath = csvFile.getPath();
    }

    /**
     * Will find the task in the csv file if it exists
     * @param taskName is a String that describes the task name.
     * @return Task
     */
    public Task getTask(String taskName) {
        try (Reader reader = new FileReader(csvPath, StandardCharsets.UTF_8)) {
            // Updated code to avoid using deprecated withHeader()
            CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader(CSVColumns.class)
                    .get();
            Iterable<CSVRecord> records = format.parse(reader);

            for (CSVRecord record : records) {
                String taskFromFile = record.get(CSVColumns.TASK_NAME);
                if (taskFromFile.equals(taskName)) {
                    return new Task(
                            record.get(CSVColumns.BOSS_NAME),
                            record.get(CSVColumns.TASK_NAME),
                            record.get(CSVColumns.TASK_DESCRIPTION),
                            record.get(CSVColumns.TYPE),
                            record.get(CSVColumns.TIER),
                            record.get(CSVColumns.DONE)
                    );
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read csv file.");
        }
        return null;
    }

    /**
     * This will update a Task based on the task passed in.
     */
    public void updateTask(Task task) {
        boolean isUpdated = false;
        File tmp = new File("tmp.csv");
        File csvFile = new File(csvPath);
        try (Reader reader = new FileReader(csvPath, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(new FileWriter(tmp))) {

            // Updated code to avoid using deprecated withHeader()
            CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader(CSVColumns.class)
                    .setSkipHeaderRecord(true)
                    .get();
            Iterable<CSVRecord> records = format.parse(reader);
            Iterator<CSVRecord> iterator = records.iterator();

            // Write headers to the temporary file
            writer.write("Boss,Task Name,Task Description,Type,Tier,Done\n");

            while (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                Task readTask = new Task(
                        record.get(CSVColumns.BOSS_NAME),
                        record.get(CSVColumns.TASK_NAME),
                        record.get(CSVColumns.TASK_DESCRIPTION),
                        record.get(CSVColumns.TYPE),
                        record.get(CSVColumns.TIER),
                        record.get(CSVColumns.DONE)
                );
                if (readTask.getTaskName().equals(task.getTaskName())) {
                    // Update the task.
                    writer.write(task.toString());
                    writer.newLine();
                    isUpdated = true;
                } else {
                    writer.write(readTask.toString());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read or write to csv file: " + csvPath);
        }

        if (isUpdated) {
            try (FileInputStream in = new FileInputStream(tmp);
                 FileOutputStream out = new FileOutputStream(csvFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
                if (!tmp.delete()) {
                    // revisit this.
                    System.out.println("Could not delete tmp file...");
                }
            } catch (IOException ce) {
                System.err.println("Manual file copy also failed: " + ce.getMessage());
            }
        }
    }

    /**
     * This will create a Task if it is not in the csv file.
     */
    public void createTask(Task task) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath, true))) {
            writer.write(task.toString());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Could not write to csv file: " + csvPath);
        }
    }
}