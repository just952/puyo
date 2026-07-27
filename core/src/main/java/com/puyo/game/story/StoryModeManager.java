package com.puyo.game.story;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

/**
 * 스토리 모드의 진행 상태와 단계 데이터를 관리하는 매니저 클래스.
 * 외부 JSON 파일에서 단계 정보를 로드하고, 승리/패배에 따라 현재 단계를 업데이트한다.
 */
public class StoryModeManager {
    /** 스토리 데이터 JSON 파일의 경로 (assets 내부) */
    private static final String STORY_DATA_PATH = "data/story/stages.json";
    /** 로드된 모든 단계 데이터 배열 */
    private StageData[] stages;
    /** 현재 플레이 중인 단계의 인덱스 (0부터 시작) */
    private int currentStageIndex = 0;
    /** 현재 단계에서 연속으로 얻은 승리 횟수 */
    private int winsInCurrentStage = 0;

    /**
     * 기본 생성자 – 첫 번째 스테이지부터 시작한다.
     */
    public StoryModeManager() {
        loadStages();
    }

    /**
     * 특정 인덱스의 스테이지부터 시작하도록 하는 생성자.
     * 메뉴에서 스테이지를 직접 선택할 때 사용된다.
     *
     * @param startStageIndex 시작할 단계의 인덱스 (0 기반)
     */
    public StoryModeManager(int startStageIndex) {
        loadStages();
        if (startStageIndex >= 0 && startStageIndex < stages.length) {
            this.currentStageIndex = startStageIndex;
        } else {
            this.currentStageIndex = 0; // 범위를 벗어나면 처음부터 시작
        }
        this.winsInCurrentStage = 0;
    }

    /**
     * JSON 파일에서 단계 데이터를 읽어들여 stages 배열에 저장한다.
     * 파일이 존재하지 않으면 빈 배열을 할당하고 에러를 로그에 남긴다.
     */
    private void loadStages() {
        // 1) classpath 리소스에서 먼저 시도 (테스트용)
        FileHandle file = Gdx.files.classpath(STORY_DATA_PATH);
        
        // 2) 없으면 internal (애셋/데스크톱/안드로이드) 폴백
        if (file == null || !file.exists()) {
            file = Gdx.files.internal(STORY_DATA_PATH);
        }
        
        // 3) 여전히 없으면 Java ClassLoader 직접 시도 (헤드리스 테스트 리소스용)
        if (file == null || !file.exists()) {
            try {
                // 테스트 환경에서는 context ClassLoader가 테스트 리소스를 가짐
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = getClass().getClassLoader();
                }
                try (java.io.InputStream is = cl.getResourceAsStream(STORY_DATA_PATH)) {
                    if (is != null) {
                        Json json = new Json();
                        try {
                            StoryDataWrapper wrapper = json.fromJson(StoryDataWrapper.class, is);
                            stages = wrapper != null ? wrapper.stages : new StageData[0];
                            Gdx.app.log("StoryModeManager", "Loaded " + stages.length + " stages from ClassLoader.");
                            return;
                        }
                    }
                } catch (Exception e) {
                    Gdx.app.error("StoryModeManager", "Failed to load from ClassLoader", e);
                }
            }

        if (file == null || !file.exists()) {
            // 파일을 찾을 수 없을 때는 오류 로그를 출력하고 빈 배열로 초기화
            Gdx.app.error("StoryModeManager", "Story data file not found: " + STORY_DATA_PATH);
            stages = new StageData[0];
            return;
        }
        
        Json json = new Json();
        try {
            // JSON은 {"stages": [...]} 래퍼 객체이므로 중간 클래스로 파싱
            StoryDataWrapper wrapper = json.fromJson(StoryDataWrapper.class, file);
            stages = wrapper != null ? wrapper.stages : new StageData[0];
        } catch (Exception e) {
            Gdx.app.error("StoryModeManager", "Failed to parse story JSON", e);
            stages = new StageData[0];
        }
        
        // 로드 성공 로그 출력
        Gdx.app.log("StoryModeManager", "Loaded " + stages.length + " stages.");
    }

    /** JSON 래퍼 클래스 */
    private static class StoryDataWrapper {
        public StageData[] stages;
        public int total_stages;
    }

    /** 전체 단계 배열을 반환한다. */
    public StageData[] getStages() {
        return stages;
    }

    /** 주어진 인덱스의 단계 데이터를 반환한다. */
    public StageData getStageAt(int index) {
        if (index < 0 || index >= stages.length) {
            return null;
        }
        return stages[index];
    }

    /** 현재 플레이 중인 단계 데이터를 반환한다. */
    public StageData getCurrentStage() {
        if (stages.length == 0 || currentStageIndex >= stages.length) {
            return null;
        }
        return stages[currentStageIndex];
    }

    /** 플레이어가 현재 단계에서 한 판을 이겼을 때 호출된다. */
    public void onPlayerWin() {
        winsInCurrentStage++;
        StageData current = getCurrentStage();
        if (current != null && winsInCurrentStage >= current.clear_to_advance) {
            advanceToNextStage();
        }
    }

    /** 플레이어가 현재 단계에서 한 판을 졌을 때 호출된다. */
    public void onPlayerLose() {
        winsInCurrentStage = 0;
    }

    /** 다음 단계로 진행한다. */
    public void advanceToNextStage() {
        winsInCurrentStage = 0;
        currentStageIndex++;
        if (currentStageIndex >= stages.length) {
            Gdx.app.log("StoryModeManager", "All stages completed!");
            currentStageIndex = stages.length - 1;
        }
    }

    /** 현재 단계 인덱스를 직접 지정한다. */
    public void setCurrentStageIndex(int index) {
        if (index < 0) {
            index = 0;
        }
        if (index >= stages.length) {
            index = stages.length - 1;
        }
        this.currentStageIndex = index;
        this.winsInCurrentStage = 0;
    }

    /** 모든 스토리 스테이지를 클리어했는지 여부를 반환한다. */
    public boolean isStoryComplete() {
        return currentStageIndex >= stages.length;
    }

    /** 현재 진행 중인 단계의 1-based 번호를 반환한다. */
    public int getCurrentStageNumber() {
        return currentStageIndex + 1;
    }

    /** JSON에 정의된 전체 스테이지 수를 반환한다. */
    public int getTotalStages() {
        return stages.length;
    }

    /** 현재까지 잠금 해제된 스테이지 수를 반환한다. */
    public int getUnlockedStageCount() {
        if (isStoryComplete()) {
            return stages.length;
        }
        return currentStageIndex + 1;
    }
}
