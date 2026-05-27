package com.mygame.game;

import com.badlogic.gdx.graphics.Color;

public class Player extends Character {
    private int level;
    private int exp;
    private int mp;
    private int maxMp;

    public Player(String name, int maxHp, int attack, int defense) {
        super(name, maxHp, attack, defense);
        this.level = 1;
        this.exp = 0;
        this.maxMp = 50;
        this.mp = 50;
    }

    @Override
    public void attack(Character target, TheLastAncestorsGame game) {
        game.showMessage(name, "Menyerang " + target.getName() + " dengan tebasan pedang!");
        target.defend(this.attack, game, 1000); // Standard 1s delay
    }

    // Method Overloading
    public void attack(Character target, TheLastAncestorsGame game, String skillName, int damageMultiplier) {
        game.showMessage(name, "Mengeluarkan skill " + skillName + "!");
        int totalDamage = this.attack * damageMultiplier;
        target.defend(totalDamage, game, 1000); // Standard 1s delay
    }

    public void heal(int amount, int energyRecharge, TheLastAncestorsGame game) {
        this.hp += amount;
        if (this.hp > maxHp)
            this.hp = maxHp;
        if (energyRecharge > 0) {
            game.showMessage(name,
                    "Menggunakan Potion! Memulihkan " + amount + " HP & meregenerasi " + energyRecharge + " EN.");
            game.showPopupText(false, "+" + amount + " HP  +" + energyRecharge + " EN", Color.GREEN);
        } else {
            game.showMessage(name, "Menggunakan Potion! Memulihkan " + amount + " HP.");
            game.showPopupText(false, "+" + amount, Color.GREEN);
        }
    }

    public void gainExp(int amount, TheLastAncestorsGame game) {
        this.exp += amount;
        game.showMessage(name, "mendapatkan " + amount + " EXP!");
        if (this.exp >= 100) {
            this.exp -= 100;
            this.level++;
            this.maxHp += 20;
            this.hp = maxHp;
            this.attack += 5;
            this.maxMp += 10;
            this.mp = maxMp;
            game.showMessage(name, "LEVEL UP! Sekarang Level " + level + "!");
        }
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
    }

    public int getMaxExp() {
        return 100;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = Math.max(0, Math.min(this.maxMp, mp));
    }

    public int getMaxMp() {
        return maxMp;
    }
}
