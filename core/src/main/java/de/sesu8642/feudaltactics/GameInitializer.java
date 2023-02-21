// SPDX-License-Identifier: GPL-3.0-or-later

package de.sesu8642.feudaltactics;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

import de.sesu8642.feudaltactics.backend.exceptions.InitializationException;
import de.sesu8642.feudaltactics.backend.persistence.AutoSaveRepository;
import de.sesu8642.feudaltactics.events.GameResumedEvent;
import de.sesu8642.feudaltactics.frontend.ScreenNavigationController;
import de.sesu8642.feudaltactics.frontend.dagger.qualifierannotations.VersionProperty;
import de.sesu8642.feudaltactics.frontend.events.ScreenTransitionTriggerEvent;
import de.sesu8642.feudaltactics.frontend.events.ScreenTransitionTriggerEvent.ScreenTransitionTarget;
import de.sesu8642.feudaltactics.frontend.persistence.GameVersionDao;

/**
 * Class for initializing the game in a non-static context that can get injected
 * dependencies.
 */
@Singleton
public class GameInitializer {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private EventBus eventBus;
	private GameVersionDao gameVersionDao;
	private AutoSaveRepository autoSaveRepository;
	private ScreenNavigationController screenNavigationController;
	private String gameVersion;

	/** Constructor. */
	@Inject
	public GameInitializer(EventBus eventBus, GameVersionDao gameVersionDao, AutoSaveRepository autoSaveRepository,
			ScreenNavigationController screenNavigationController, @VersionProperty String gameVersion) {
		this.eventBus = eventBus;
		this.gameVersionDao = gameVersionDao;
		this.autoSaveRepository = autoSaveRepository;
		this.screenNavigationController = screenNavigationController;
		this.gameVersion = gameVersion;

	}

	void initializeGame() {

		try {
			// configure logging
			LogManager logManager = LogManager.getLogManager();
			InputStream stream = GameInitializer.class.getClassLoader().getResourceAsStream("logging.properties");
			logManager.readConfiguration(stream);
		} catch (IOException e) {
			throw new InitializationException("Unable to configure logging", e);
		}

		// do not close on android back key
		Gdx.input.setCatchKey(Keys.BACK, true);

		eventBus.register(screenNavigationController);

		// show appropriate screen
		if (autoSaveRepository.getNoOfAutoSaves() > 0) {
			// resume running game
			eventBus.post(new ScreenTransitionTriggerEvent(ScreenTransitionTarget.INGAME_SCREEN));
			eventBus.post(new GameResumedEvent());
		} else {
			// fresh start
			eventBus.post(new ScreenTransitionTriggerEvent(ScreenTransitionTarget.SPLASH_SCREEN));
		}

		String previousVersion = gameVersionDao.getGameVersion();
		if (!Strings.isNullOrEmpty(previousVersion) && !previousVersion.equals(gameVersion)) {
			// first start after update
			logger.info("game was updated from version {} to {}", previousVersion, gameVersion);
			gameVersionDao.saveChangelogState(true);
		}

		// save current game version
		gameVersionDao.saveGameVersion(gameVersion);

	}

}
