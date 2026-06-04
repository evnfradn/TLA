package com.mygame.game.lwjgl3;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AssetChecker {
    public static void main(String[] args) {
        System.out.println("=== STARTING ASSET VERIFICATION ===");
        File assetsDir = new File("assets");
        if (!assetsDir.exists() || !assetsDir.isDirectory()) {
            assetsDir = new File("../assets");
        }
        System.out.println("Assets directory path: " + assetsDir.getAbsolutePath());
        if (!assetsDir.exists()) {
            System.err.println("CRITICAL: assets directory not found!");
            System.exit(1);
        }

        List<String> pathsToCheck = new ArrayList<>();

        // Fonts
        pathsToCheck.add("Battle/PressStart2P.ttf");

        // UI
        pathsToCheck.add("Eksplore/UI/Dialog_Box.png");
        pathsToCheck.add("Eksplore/UI/Check_Box_Yes.png");
        pathsToCheck.add("Eksplore/UI/Check_Box_No.png");

        // Battle UI Icons
        pathsToCheck.add("Battle/BattleSceneTestMap.png");
        pathsToCheck.add("Battle/player_knight.png");
        pathsToCheck.add("Battle/Menu/Attack_Icon.png");
        pathsToCheck.add("Battle/Menu/Inventory_Icon.png");
        pathsToCheck.add("Battle/Menu/Run_Icon.png");
        pathsToCheck.add("Battle/Skill/Punch_Icon.png");
        pathsToCheck.add("Battle/Skill/Slash_Icon.png");
        pathsToCheck.add("Battle/Skill/Evade_Icon.png");
        pathsToCheck.add("Battle/Skill/Heal_Icon.png");
        pathsToCheck.add("Battle/Menu/Back_Icon.png");

        // Treant Stage 1
        addFramesSingle(pathsToCheck, "Battle/Treant/Stage 1/EyeSpawn", "Spawn", 9, "123456789");
        addFramesSingle(pathsToCheck, "Battle/Treant/Stage 1/EyeIdle", "Idle", 10, "123456789A");
        addFramesSingle(pathsToCheck, "Battle/Treant/Stage 1/EyeAttack", "EyeAttack", 18, "123456789ABCDEFGHI");
        addFrames(pathsToCheck, "Battle/Treant/Stage 1/EyeDead", "Rising", 79, 9, 9);

        // Treant Stage 2
        addFrames(pathsToCheck, "Battle/Treant/Stage 2/Idle_Treant", "IdleTreant", 20, 4, 5);
        addFrames(pathsToCheck, "Battle/Treant/Stage 2/Attack_Treant", "TreantAttack", 41, 6, 7);
        addFrames(pathsToCheck, "Battle/Treant/Stage 2/Defeated_Treant", "TreantDefeated", 8, 3, 3);

        // Treant Stage 3
        addFrames(pathsToCheck, "Battle/Treant/Stage 3/Idle_Groot", "IdleGroot", 19, 4, 5);
        addFrames(pathsToCheck, "Battle/Treant/Stage 3/Attack_Groot", "GrootAttack", 48, 7, 7);
        addFrames(pathsToCheck, "Battle/Treant/Stage 3/Death_Groot", "DefeatedGroot", 72, 8, 9);

        // Player Deo animations
        // Idle
        String[] idleFileNames = {
            "Idle-1-1.png", "Idle-1-2.png", "Idle-1-3.png", "Idle-1-4.png",
            "Idle-2-1.png", "Idle-2-2.png", "Idle-2-3.png", "Idle-2-4.png",
            "Idle-3-1.png", "Idle-3-2.png", "Idle-3-3.png", "Idle-3-4.png",
            "Idle-4-1.png", "Idle-4-2.png", "Idle-4-3.png", "Idle-4-4.png",
            "Idle-5-1.png", "Idle-5-2.png", "Idle-5-3.png", "Idle-5-4.png",
            "Idle-6-1.png"
        };
        for (String name : idleFileNames) {
            pathsToCheck.add("Battle/Deo/Idle Ver. 1/Idle Ver. 1/" + name);
        }

        // Actions
        addFrames(pathsToCheck, "Battle/Deo/Punch/Punch", "Punch", 29, 8, 4);
        addFrames(pathsToCheck, "Battle/Deo/Hit/Hit", "Hit", 25, 7, 4);
        addFrames(pathsToCheck, "Battle/Deo/Dodge/Dodge", "Dodge", 29, 8, 4);
        addFrames(pathsToCheck, "Battle/Deo/Heal/Heal", "Heal", 29, 8, 4);

        // Verify all paths
        int missingCount = 0;
        for (String relPath : pathsToCheck) {
            File file = new File(assetsDir, relPath);
            if (!file.exists()) {
                System.err.println("MISSING ASSET: " + relPath + " (Expected at: " + file.getAbsolutePath() + ")");
                missingCount++;
            }
        }

        System.out.println("=== VERIFICATION COMPLETE ===");
        System.out.println("Total checked: " + pathsToCheck.size());
        System.out.println("Missing assets: " + missingCount);
        if (missingCount > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static void addFrames(List<String> paths, String basePath, String actionName, int totalFrames, int rows, int cols) {
        int count = 0;
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                if (count >= totalFrames)
                    break;
                String fileName = String.format("%s-%d-%d.png", actionName, r, c);
                paths.add(basePath + "/" + fileName);
                count++;
            }
        }
    }

    private static void addFramesSingle(List<String> paths, String basePath, String actionName, int totalFrames, String suffixChars) {
        for (int i = 0; i < totalFrames; i++) {
            char suffix = suffixChars.charAt(i);
            String fileName = String.format("%s-%c.png", actionName, suffix);
            paths.add(basePath + "/" + fileName);
        }
    }
}
