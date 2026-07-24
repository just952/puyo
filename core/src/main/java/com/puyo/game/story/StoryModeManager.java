package com.puyo.game.story;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class StoryModeManager {
    private static final String STORY_DATA_PATH = "data/story/stages.json";
    private StageData[] stages;
    private int currentStageIndex = 0;
    private int winsInCurrentStage = 0;

    public StoryModeManager() {
        loadStages();
    }

    public StoryModeManager(int startStageIndex) {
        loadStages();
        if (startStageIndex >= 0 && startStageIndex < stages.length) {
            this.currentStageIndex = startStageIndex;
        } else {
            this.currentStageIndex = 0;
        }
        this.winsInCurrentStage = 0;
    }

    private void loadStages() {
        FileHandle file = Gdx.files.internal(STORY_DATA_PATH);
        if (!file.exists()) {
            Gdx.error("StoryModeManager", "Story data file not found: " + STORY_DATA_PATH);
            stages = new StageData[0];
            return;
        }
        Json json = new Json();
        stages = json.fromJson(StageData[].class, file);
        Gdx.log("StoryModeManager", "Loaded " + stages.length + " stages.");
    }

    public StageData[] getStages() {
        return stages;
    }

    public StageData getStageAt(int index) {
        if (index < 0 || index >= stages.length) {
            return null;
        }
        return stages[index];
    }

    public StageData getCurrentStage() {
        if (stages.length == 0 || currentStageIndex >= stages.length) {
            return null;
        }
        return stages[currentStageIndex];
    }

    public void onPlayerWin() {
        winsInCurrentStage++;
        StageData current = getCurrentStage();
        if (current != null && winsInCurrentStage >= current.clear_to_advance) {
            advanceToNextStage();
        }
    }

    public void onPlayerLose() {
        // reset win count for current stage on loss (could also implement lives)
        winsInCurrentStage = 0;
    }

    public void advanceToNextStage() {
        winsInCurrentStage = 0;
        currentStageIndex++;
        if (currentStageIndex >= stages.length) {
            // all story completed
            Gdx.log("StoryModeManager", "All stages completed!");
            // could loop or stay at last stage
            currentStageIndex = stages.length - 1;
        }
    }

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

    public boolean isStoryComplete() {
        return currentStageIndex >= stages.length;
    }

    public int getCurrentStageNumber() {
        return currentStageIndex + 1;
    }

    public int getTotalStages() {
        return stages.length;
    }
}
