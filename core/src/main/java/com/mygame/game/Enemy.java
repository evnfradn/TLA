package com.mygame.game;

public class Enemy extends Character {
    private String enemyType;

    public Enemy(String name, int maxHp, int attack, int defense, String enemyType) {
        super(name, maxHp, attack, defense);
        this.enemyType = enemyType;
    }

    @Override
    public void attack(Character target, TheLastAncestorsGame game) {
        game.showMessage(name, "Mengayunkan gada raksasanya ke " + target.getName() + "!");
        // Tunda popup damage agar selaras dengan pukulan di GIF serangan (~setelah wind-up)
        target.defend(this.attack, game, 2400);
    }
}
