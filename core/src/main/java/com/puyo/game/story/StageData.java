package com.puyo.game.story;

public class StageData {
    public int stage_id;
    public String opponent;
    public String[] dialogue;
    public String background;
    public String music;
    public float ai_difficulty;   // 0.0 ~ 1.0
    public float ai_aggression;   // 0.0 ~ 1.0
    public float fall_speed_base; // 배수
    public int clear_to_advance;  // 필요한 승리 횟수
}
