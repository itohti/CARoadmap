package com.caroadmap;

import com.caroadmap.api.CARoadmapServer;
import com.caroadmap.api.PlayerDataBatcher;
import com.caroadmap.api.WiseOldMan;
import com.caroadmap.data.*;
import com.caroadmap.dto.TaskDTO;
import com.caroadmap.ui.CAKillCounter;
import com.caroadmap.ui.CARoadmapPanel;
import com.caroadmap.ui.CASpeedCounter;
import com.google.gson.Gson;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.*;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.hiscore.Skill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.runelite.client.util.Text;

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

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private Gson gson;

	private CARoadmapPanel caRoadmapPanel;
	private RecommendationCacheHandler recommendationCacheHandler;
	private NavigationButton navButton;

    private PlayerDataBatcher playerDataBatcher;
	private WiseOldMan wiseOldMan;

	private boolean getData = false;
	private Boss[] playerBossData;
	private Map<String, Boss> bossLookup;

	private RecommendTasks recommendTasks;
	private String username;
	private boolean hasFetched = false;

	private ExecutorService databaseExecutor;
	private ExecutorService generalExecutor;

	private final CombatSessionManager combatSessionManager = new CombatSessionManager();

	@Override
	protected void startUp() throws Exception
	{
		this.caRoadmapPanel = new CARoadmapPanel(spriteManager, configManager, combatSessionManager);
		this.recommendationCacheHandler = new RecommendationCacheHandler(gson);
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

		databaseExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			t.setName("Database Thread");
			return t;
		});

		generalExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			t.setName("General Thread");
			return t;
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		databaseExecutor.shutdown();
		generalExecutor.shutdown();
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

		// this will get what the player is interacting with.
		NPC engagedBoss = getEngagedBoss();

		CombatSession session =
				combatSessionManager.getCurrentSession();


		/*
		 * Boss is actively engaging the player
		 */
		if (engagedBoss != null)
		{
			if (session == null)
			{
				startCombatSession(engagedBoss);
			}
			else
			{
				if (session.isBossDefeated()) {
					session.startNextAttempt();
				}

				session.updateBoss(engagedBoss);
				session.heartbeat();
			}
		}


		/*
		 * Existing session but boss is temporarily inactive
		 */
		session =
				combatSessionManager.getCurrentSession();

		if (session != null)
		{
			// Highest priority: player left the boss instance.
			if (session.isInstanced() && hasLeftInstance(session))
			{
				log.info("Player left instance");

				session.invalidateStreak();
				combatSessionManager.endSession();
			}
			// Fallback
			else if (session.shouldEnd())
			{
				log.info(
						"Combat session ended due to inactivity"
				);

				combatSessionManager.endSession();
			}
		}


		/*
		 * Update combat UI
		 */
		session =
				combatSessionManager.getCurrentSession();

		if (session != null)
		{
			SwingUtilities.invokeLater(() ->
					caRoadmapPanel.refreshCombat()
			);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		if (event.getType() == ChatMessageType.GAMEMESSAGE) {
			String msg = Text.removeTags(event.getMessage());
			// check to see if the player defeated a boss or raid.
			Pattern killCountPattern = Pattern.compile("Your (.+?) (?:kill count|count) is: (\\d+)");
			Matcher killCountMatcher = killCountPattern.matcher(msg);

			Pattern failedTaskPattern = Pattern.compile("^You have failed (.*?):");
			Matcher failedTaskMatcher = failedTaskPattern.matcher(msg);

			if (failedTaskMatcher.find())
			{
				String taskTitle = failedTaskMatcher.group(1).trim();

				CombatSession session =
						combatSessionManager.getCurrentSession();

				if (session != null)
				{
					log.info("Combat task failed: {}", taskTitle);

					session.failTask(taskTitle);

					SwingUtilities.invokeLater(() ->
							caRoadmapPanel.refreshCombat()
					);
				}
			}

			if (killCountMatcher.find()) {
				String boss = killCountMatcher.group(1);
				int killCount = Integer.parseInt(killCountMatcher.group(2));
				CombatSession session =
						combatSessionManager.getCurrentSession();

				if (session != null)
				{
					if (session.getBossName()
							.equalsIgnoreCase(boss))
					{
						log.info(
								"Updating combat session kill count {} -> {}",
								boss,
								killCount
						);

						combatSessionManager.updateKillCount(
								killCount
						);

						generalExecutor.submit(() -> {
							server.updatePlayerBossData(client.getAccountHash(), normalizeBossName(boss), killCount);
						});

						session.completeAttempt();

						SwingUtilities.invokeLater(() ->
								caRoadmapPanel.refreshCombat()
						);
					}
				}
			}


			// check to see if the player completed a combat task.
			if (msg.contains("combat task") && msg.contains("completed")) {
				try {
					Pattern pattern = Pattern.compile(
							"combat task: (.*?) \\(\\d+ points\\)",
							Pattern.CASE_INSENSITIVE
					);
					Matcher matcher = pattern.matcher(msg);

					if (matcher.find()) {
						String taskName = matcher.group(1).trim();

						boolean removed = caRoadmapPanel.taskCompleted(taskName);
						if (removed) {
							log.info("Successfully marked task as complete");
						}

						CombatSession session = combatSessionManager.getCurrentSession();

						session.completeTask(taskName);

						generalExecutor.submit(() -> {
							server.updatePlayerTaskStatus(client.getAccountHash(), taskName);
						});

						caRoadmapPanel.refresh();
					}
				}
				catch (Exception e) {
					log.error("Something went wrong with getting task name", e);
				}
			}
		}
	}

