package com.mygame.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Timer;

public abstract class Character {
    protected String name;
    protected int hp;
    protected int maxHp;
    protected int attack;
    protected int defense;

    public Character(String name, int maxHp, int attack, int defense) {
        this.name = name;
        this.hp = maxHp;
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
    }

    public abstract void attack(Character target, TheLastAncestorsGame game);

    public void defend(int damage, final TheLastAncestorsGame game, int delayMillis) {
        final int actualDamage = Math.max(0, damage - defense);
        this.hp -= actualDamage;
        if (this.hp < 0)
            this.hp = 0;

        if (delayMillis > 0) {
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    game.showMessage(name, "menerima " + actualDamage + " damage!");
                    game.showPopupText(Character.this instanceof Enemy, "-" + actualDamage, Color.RED);
                    if (Character.this instanceof Player) {
                        game.triggerScreenShake(20, 25); // durasi 20 frame, kekuatan 25 piksel
                        game.setPlayerTempState(3, 1200); // 3: Hit state
                    }
                }
            }, delayMillis / 1000f);
        } else {
            game.showMessage(name, "menerima " + actualDamage + " damage!");
            game.showPopupText(this instanceof Enemy, "-" + actualDamage, Color.RED);
            if (this instanceof Player) {
                game.triggerScreenShake(20, 25);
                game.setPlayerTempState(3, 1200); // 3: Hit state
            }
        }
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public String getStatus() {
        return name + " (HP: " + hp + "/" + maxHp + ")";
    }

    public String getName() {
        return name;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }
}
