package com.caroadmap;

import com.caroadmap.api.CARoadmapServer;
import com.caroadmap.api.PlayerDataBatcher;
import com.caroadmap.api.WiseOldMan;
import com.caroadmap.data.*;
import com.caroadmap.ui.CARoadmapPanel;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.*;

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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	@Inject
	private ConfigManager configManager;

	@Inject
	private CARoadmapServer server;

	@Inject
	private SpriteManager spriteManager;

	private CARoadmapPanel caRoadmapPanel;
	private CSVHandler recommendationsCSV;
	private NavigationButton navButton;

    private PlayerDataBatcher firestore;
	private WiseOldMan wiseOldMan;

	private boolean getData = false;

	private RecommendTasks recommendTasks;
	private String username;
	private boolean hasFetched = false;
	private String apiKey;

	private ExecutorService firestoreExecutor;
	private ExecutorService csvHandlerExecutor;

	@Override
	protected void startUp() throws Exception
	{
		this.apiKey = config.apiKey();
		this.caRoadmapPanel = new CARoadmapPanel(spriteManager, configManager);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/combat_achievements_icon.png");
		if (icon == null) {
			log.error("Could not load icon");
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
			log.error("There was an error in setting up the nav button: " + e.getMessage());
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
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		firestoreExecutor.shutdown();
		csvHandlerExecutor.shutdown();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN && !hasFetched)
		{
			hasFetched = true;
			getData = true;
		}
		else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
			log.info("Fetching because user is relogging in.");
			hasFetched = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// this function gets called every GAME tick.
		if (getData) {
			fetchData();
			getData = false;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		if (event.getType() == ChatMessageType.GAMEMESSAGE) {
			String msg = event.getMessage();
			if (msg.contains("combat task") && msg.contains("completed")) {
				csvHandlerExecutor.submit(() -> {
					try {
						Pattern pattern = Pattern.compile("combat task: <col=\\w+>(.*?)</col>");
						Matcher matcher = pattern.matcher(msg);

						if (matcher.find()) {
							String taskName = matcher.group(1).trim();

							recommendationsCSV.updateTask(taskName);

							boolean removed = caRoadmapPanel.taskCompleted(taskName);
							if (removed) {
								log.info("Successfully marked task as complete");
							}
							caRoadmapPanel.refresh();
						}
					}
					catch (Exception e) {
						log.error("Something went wrong with getting task name", e);
					}
				});
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event) {
		// we can see if the user completed a task with the widget pop up now.
		if (event.getGroupId() == 660) {
			Widget popupTextWidget = client.getWidget(660, 8);
			if (popupTextWidget != null && popupTextWidget.getText() != null) {
				String rawText = popupTextWidget.getText();
				log.info("Combat task popup text: {}", rawText);

				String cleanText = rawText.replaceAll("<[^>]+>", "").trim();
				log.info("Cleaned task text: {}", cleanText);
			}
		}
	}

	private void fetchData() {
		this.username = getUsername();

		// Initialize classes that are dependent on username
		this.recommendationsCSV = new CSVHandler(username, "recommendations_list");
		this.recommendTasks = new RecommendTasks(server, recommendationsCSV, configManager);
		caRoadmapPanel.setRecommendTasks(recommendTasks);
		this.wiseOldMan = new WiseOldMan(username);

		if (!apiKey.isEmpty()) {
			server.setApiKey(apiKey);
		}
		else {
			server.register(username);
			configManager.setConfiguration("CARoadmap", "apiKey", server.getApiKey());
		}

		firestoreExecutor.submit(() -> {
			this.firestore = new PlayerDataBatcher(username, server);
			fetchAndStorePlayerSkills(username);

			Boss[] wiseOldManData = wiseOldMan.fetchBossInfo();
			for (Boss boss : wiseOldManData) {
				// before we send it to firestore get pb.
				Double pb = configManager.getRSProfileConfiguration("personalbest", boss.getBoss().toLowerCase(), double.class);
				boss.setKillTime(Objects.requireNonNullElse(pb, -1.0));
				if (!firestore.addBossToBatch(boss)) {
					log.error("Could not add boss [{}] to batch", boss.getBoss());
				}
			}
		});

		fetchAndStorePlayerTasks();
		firestoreExecutor.submit(() -> {
			boolean result = firestore.sendData();
			if (!result) {
				log.error("Did not upload player data to database");
			}
			else {
				File pluginDir = new File(RuneLite.RUNELITE_DIR, "caroadmap");
				File cacheFile = new File(pluginDir, String.format("player_cache_%s.json", username.replace(" ", "_")));
				if (!cacheFile.exists()) {
					log.info("Caching player data from firestore.");
					server.fetchAndCachePlayerData(username);
				}
			}
		});

		csvHandlerExecutor.submit(() -> {
			recommendTasks.getRecommendations(username, 1014);

			SwingUtilities.invokeLater(() -> {
				if (caRoadmapPanel != null) {
					caRoadmapPanel.setUsername(username);
					caRoadmapPanel.refresh();
				}
			});
		});
	}

	private void fetchAndStorePlayerTasks() {
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

				// fetching the boss from game data for the combat task is not great, plus I don't even use it when I add the task in the db.
				String boss = "";
				// we can use the cs2 vm to invoke script 4834 to do the lookup for us
				client.runScript(4834, id);
				boolean done = client.getIntStack()[0] != 0;

				Task taskObject = new Task(boss, name, description, type, tier, done);
				// add to Firestore
				firestoreExecutor.submit(() -> {
					boolean result = firestore.addTaskToBatch(taskObject);
					if (!result) {
						log.error("Could not add task: [{}] to batch", taskObject.getTaskName());
					}
				});
			}
		}
	}

	private void fetchAndStorePlayerSkills(String displayName) {
		if (displayName == null) {
			return;
		}
		try {
			HiscoreResult result = hiscoreClient.lookup(displayName);
			// adding skills in firestore
			firestore.addSkillToBatch("Attack", result.getSkills().get(HiscoreSkill.ATTACK).getLevel());
			firestore.addSkillToBatch("Defence", result.getSkills().get(HiscoreSkill.DEFENCE).getLevel());
			firestore.addSkillToBatch("Strength", result.getSkills().get(HiscoreSkill.STRENGTH).getLevel());
			firestore.addSkillToBatch("Hitpoints", result.getSkills().get(HiscoreSkill.HITPOINTS).getLevel());
			firestore.addSkillToBatch("Ranged", result.getSkills().get(HiscoreSkill.RANGED).getLevel());
			firestore.addSkillToBatch("Prayer", result.getSkills().get(HiscoreSkill.PRAYER).getLevel());
			firestore.addSkillToBatch("Magic", result.getSkills().get(HiscoreSkill.MAGIC).getLevel());
			firestore.addSkillToBatch("Slayer", result.getSkills().get(HiscoreSkill.SLAYER).getLevel());

		}
		catch (IOException e) {
			log.error("Could not fetch hiscores for user: ", e);
		}
	}

	private String getUsername() {
		String username = client.getLauncherDisplayName();

		if (username == null) {
			if (client.getLocalPlayer() != null) {
				username = client.getLocalPlayer().getName();
			} else {
				log.warn("Both launcher display name and local player are null");
			}
		}

		return username;
	}

	@Provides
	CARoadmapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CARoadmapConfig.class);
	}
}
