package com.mygame.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;

public class TheLastAncestorsGame extends Game {

    @Override
    public void create() {
        // Set ExplorationScreen sebagai layar awal saat game pertama kali dijalankan
        setScreen(new ExplorationScreen(this));
    }

    // Metode jembatan delegasi agar kelas Player, Enemy, dan Character 
    // tetap dapat berjalan mulus tanpa merusak kompatibilitas kelas mereka.
    
    public void showMessage(String speaker, String message) {
        if (getScreen() instanceof BattleScreen) {
            ((BattleScreen) getScreen()).showMessage(speaker, message);
        }
    }

    public void showPopupText(boolean isEnemy, String text, Color color) {
        if (getScreen() instanceof BattleScreen) {
            ((BattleScreen) getScreen()).showPopupText(isEnemy, text, color);
        }
    }

    public void triggerScreenShake(int duration, int magnitude) {
        if (getScreen() instanceof BattleScreen) {
            ((BattleScreen) getScreen()).triggerScreenShake(duration, magnitude);
        }
    }

    public void setPlayerTempState(int state, int durationMs) {
        if (getScreen() instanceof BattleScreen) {
            ((BattleScreen) getScreen()).setPlayerTempState(state, durationMs);
        }
    }
}
