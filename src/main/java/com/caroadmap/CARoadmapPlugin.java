package com.caroadmap;

import com.caroadmap.ui.CARoadmapPanel;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.checkerframework.checker.units.qual.C;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.Map;
import java.util.concurrent.Executors;

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

	@Inject
	private ClientToolbar clientToolbar;

//	@Inject
//	private CARoadmapPanel caRoadmapPanel;
	private NavigationButton navButton;

	private CSVHandler csvHandler;
	private FirebaseDatabase firestore;

	private boolean getData = false;

	private final Map<String, Integer> bossList = new HashMap<>();
	private final Map<String, String> bossToRaid = new HashMap<>();

	private ExecutorService firestoreExecutor;
	private ExecutorService csvHandlerExecutor;

	@Override
	protected void startUp() throws Exception
	{
		CARoadmapPanel caRoadmapPanel = new CARoadmapPanel();
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/combat_achievements_icon.png");
		if (icon == null) {
			System.err.println("Could not load icon");
		}
		try {
			navButton = NavigationButton.builder()
					.tooltip("CA Roadmap")
					.icon(icon)
					.panel(caRoadmapPanel)
					.build();

			clientToolbar.addNavigation(navButton);
		}
		catch (Exception e) {
			System.err.println("There was an error in setting up the nav button: " + e.getMessage());
		}

		firestoreExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			t.setName("FirestoreThread");
			return t;
		});

		csvHandlerExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			t.setName("csvThread");
			return t;
		});

		firestoreExecutor.submit(() -> {
			this.firestore = new FirebaseDatabase(client.getLauncherDisplayName());
		});

		clientThread.invoke(() -> {
			fetchAndStorePlayerData(client.getLauncherDisplayName());
			populateBossToRaid();

			csvHandlerExecutor.submit(() -> {
				this.csvHandler = new CSVHandler();
			});

			return true;
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		firestore.cleanUp();
		firestoreExecutor.shutdown();
		csvHandlerExecutor.shutdown();
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

	public void populateData() {
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
				for (String bossName : bossList.keySet()) {
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
					} else if (lowerDescription.contains(bossName.toLowerCase())) {
						boss = bossName;
					}
				}
				// we can use the cs2 vm to invoke script 4834 to do the lookup for us
				client.runScript(4834, id);
				boolean done = client.getIntStack()[0] != 0;

				Task taskObject = new Task(boss, name, description, type, tier, done);
				// add to Firestore
				firestoreExecutor.submit(() -> {
					boolean result = firestore.addTaskToBatch(taskObject);
					if (!result) {
						System.err.println("Could not add task to batch");
					}
				});

				// finding the task in the csv file by its name
				csvHandlerExecutor.submit(() -> {
					Task readTask = csvHandler.getTask(name);
					if (readTask != null) {
						// if the task in the csv does not equal the task we fetched from the game update it.
						// this would typically happen if a player has completed a task.
						if (!taskObject.equals(readTask)) {
							csvHandler.updateTask(taskObject);
							log.info("Task in csv and game task did not match updating...");
						}
					} else {
						// otherwise if we did not find the task in the csv file create a entry in the csv file.
						csvHandler.createTask(taskObject);
						log.info("Could not find task in csv, creating an entry for it...");
					}
				});
			}
		}

		firestoreExecutor.submit(() -> {
			boolean result = firestore.commitBatch();
			if (!result) {
				System.err.println("Could not commit batch to firestore.");
			}
		});
	}

	public void fetchAndStorePlayerData(String displayName) {
		if (displayName == null) {
			return;
		}
		try {
			HiscoreResult result = hiscoreClient.lookup(displayName);
			boolean isBossList = false;
			for (HiscoreSkill skill: result.getSkills().keySet()) {
				if (isBossList) {
					bossList.put(skill.getName(), result.getSkill(skill).getLevel());
					firestoreExecutor.submit(() -> {
						boolean wrote = firestore.addSkillToBatch(skill.getName(), result.getSkill(skill).getLevel(), true);
						if (!wrote) {
							log.error(String.format("Could not write %s in firestore", skill.getName()));
						}
					});
				}

				else if (skill.getName().equals("Collections Logged")) {
					isBossList = true;
				}
			}

			// adding skills in firestore
			firestoreExecutor.submit(() -> {
				firestore.addSkillToBatch("Attack", result.getSkills().get(HiscoreSkill.ATTACK).getLevel(), false);
				firestore.addSkillToBatch("Defence", result.getSkills().get(HiscoreSkill.DEFENCE).getLevel(), false);
				firestore.addSkillToBatch("Strength", result.getSkills().get(HiscoreSkill.STRENGTH).getLevel(), false);
				firestore.addSkillToBatch("Hitpoints", result.getSkills().get(HiscoreSkill.HITPOINTS).getLevel(), false);
				firestore.addSkillToBatch("Ranged", result.getSkills().get(HiscoreSkill.RANGED).getLevel(), false);
				firestore.addSkillToBatch("Prayer", result.getSkills().get(HiscoreSkill.PRAYER).getLevel(), false);
			});
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
