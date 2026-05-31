package com.mygame.game;

public class Quest {
    private final String id;
    private final String title;
    private final String description;
    private int progress;
    private final int target;
    private final String rewardType; // "Gems", "Gold", "Scroll"
    private final int rewardAmount;
    private boolean isCompleted;
    private boolean isClaimed;
    private final String tab; // "Daily" or "Achievements"

    public Quest(String id, String title, String description, int target, String rewardType, int rewardAmount, String tab) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.progress = 0;
        this.target = target;
        this.rewardType = rewardType;
        this.rewardAmount = rewardAmount;
        this.isCompleted = false;
        this.isClaimed = false;
        this.tab = tab;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, target);
        if (this.progress >= target) {
            this.isCompleted = true;
        }
    }

    public void addProgress(int amount) {
        setProgress(this.progress + amount);
    }

    public int getTarget() {
        return target;
    }

    public String getRewardType() {
        return rewardType;
    }

    public int getRewardAmount() {
        return rewardAmount;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public boolean isClaimed() {
        return isClaimed;
    }

    public void setClaimed(boolean claimed) {
        this.isClaimed = claimed;
    }

    public String getTab() {
        return tab;
    }
}
