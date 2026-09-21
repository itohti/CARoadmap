package com.caroadmap;

import com.caroadmap.api.CARoadmapServer;
import com.caroadmap.api.PlayerDataBatcher;
import com.caroadmap.api.WiseOldMan;
import com.caroadmap.data.*;
import com.caroadmap.dto.TaskDTO;
import com.caroadmap.ui.BossNameUtil;
import com.caroadmap.ui.CARoadmapPanel;
import com.google.gson.Gson;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
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
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.hiscore.Skill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
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
	private WiseOldMan wiseOldMan;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private Gson gson;

	private CARoadmapPanel caRoadmapPanel;
	private RecommendationCacheHandler recommendationCacheHandler;
	private NavigationButton navButton;

    private PlayerDataBatcher playerDataBatcher;

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
		this.caRoadmapPanel = new CARoadmapPanel(spriteManager, configManager, combatSessionManager, generalExecutor);
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
	public void onConfigChanged(ConfigChanged event)
	{
		if ("CARoadmap".equals(event.getGroup()) && "targetRewardTier".equals(event.getKey()))
		{
			clientThread.invoke(this::reloadRecommendationsForTarget);
		}
	}

	/**
	 * Re-resolves the target tier and, if the point goal actually changed,
	 * refetches recommendations for it and refreshes the panel. Runs on the
	 * client thread (reads varbits); the network call is handed to the executor.
	 */
	private void reloadRecommendationsForTarget()
	{
		if (recommendTasks == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		RewardTier targetTier = resolveTargetTier();
		Integer pointsToTarget = pointsNeededOrNull(targetTier);

		if (Objects.equals(recommendTasks.getPointsNeeded(), pointsToTarget))
		{
			return;
		}

		recommendTasks.setPointsNeeded(pointsToTarget);
		long accountHash = client.getAccountHash();
		log.info("Target tier changed to {}; refetching recommendations (points needed: {})", targetTier, pointsToTarget);

		generalExecutor.submit(() ->
		{
			try
			{
				recommendTasks.getRecommendations(accountHash);
			}
			catch (Exception e)
			{
				log.error("Failed to refresh recommendations after tier change", e);
			}

			SwingUtilities.invokeLater(() ->
			{
				if (caRoadmapPanel != null)
				{
					caRoadmapPanel.refresh();
				}
			});
		});
	}

	/**
	 * Point gap to {@code targetTier}, or {@code null} when the player is already
	 * at/past it (the server then returns its default hard cap of 20).
	 * Reads varbits - call on the client thread.
	 */
	private Integer pointsNeededOrNull(RewardTier targetTier)
	{
		int gap = CombatAchievementProgress.pointsNeededFor(client, targetTier);
		return gap > 0 ? gap : null;
	}

	/**
	 * Returns the effective target reward tier, snapping the config value back up
	 * if the player picked a tier below one they have already unlocked. Reads
	 * varbits, so must be called on the client thread.
	 */
	private RewardTier resolveTargetTier()
	{
		RewardTier selected = config.targetRewardTier();

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return selected;
		}

		RewardTier completed = CombatAchievementProgress.getCompletedTier(client);
		if (completed != null && selected.ordinal() < completed.ordinal())
		{
			log.info("Target tier {} is below completed tier {}; snapping to {}.", selected, completed, completed);
			configManager.setConfiguration("CARoadmap", "targetRewardTier", completed);
			return completed;
		}

		return selected;
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
				String taskTitle = failedTaskMatcher.group(1).replaceAll("@[A-Za-z0-9_]+@", "").trim();

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
							server.updatePlayerBossData(client.getAccountHash(), BossNameUtil.normalizeForDatabase(boss), killCount);
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
						// Jagex wraps the task name in chat link tokens like "@ach_comp@" that
						// Text.removeTags() does not strip, so remove any "@token@" markers here.
						String taskName = matcher.group(1).replaceAll("@[A-Za-z0-9_]+@", "").trim();

						SwingUtilities.invokeLater(() -> {
							boolean removed = caRoadmapPanel.taskCompleted(taskName);
							if (removed) {
								log.info("Successfully marked task as complete");
							}
							caRoadmapPanel.refresh();
						});

						CombatSession session = combatSessionManager.getCurrentSession();

						session.completeTask(taskName);

						generalExecutor.submit(() -> {
							server.updatePlayerTaskStatus(client.getAccountHash(), taskName);
						});
					}
				}
				catch (Exception e) {
					log.error("Something went wrong with getting task name", e);
				}
			}
		}
	}

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

			String bossName = BossNameUtil.normalizeForDatabase(npc.getName());

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
							BossNameUtil.normalizeForDatabase(session.getBossName()),
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

		// Combat Achievement points progress (read on the client thread).
		int caPoints = CombatAchievementProgress.getCurrentPoints(client);
		RewardTier targetTier = resolveTargetTier();
		Integer pointsToTarget = pointsNeededOrNull(targetTier);
		log.info(
			"CA points: {} | target tier: {} ({} pts) | points needed: {}",
			caPoints,
			targetTier,
			CombatAchievementProgress.getThreshold(client, targetTier),
			pointsToTarget
		);

		// Initialize classes that are dependent on username
		this.recommendTasks = new RecommendTasks(server, configManager, recommendationCacheHandler, pointsToTarget);
		caRoadmapPanel.setRecommendTasks(recommendTasks);

		databaseExecutor.submit(() -> {
			this.playerDataBatcher = new PlayerDataBatcher(username, accountHash, server, gson);
			fetchAndStorePlayerSkills(username);
			playerBossData = wiseOldMan.fetchBossInfo(username);
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

		generalExecutor.submit(() -> {
			boolean result = playerDataBatcher.sendData();
			if (!result) {
				log.error("Did not upload player data to database");
			}
			else {
				try {
					recommendTasks.getRecommendations(accountHash);
				}
				catch (Exception e) {
					log.error("Failed to get recommendations with error", e);
				}
			}

			SwingUtilities.invokeLater(() -> {
				if (caRoadmapPanel != null) {
					caRoadmapPanel.setCharacterId(accountHash);
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

	@Provides
	CARoadmapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CARoadmapConfig.class);
	}
}