//	@Subscribe
//	public void onWidgetLoaded(WidgetLoaded event) {
//		// we can see if the user completed a task with the widget pop up now.
//		if (event.getGroupId() == 660) {
//			Widget popupTextWidget = client.getWidget(660, 8);
//			if (popupTextWidget != null && popupTextWidget.getText() != null) {
//				String rawText = popupTextWidget.getText();
//				log.info("Combat task popup text: {}", rawText);
//
//				String cleanText = rawText.replaceAll("<[^>]+>", "").trim();
//				log.info("Cleaned task text: {}", cleanText);
//			}
//		}
//	}

	private boolean hasLeftInstance(CombatSession session)
	{
		WorldView worldView = client.getTopLevelWorldView();
        return !worldView.isInstance();
    }

	private NPC getEngagedBoss()
	{
		Player player = client.getLocalPlayer();

		WorldView worldView = client.getTopLevelWorldView();

		if (worldView == null)
		{
			return null;
		}

		for (NPC npc : worldView.npcs())
		{
			if (npc == null || npc.getName() == null)
			{
				continue;
			}

			String bossName = normalizeBossName(npc.getName());

			if (!bossLookup.containsKey(bossName))
			{
				continue;
			}

			if (npc.getInteracting() == player || player.getInteracting() == npc)
			{
				return npc;
			}
		}

		return null;
	}

	private void startCombatSession(NPC boss)
	{
		WorldView worldView = client.getTopLevelWorldView();
		combatSessionManager.startSession(boss, worldView.isInstance());

		CombatSession session =
				combatSessionManager.getCurrentSession();


		log.info(
				"Started combat session for {}",
				session.getBossName()
		);


		generalExecutor.submit(() ->
		{
			log.info(
					"Fetching combat tasks for {}",
					session.getBossName()
			);


			ArrayList<Task> incompleteTasks =
					new ArrayList<>();


			for (TaskDTO dto :
					server.fetchTaskFromBoss(
							normalizeBossName(session.getBossName()),
							client.getAccountHash()
					))
			{
				try
				{
					incompleteTasks.add(
							TaskMapper.fromDTO(dto)
					);
				}
				catch (Exception e)
				{
					log.error(
							"Failed converting task",
							e
					);
				}
			}


			session.setTasks(incompleteTasks);


			log.info(
					"Combat session tasks loaded: {}",
					incompleteTasks
			);


			SwingUtilities.invokeLater(() ->
					caRoadmapPanel.refreshCombat()
			);
		});
	}

	private void fetchData() {
		// store character information to the db
		this.username = getUsername();
		long accountHash = client.getAccountHash();
		// Initialize classes that are dependent on username
		this.recommendTasks = new RecommendTasks(server, configManager, recommendationCacheHandler);
		caRoadmapPanel.setRecommendTasks(recommendTasks);
		this.wiseOldMan = new WiseOldMan(username, gson);

		databaseExecutor.submit(() -> {
			this.playerDataBatcher = new PlayerDataBatcher(username, accountHash, server, gson);
			fetchAndStorePlayerSkills(username);
			playerBossData = wiseOldMan.fetchBossInfo();
			if (playerBossData.length == 0) {
				log.error("Could not receive boss data.");
			}
			bossLookup = Arrays.stream(playerBossData)
					.collect(Collectors.toMap(
							Boss::getBoss,
							b -> b
					));
			for (Boss boss : playerBossData) {
				// before we send it to db get pb.
				Double pbDouble = configManager.getRSProfileConfiguration(
						"personalbest", boss.getBoss().toLowerCase(), Double.class
				);
				int pb = (pbDouble != null) ? pbDouble.intValue() : 0;
				boss.setKillTime(pb);
				if (!playerDataBatcher.addBossToBatch(boss)) {
					log.error("Could not add boss [{}] to batch", boss.getBoss());
				}
			}
		});

		fetchAndStorePlayerTasks();
		databaseExecutor.submit(() -> {
			boolean result = playerDataBatcher.sendData();
			if (!result) {
				log.error("Did not upload player data to database");
			}
		});

		generalExecutor.submit(() -> {
			recommendTasks.getRecommendations(accountHash);

			SwingUtilities.invokeLater(() -> {
				if (caRoadmapPanel != null) {
					caRoadmapPanel.setCharacterId(accountHash);
					caRoadmapPanel.refresh();
					caRoadmapPanel.taskCompleted("Duke Sucellus Adept");
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
				// add to db
				databaseExecutor.submit(() -> {
					boolean result = playerDataBatcher.addTaskToBatch(taskObject);
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
			// adding skills in db
			for (Map.Entry<HiscoreSkill, Skill> entry : result.getSkills().entrySet()) {
				HiscoreSkill skillName = entry.getKey();

				// make sure we only get skills not activities.
				if (skillName.getType() == HiscoreSkillType.SKILL) {
					Skill skill = entry.getValue();
					playerDataBatcher.addSkillToBatch(skillName.getName(), skill.getLevel());
				}
			}
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

	private static String normalizeBossName(String metric) {
		return metric
				.toLowerCase()
				.replace("_", " ")
				.replace(":", " ")
				.replace("-", " ")
				.replace("'", "")
				.replaceAll("\\s+", " ")
				.trim();
	}

	@Provides
	CARoadmapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CARoadmapConfig.class);
	}
}
