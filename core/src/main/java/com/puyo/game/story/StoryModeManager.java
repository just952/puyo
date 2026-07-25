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
        FileHandle file = Gdx.files.internal(STORY_DATA_PATH);
        if (!file.exists()) {
            // 파일을 찾을 수 없을 때는 오류 로그를 출력하고 빈 배열로 초기화
            Gdx.app.error("StoryModeManager", "Story data file not found: " + STORY_DATA_PATH);
            stages = new StageData[0];
            return;
        }
        Json json = new Json();
        stages = json.fromJson(StageData[].class, file);
        // 로드 성공 로그 출력
        Gdx.app.log("StoryModeManager", "Loaded " + stages.length + " stages.");
    }

    /**
     * 전체 단계 배열을 반환한다.
     * UI에서 목록을 표시하거나 전체 개수를 확인할 때 사용한다.
     *
     * @return StageData 객체들의 배열
     */
    public StageData[] getStages() {
        return stages;
    }

    /**
     * 주어진 인덱스의 단계 데이터를 반환한다.
     * 인덱스가 유효하지 않으면 null을 반환한다.
     *
     * @param index 조회하고 싶은 단계의 인덱스 (0 기반)
     * @return 해당 인덱스의 StageData 객체 또는 null
     */
    public StageData getStageAt(int index) {
        if (index < 0 || index >= stages.length) {
            return null;
        }
        return stages[index];
    }

    /**
     * 현재 플레이 중인 단계 데이터를 반환한다.
     *
     * @return 현재 단계의 StageData 객체 (데이터가 없으면 null)
     */
    public StageData getCurrentStage() {
        if (stages.length == 0 || currentStageIndex >= stages.length) {
            return null;
        }
        return stages[currentStageIndex];
    }

    /**
     * 플레이어가 현재 단계에서 한 판을 이겼을 때 호출된다.
     * 승리 횟수를 증가시키고, 클리어 조건을 만족하면 다음 단계로 진행한다.
     */
    public void onPlayerWin() {
        winsInCurrentStage++;
        StageData current = getCurrentStage();
        if (current != null && winsInCurrentStage >= current.clear_to_advance) {
            advanceToNextStage();
        }
    }

    /**
     * 플레이어가 현재 단계에서 한 판을 졌을 때 호출된다.
     * 현재 단계의 승리 횟수를 0으로 리설정한다.
     * (추후 생명 시스템을 도입할 경우 여기서 라이프를 감소시킬 수 있다.)
     */
    public void onPlayerLose() {
        // 현재 단계의 승리 카운트 리셋
        winsInCurrentStage = 0;
    }

    /**
     * 다음 단계로 진행한다.
     * 승리 카운트를 리셋하고 현재 단계 인덱스를 증가시킨다.
     * 모든 스토리를 클리어했을 경우 마지막 단계에 머무르게 한다.
     */
    public void advanceToNextStage() {
        winsInCurrentStage = 0;
        currentStageIndex++;
        if (currentStageIndex >= stages.length) {
            // 모든 스토리 클리어 시 로그 출력 (필요에 따라 엔딩 루프 등으로 확장 가능)
            Gdx.app.log("StoryModeManager", "All stages completed!");
            // 마지막 단계에 머물러 반복 플레이 가능하게 함
            currentStageIndex = stages.length - 1;
        }
    }

    /**
     * 현재 단계 인덱스를 직접 지정한다.
     * 주로 스테이지 선택 화면에서 사용된다.
     *
     * @param index 설정하고 싶은 단계의 인덱스 (0 기반)
     */
    public void setCurrentStageIndex(int index) {
        if (index < 0) {
            index = 0;
        }
        if (index >= stages.length) {
            index = stages.length - 1;
        }
        this.currentStageIndex = index;
        this.winsInCurrentStage = 0; // 단계 변경 시 승리 카운트 초기화
    }

    /**
     * 모든 스토리 스테이지를 클리어했는지 여부를 반환한다.
     *
     * @return 모든 스테이지를 클리어했으면 true, 그렇지 않으면 false
     */
    public boolean isStoryComplete() {
        return currentStageIndex >= stages.length;
    }

    /**
     * 현재 진행 중인 단계의 1-based 번호를 반환한다.
     * UI에 "Stage X / Y" 형태 표시 시 사용한다.
     *
     * @return 현재 단계 번호 (1부터 시작)
     */
    public int getCurrentStageNumber() {
        return currentStageIndex + 1;
    }

    /**
     * JSON에 정의된 전체 스테이지 수를 반환한다.
     *
     * @return 전체 스테이지 수
     */
    public int getTotalStages() {
        return stages.length;
    }

    /**
     * 현재까지 잠금 해제된 스테이지 수를 반환한다.
     * 클리어한 스테이지 수에 현재 플레이 가능한 단계를 더한 값이다.
     * 모든 스토리를 클리어했을 경우 전체 수를 반환한다.
     *
     * @return 잠금 해제된 스테이지 수
     */
    public int getUnlockedStageCount() {
        if (isStoryComplete()) {
            return stages.length;
        }
        return currentStageIndex + 1; // 클리어한 스테이지 수 + 현재 진행 중인 스테이지
    }
}
