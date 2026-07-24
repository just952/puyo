package com.puyo.game.menus;

public class MenuItem {
    public String id;
    public String label;
    public String action;     // e.g., push_screen, start_game, exit_game
    public String target;     // screen ID (used when action is push_screen)
    public String mode;       // game mode (used when action is start_game, e.g., endless, normal)
}
