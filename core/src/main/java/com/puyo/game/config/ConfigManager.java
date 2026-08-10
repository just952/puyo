package com.puyo.game.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class ConfigManager {
    private static final String CONFIG_DIR = "assets/config/";
    private static ConfigManager instance;
    private GameConfig config;

    private static String getEnvironment() {
        String env = System.getProperty("game.env");
        if (env == null || env.isEmpty()) {
            env = "development"; // 기본값: 개발 환경
        }
        return env.toLowerCase();
    }

    private ConfigManager() {
        String env = getEnvironment();
        FileHandle file = Gdx.files.internal(CONFIG_DIR + env + ".json");

        if (!file.exists()) {
            Gdx.app.error("ConfigManager", "Config file not found: " + env + ".json");
            config = new GameConfig(); // 기본값으로 fallback
        } else {
            Json json = new Json();
            config = json.fromJson(GameConfig.class, file);
        }

        Gdx.app.log("ConfigManager", "Loaded config for environment: " + env + " | Log level: " + config.log_level);
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public GameConfig getConfig() {
        return config;
    }

    public static class GameConfig {
        public String env = "development";
        public String api_base_url = "";
        public String ws_url = "";
        public String wss_url = "";
        public String log_level = "info"; // 기본값: info
        public boolean enable_keepalive = false;
        public boolean enable_cheats = false;
        public String matchmaking_server = "";
        public String player_data_endpoint = "";
        public String matchmaking_endpoint = "";
        public int heartbeat_interval_sec = 0;
        // 필요 시 필드 추가 가능
    }
}
