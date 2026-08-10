package com.puyo.game.util;

import com.badlogic.gdx.Gdx;
import com.puyo.game.config.ConfigManager;

/**
 * 로깅 유틸리티 클래스.
 * ConfigManager의 log_level 설정에 따라 디버그 로그를 제어합니다.
 * development.json: log_level=debug → 디버그 로그 출력
 * production.json: log_level=info → 디버그 로그 미출력
 */
public class LogUtil {

    /**
     * 현재 환경이 디버그 모드인지 확인
     */
    private static boolean isDebug() {
        try {
            return "debug".equalsIgnoreCase(ConfigManager.getInstance().getConfig().log_level);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 디버그 로그 출력 (디버그 모드일 때만 출력)
     */
    public static void debug(String tag, String message) {
        if (isDebug()) {
            Gdx.app.log(tag, message);
        }
    }

    /**
     * 디버그 로그 출력 (예외 포함, 디버그 모드일 때만 출력)
     */
    public static void debug(String tag, String message, Throwable throwable) {
        if (isDebug()) {
            Gdx.app.log(tag, message, throwable);
        }
    }

    /**
     * 정보 로그 출력 (항상 출력)
     */
    public static void info(String tag, String message) {
        Gdx.app.log(tag, message);
    }

    /**
     * 에러 로그 출력 (항상 출력)
     */
    public static void error(String tag, String message) {
        Gdx.app.error(tag, message);
    }

    /**
     * 에러 로그 출력 (예외 포함, 항상 출력)
     */
    public static void error(String tag, String message, Throwable throwable) {
        Gdx.app.error(tag, message, throwable);
    }
}