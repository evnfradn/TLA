package com.mygame.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

public class TheLastAncestorsGame extends Game {

    private int gold = 100;
    private int gems = 0;
    private Array<Quest> quests = new Array<>();

    @Override
    public void create() {
        initDefaultQuests();
        // Set ExplorationScreen sebagai layar awal saat game pertama kali dijalankan
        setScreen(new ExplorationScreen(this));
    }

    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }
    public void addGold(int amount) { this.gold += amount; }

    public int getGems() { return gems; }
    public void setGems(int gems) { this.gems = gems; }
    public void addGems(int amount) { this.gems += amount; }

    public Array<Quest> getQuests() { return quests; }

    public Quest getQuestById(String id) {
        for (Quest q : quests) {
            if (q.getId().equals(id)) return q;
        }
        return null;
    }

    public void initDefaultQuests() {
        quests.clear();
        // Daily
        quests.add(new Quest("seat", "Kembali ke Tempat Duduk", 
            "Kembali ke tempat dudukmu di baris ketiga sebelah kiri dekat jendela.", 1, "Gems", 60, "Daily"));
        quests.add(new Quest("greet_teacher", "Menyapa Pak Guru", 
            "Sapa Pak Guru sebelum pembelajaran dimulai untuk mendengar instruksi.", 1, "Gold", 100, "Daily"));
        quests.add(new Quest("explore_classroom", "Eksplorasi Kelas", 
            "Berjalanlah keliling kelas untuk bersiap belajar (WASD / Panah).", 500, "Scroll", 5, "Daily"));
        
        // Achievements
        quests.add(new Quest("first_steps", "Langkah Pertama", 
            "Lakukan perjalanan sejauh 2000 unit di dalam kelas.", 2000, "Gems", 200, "Achievements"));
        quests.add(new Quest("learn_start", "Murid yang Rajin", 
            "Mulai sesi pembelajaran pertama hari ini.", 1, "Gems", 100, "Achievements"));
        quests.add(new Quest("talk_npc", "Sosialisasi Kelas", 
            "Bicaralah dengan setidaknya 5 teman sekelas yang berbeda (Tekan E).", 5, "Gems", 150, "Achievements"));
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
