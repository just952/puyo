package com.puyo.game.input;

/** 입력 처리 모드 */
public enum InputMode {
    GAME_PLAY,      // 일반 게임 플레이 (DAS/ARR 적용, 액션 명령)
    TEXT_INPUT,     // 채팅/닉네임/검색 입력 (문자 이벤트, IME 지원)
    UI_NAVIGATION   // 메뉴 탐색 (방향키만, DAS/ARR 선택적)
}