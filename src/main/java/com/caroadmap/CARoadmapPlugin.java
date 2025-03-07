package com.caroadmap;

import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "CARoadmap"
)
public class CARoadmapPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CARoadmapConfig config;

	@Inject
	private HiscoreClient hiscoreClient;

	private boolean getData = false;
	private final String excelPath = "./src/main/resources/Combat_Achivements_Checklist.xlsx";

	private final Map<String, Integer> bossList = new HashMap<>();
	private final Map<String, String> bossToRaid = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invoke(() -> {
			populateBossList(client.getLauncherDisplayName());
			populateBossToRaid();
			// check if excel sheet exists.
			File file = new File(excelPath);
			if (!file.exists()) {
				// if it doesn't exist create an empty excel sheet with only the header row. It will be populated once
				// we start reading values from varbits.
				try (Workbook workbook = new XSSFWorkbook()) {
					Sheet sheet = workbook.createSheet("Combat Achievements");

					Row headerRow = sheet.createRow(0);
					headerRow.createCell(ExcelColumns.BOSS_NAME.getIndex()).setCellValue("Boss");
					headerRow.createCell(ExcelColumns.TASK_NAME.getIndex()).setCellValue("Task Name");
					headerRow.createCell(ExcelColumns.TASK_DESCRIPTION.getIndex()).setCellValue("Task Description");
					headerRow.createCell(ExcelColumns.TYPE.getIndex()).setCellValue("Type");
					headerRow.createCell(ExcelColumns.TIER.getIndex()).setCellValue("Tier");
					headerRow.createCell(ExcelColumns.DONE.getIndex()).setCellValue("Done");

					try (FileOutputStream fos = new FileOutputStream(new File(excelPath))) {
						workbook.write(fos);
						log.info("Created excel sheet at " + excelPath);
					}
				}
				catch (IOException e) {
					log.error("There was an IOException at start up.");
				}
			}
			return true;
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			getData = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// this function gets called every GAME tick.
		if (getData) {
			populateData();
			getData = false;
		}
	}

	public static Row findRowByValue(Sheet sheet, String value) {
		for (Row row : sheet) {
			Cell cell = row.getCell(ExcelColumns.TASK_NAME.getIndex()); // Ensure correct column
			if (cell != null && cell.getCellType() == CellType.STRING &&
					cell.getStringCellValue().trim().equalsIgnoreCase(value.trim())) {
				return row; // Return the row if the value matches
			}
		}
		return null;
	}

	public void populateData() {
		try (FileInputStream fis = new FileInputStream(new File(excelPath)); Workbook workbook = new XSSFWorkbook(fis)) {
			Sheet sheet = workbook.getSheetAt(0);
			// from [proc,ca_tasks_total]
			// there is an enum per ca tier
			for (int enumId : new int[]{3981, 3982, 3983, 3984, 3985, 3986}) {
				var listOfCombatTasks = client.getEnum(enumId);
				// so we can iterate the enum to find a bunch of structs
				for (int structId : listOfCombatTasks.getIntVals()) {
					var task = client.getStructComposition(structId);
					// and with the struct we can get info about the ca
					// like its name
					String name = task.getStringValue(1308);
					// or its id, which we can use to get if its completed or not
					int id = task.getIntValue(1306);
					// 1308 is the description of the task.
					String description = task.getStringValue(1309);
					// 1310 is tier
					int tier = task.getIntValue(1310);
					// 1311 is the mapping of type. Refer to TaskType.java to see the mapping.
					TaskType type = TaskType.fromValue(task.getIntValue(1311));
					// inferring what boss it is based on the description.
					String boss = "";
					for (String bossName: bossList.keySet()) {
						String lowerDescription = description.toLowerCase();
						// weird case to barrows.
						if (bossName.equals("Barrows Chests")) {
							// change it to Barrows only.
							if (description.contains("Barrows")) {
								boss = "Barrows";
							}
						}
						// Raids are weird too
						for (String miniBoss : bossToRaid.keySet()) {
							if (description.contains(miniBoss)) {
								boss = bossToRaid.get(miniBoss);
							}
						}
						// Perilous Moons
						if (description.contains("Moons") || description.contains("Moon") || description.contains("moon")) {
							boss = "Perilous Moons";
						}
						else if (lowerDescription.contains(bossName.toLowerCase())) {
							boss = bossName;
						}
					}

					// we can use the cs2 vm to invoke script 4834 to do the lookup for us
					client.runScript(4834, id);
					boolean done = client.getIntStack()[0] != 0;

					// now that we have all of our info lets populate the excel sheet.
					Row foundRow = findRowByValue(sheet, name);
					if (foundRow != null) {
						// We found the task.
						Cell excelTask = foundRow.getCell(ExcelColumns.DONE.getIndex());

						// Create a cell style
						CellStyle style = workbook.createCellStyle();
						style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
						style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

						if (done) {
							excelTask.setCellStyle(style);
						}

						excelTask.setCellValue(done); // Set the boolean value
					}
					else {
						// we did not find the task. so create an entry and add it to the excel sheet.
						log.info("Could not find task adding it...");
						Row newTask = sheet.createRow(sheet.getLastRowNum() + 1);
						if (!boss.isEmpty()) {
							newTask.createCell(ExcelColumns.BOSS_NAME.getIndex()).setCellValue(boss);
						}
						newTask.createCell(ExcelColumns.TASK_NAME.getIndex()).setCellValue(name);
						newTask.createCell(ExcelColumns.TASK_DESCRIPTION.getIndex()).setCellValue(description);
						newTask.createCell(ExcelColumns.TYPE.getIndex()).setCellValue(type.name());
						newTask.createCell(ExcelColumns.TIER.getIndex()).setCellValue(tier);
						newTask.createCell(ExcelColumns.DONE.getIndex()).setCellValue(done);

						CellStyle style = workbook.createCellStyle();
						style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
						style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

						if (done) {
							newTask.getCell(ExcelColumns.DONE.getIndex()).setCellStyle(style);
						}
					}
				}
			}

			for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
				// skip auto sizing the description.
				if (i == 2) {
					continue;
				}
				sheet.autoSizeColumn(i);
			}

			try (FileOutputStream fos = new FileOutputStream(excelPath)) {
				workbook.write(fos);  // This writes the changes to the file
				log.info("Excel file updated successfully!");
			}
		}
		catch (IOException exception) {
			log.error("Could not open excel spread sheet.");
		}
	}

	public void populateBossList(String displayName) {
		if (displayName == null) {
			return;
		}
		try {
			HiscoreResult result = hiscoreClient.lookup(displayName);
			boolean isBossList = false;
			for (HiscoreSkill skill: result.getSkills().keySet()) {
				// pretty dumb solution
				if (isBossList) {
					bossList.put(skill.getName(), result.getSkill(skill).getLevel());
				}
				// assuming Abyssal Sire will be the start of the boss list. Come back to this.
				else if (skill.getName().equals("Collections Logged")) {
					isBossList = true;
				}
			}

			System.out.println(bossList);
		}
		catch (IOException e) {
			log.error("Could not fetch hiscores for user: " + displayName);
		}
	}

	public void populateBossToRaid() {
		// Theatre of Blood
		bossToRaid.put("Maiden of Sugadinti", "Theatre of Blood");
		bossToRaid.put("Pestilent Bloat", "Theatre of Blood");
		bossToRaid.put("Nylocas Vasilias", "Theatre of Blood");
		bossToRaid.put("Sotetseg", "Theatre of Blood");
		bossToRaid.put("Xarpus", "Theatre of Blood");
		bossToRaid.put("Verzik", "Theatre of Blood");

		// Chambers of Xeric
		bossToRaid.put("Tightrope", "Chambers of Xeric");
		bossToRaid.put("Great Olm", "Chambers of Xeric");
		bossToRaid.put("Vasa Nistirio", "Chambers of Xeric");
		bossToRaid.put("Tekton", "Chambers of Xeric");
		bossToRaid.put("Crystal Crabs", "Chambers of Xeric");
		bossToRaid.put("Vanguards", "Chambers of Xeric");
		bossToRaid.put("Vespula", "Chambers of Xeric");
		bossToRaid.put("Muttadile", "Chambers of Xeric");
		bossToRaid.put("Stone Guardian", "Chambers of Xeric");
		bossToRaid.put("Shaman", "Chambers of Xeric");
		bossToRaid.put("Ice Demon", "Chambers of Xeric");

		// Tombs of Amascut
		bossToRaid.put("Ba-Ba", "Tombs of Amascut");
		bossToRaid.put("Zebak", "Tombs of Amascut");
		bossToRaid.put("Kephri", "Tombs of Amascut");
		bossToRaid.put("Akkha", "Tombs of Amascut");
		bossToRaid.put("Wardens", "Tombs of Amascut");
		bossToRaid.put("Het", "Tombs of Amascut");
		bossToRaid.put("Apmeken", "Tombs of Amascut");
		bossToRaid.put("Crondis", "Tombs of Amascut");
	}

	@Provides
	CARoadmapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CARoadmapConfig.class);
	}
}
