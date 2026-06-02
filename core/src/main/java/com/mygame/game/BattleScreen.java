package com.mygame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class BattleScreen implements Screen {

    private final TheLastAncestorsGame game;

    private static final int TREANT_ATTACK_HOLD_MS = 3400;
    private static final int TREANT_HIT_HOLD_MS = 1500;
    private static final int TREANT_SPAWN_HOLD_MS = 2400;
    private static final int ENEMY_ATTACK_UI_TAIL_MS = 550;

    private Player player;
    private Enemy enemy;

    private String currentMessage = "Sambutlah pertempuran terakhir!";
    private String currentSpeaker = "SISTEM";
    private boolean isDodging = false;
    private int menuState = 0; // 0: Main, 1: Attack Submenu

    // Textures
    private Texture backgroundImage;
    private Texture playerSprite;
    private Texture enemyPortraitStill;

    private TextureRegion[] playerIdleFrames;
    private int idleFrameIndex = 0;
    private int idleFrameTicks = 0;

    private TextureRegion[] playerAttackFrames;
    private TextureRegion[] playerHitFrames;
    private TextureRegion[] playerDodgeFrames;
    private TextureRegion[] playerHealFrames;
    private int playerAnimIndex = 0;
    private int playerAnimTicks = 0;

    private int playerState = 1; // 1: Idle, 2: Attack, 3: Hit, 4: Dodge, 5: Heal
    private Timer.Task playerStateRevertTask;

    private Texture battleIconAttack;
    private Texture battleIconInventory;
    private Texture battleIconRun;
    private Texture battleIconPunch;
    private Texture battleIconSlash;
    private Texture battleIconDodge;
    private Texture battleIconHeal;
    private Texture battleIconBack;
    private Texture dialogBoxImage;

    private boolean timelineActiveIsPlayer = true;

    private static final int DIALOGUE_PAD = 20;
    private static final int BATTLE_BTN_MAIN = 108;
    private static final int BATTLE_BTN_SUB = 92;

    // Treant Animated Assets
    private TextureRegion[] stage1Spawn;
    private TextureRegion[] stage1Idle;
    private TextureRegion[] stage1Attack;
    private TextureRegion[] stage1Dead;
    private TextureRegion[] stage2Idle;
    private TextureRegion[] stage2Attack;
    private TextureRegion[] stage2Dead;
    private TextureRegion[] stage3Idle;
    private TextureRegion[] stage3Attack;
    private TextureRegion[] stage3Dead;
    private int enemyAnimIndex = 0;
    private int enemyAnimTicks = 0;
    private boolean hideEnemySprite = false;
    private int blackFlashTicks = 0;
    private int enemyState = 1; // 0: Spawn, 1: Idle, 2: Attack, 3: Hit, 4: Death
    private int bossStage = 1; // 1, 2, or 3
    private Timer.Task enemyStateRevertTask;

    private int attackAnimTimer = 0;
    private int attackType = 0; // 0: none, 1: pulse, 2: pull

    private int introTimer = 100; // Decreases to 0 (Intro runs at start)
    private int victoryTimer = 0; // Increases to 100
    private int defeatTimer = 0; // Increases to 100
    private int fadeInTimer = 50; // Fade-in at start

    private int shakeTimer = 0;
    private int shakeMagnitude = 0;

    // Snapshot stats for defeat screen
    private int snapLevel, snapPlayerAtk, snapPlayerDef, snapPlayerMaxHp;
    private String snapEnemyName = "";
    private int snapEnemyAtk, snapEnemyDef, snapEnemyMaxHp;

    private Rectangle retryButtonRect = null;
    private int enemyOffsetY = 0;
    private boolean showDialogue = false;

    // Typewriter
    private int textCharIndex = 0;
    private float typewriterTimerAccumulator = 0;
    private boolean typewriterActive = false;

    // Floating text popups
    private String enemyDmgText = "";
    private int enemyDmgY = 0;
    private int enemyDmgTimer = 0;
    private Color enemyDmgColor = Color.WHITE;

    private String playerDmgText = "";
    private int playerDmgY = 0;
    private int playerDmgTimer = 0;
    private Color playerDmgColor = Color.WHITE;

    // Background Particles
    private float[][] particles = new float[20][3]; // x, y, size

    // LibGDX graphics variables
    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Fonts generated dynamically from PressStart2P.ttf
    private BitmapFont font8;
    private BitmapFont font10;
    private BitmapFont font11;
    private BitmapFont font13;
    private BitmapFont font14;
    private BitmapFont font20;
    private BitmapFont font32;
    private BitmapFont font60;
    private BitmapFont font80;

    private Array<Texture> allTextures = new Array<>();
    private boolean buttonsEnabled = true;

    // Dev Console (cheat terminal)
    private DevConsole devConsole;

    // Track click zones
    private Rectangle[] menuButtonRects;
    private BattleMenuIcon[] menuButtonKinds;

    private enum BattleMenuIcon {
        ATTACK, INVENTORY, RUN, PUNCH, SLASH, DODGE, HEAL, BACK
    }

    public BattleScreen(TheLastAncestorsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(1253, 832, camera);

        // Generate retro pixel fonts with beautiful black borders
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Battle/PressStart2P.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.borderWidth = 1.2f;
        parameter.borderColor = Color.BLACK;

        parameter.size = 8;
        font8 = generator.generateFont(parameter);
        font8.getData().markupEnabled = true;

        parameter.size = 10;
        font10 = generator.generateFont(parameter);
        font10.getData().markupEnabled = true;

        parameter.size = 11;
        font11 = generator.generateFont(parameter);
        font11.getData().markupEnabled = true;

        parameter.size = 13;
        font13 = generator.generateFont(parameter);
        font13.getData().markupEnabled = true;

        parameter.size = 14;
        font14 = generator.generateFont(parameter);
        font14.getData().markupEnabled = true;

        parameter.size = 20;
        font20 = generator.generateFont(parameter);
        font20.getData().markupEnabled = true;

        parameter.size = 32;
        font32 = generator.generateFont(parameter);
        font32.getData().markupEnabled = true;

        parameter.size = 60;
        font60 = generator.generateFont(parameter);
        font60.getData().markupEnabled = true;

        parameter.size = 80;
        font80 = generator.generateFont(parameter);
        font80.getData().markupEnabled = true;

        generator.dispose();

        // Load models
        player = new Player("Deo", 100, 25, 5);
        enemy = new Enemy("Treant", 150, 20, 10, "Boss");

        // Load textures
        backgroundImage = loadTexture("BattleSceneTestMap.png");
        playerSprite = loadTexture("player_knight.png");

        // Treant Spritesheets
        stage1Spawn = loadFramesSingle("Treant/Stage 1/EyeSpawn", "Spawn", 9, "123456789");
        stage1Idle = loadFramesSingle("Treant/Stage 1/EyeIdle", "Idle", 10, "123456789A");
        stage1Attack = loadFramesSingle("Treant/Stage 1/EyeAttack", "EyeAttack", 18, "123456789ABCDEFGHI");
        stage1Dead = loadFrames("Treant/Stage 1/EyeDead", "Rising", 79, 9, 9);

        stage2Idle = loadFrames("Treant/Stage 2/Idle_Treant", "IdleTreant", 20, 4, 5);
        stage2Attack = loadFrames("Treant/Stage 2/Attack_Treant", "TreantAttack", 41, 6, 7);
        stage2Dead = loadFrames("Treant/Stage 2/Defeated_Treant", "TreantDefeated", 8, 3, 3);

        stage3Idle = loadFrames("Treant/Stage 3/Idle_Groot", "IdleGroot", 19, 4, 5);
        stage3Attack = loadFrames("Treant/Stage 3/Attack_Groot", "GrootAttack", 48, 7, 7);
        stage3Dead = loadFrames("Treant/Stage 3/Death_Groot", "DefeatedGroot", 72, 8, 9);

        if (stage1Idle != null && stage1Idle.length > 0) {
            enemyPortraitStill = stage1Idle[0].getTexture();
        } else {
            enemyPortraitStill = loadTexture("Treant/Preview-Treant-Idle-Beardless.gif");
        }

        // Deo frames (Idle)
        playerIdleFrames = new TextureRegion[21];
        String[] idleFileNames = {
                "Idle-1-1.png", "Idle-1-2.png", "Idle-1-3.png", "Idle-1-4.png",
                "Idle-2-1.png", "Idle-2-2.png", "Idle-2-3.png", "Idle-2-4.png",
                "Idle-3-1.png", "Idle-3-2.png", "Idle-3-3.png", "Idle-3-4.png",
                "Idle-4-1.png", "Idle-4-2.png", "Idle-4-3.png", "Idle-4-4.png",
                "Idle-5-1.png", "Idle-5-2.png", "Idle-5-3.png", "Idle-5-4.png",
                "Idle-6-1.png"
        };
        for (int i = 0; i < 21; i++) {
            playerIdleFrames[i] = new TextureRegion(loadTexture("Deo/Idle Ver. 1/Idle Ver. 1/" + idleFileNames[i]));
        }

        // Deo action frames
        playerAttackFrames = loadFrames("Deo/Punch/Punch", "Punch", 29, 8, 4);
        playerHitFrames = loadFrames("Deo/Hit/Hit", "Hit", 25, 7, 4);
        playerDodgeFrames = loadFrames("Deo/Dodge/Dodge", "Dodge", 29, 8, 4);
        playerHealFrames = loadFrames("Deo/Heal/Heal", "Heal", 29, 8, 4);

        // Icons
        battleIconAttack = loadTexture("Menu/Attack_Icon.png");
        battleIconInventory = loadTexture("Menu/Inventory_Icon.png");
        battleIconRun = loadTexture("Menu/Run_Icon.png");
        battleIconPunch = loadTexture("Skill/Punch_Icon.png");
        battleIconSlash = loadTexture("Skill/Slash_Icon.png");
        battleIconDodge = loadTexture("Skill/Evade_Icon.png");
        battleIconHeal = loadTexture("Skill/Heal_Icon.png");
        battleIconBack = loadTexture("Menu/Back_Icon.png");
        dialogBoxImage = loadTexture("Menu/Dialog_Box.png");

        // Initialize particles
        for (int i = 0; i < particles.length; i++) {
            particles[i][0] = (float) (Math.random() * 1253);
            particles[i][1] = (float) (Math.random() * 832);
            particles[i][2] = (float) (Math.random() * 5) + 2;
        }

        setupMainMenuClickZones();

        // Inisialisasi DevConsole dan pasang InputProcessor untuk keyTyped
        devConsole = DevConsole.getInstance();
        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                return devConsole.keyTyped(character);
            }
        });

        // Initial spawn sequence
        hideEnemySprite = false;
        blackFlashTicks = 0;
        changeEnemyState(0);
        toggleButtons(false);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                changeEnemyState(1);
                toggleButtons(true);
            }
        }, TREANT_SPAWN_HOLD_MS / 1000f);
    }

    private Texture loadTexture(String path) {
        Texture tex = new Texture(Gdx.files.internal("Battle/" + path));
        allTextures.add(tex);
        return tex;
    }

    private TextureRegion[] loadFrames(String basePath, String actionName, int totalFrames, int rows, int cols) {
        TextureRegion[] frames = new TextureRegion[totalFrames];
        int count = 0;
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                if (count >= totalFrames)
                    break;
                String fileName = String.format("%s-%d-%d.png", actionName, r, c);
                Texture tex = loadTexture(basePath + "/" + fileName);
                frames[count++] = new TextureRegion(tex);
            }
        }
        return frames;
    }

    private TextureRegion[] loadFramesSingle(String basePath, String actionName, int totalFrames, String suffixChars) {
        TextureRegion[] frames = new TextureRegion[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            char suffix = suffixChars.charAt(i);
            String fileName = String.format("%s-%c.png", actionName, suffix);
            Texture tex = loadTexture(basePath + "/" + fileName);
            frames[i] = new TextureRegion(tex);
        }
        return frames;
    }

    private void setupMainMenuClickZones() {
        menuState = 0;
        // Total width: 3 * 108 + 2 * 28 = 380
        float startX = (1253 - 380) / 2f;
        float y = 712f;
        float dim = BATTLE_BTN_MAIN;

        menuButtonRects = new Rectangle[3];
        menuButtonKinds = new BattleMenuIcon[3];

        menuButtonRects[0] = new Rectangle(startX, y, dim, dim);
        menuButtonKinds[0] = BattleMenuIcon.ATTACK;

        menuButtonRects[1] = new Rectangle(startX + dim + 28, y, dim, dim);
        menuButtonKinds[1] = BattleMenuIcon.INVENTORY;

        menuButtonRects[2] = new Rectangle(startX + 2 * (dim + 28), y, dim, dim);
        menuButtonKinds[2] = BattleMenuIcon.RUN;
    }

    private void setupAttackSubMenuClickZones() {
        menuState = 1;
        // Total width: 5 * 92 + 4 * 28 = 572
        float startX = (1253 - 572) / 2f;
        float y = 720f;
        float dim = BATTLE_BTN_SUB;

        menuButtonRects = new Rectangle[5];
        menuButtonKinds = new BattleMenuIcon[5];

        menuButtonRects[0] = new Rectangle(startX, y, dim, dim);
        menuButtonKinds[0] = BattleMenuIcon.PUNCH;

        menuButtonRects[1] = new Rectangle(startX + dim + 28, y, dim, dim);
        menuButtonKinds[1] = BattleMenuIcon.SLASH;

        menuButtonRects[2] = new Rectangle(startX + 2 * (dim + 28), y, dim, dim);
        menuButtonKinds[2] = BattleMenuIcon.DODGE;

        menuButtonRects[3] = new Rectangle(startX + 3 * (dim + 28), y, dim, dim);
        menuButtonKinds[3] = BattleMenuIcon.HEAL;

        menuButtonRects[4] = new Rectangle(startX + 4 * (dim + 28), y, dim, dim);
        menuButtonKinds[4] = BattleMenuIcon.BACK;
    }

    @Override
    public void render(float delta) {
        // ── Update & konsumsi cheat DevConsole ────────────────────────
        devConsole.update(delta);
        devConsole.handleInput();

        if (devConsole.consumeReload()) {
            game.setScreen(new BattleScreen(game));
            this.dispose();
            return;
        }

        // /heal — pulihkan HP & EN pemain ke penuh
        if (devConsole.consumeHeal()) {
            player.hp = player.getMaxHp();
            player.setMp(player.getMaxMp());
            showPopupText(false, "+FULL HP", Color.GREEN);
        }

        // /god — pemain tidak dapat menerima damage (diterapkan lewat override defend nanti)
        // Flag godMode dibaca di enemyTurn saat damage akan diterapkan.

        // /killboss — langsung set HP boss ke 0 dan trigger checkBattleEnd
        if (devConsole.consumeKillBoss()) {
            enemy.hp = 0;
            checkBattleEnd();
        }

        // /setstage — pindah ke stage boss tertentu
        if (devConsole.consumeSetStage()) {
            int targetStage = devConsole.getRequestedStage();
            if (targetStage != bossStage) {
                bossStage = targetStage;
                enemy.hp = enemy.getMaxHp();
                changeEnemyState(1);
                showMessage("SISTEM", "[DEV] Boss stage diset ke " + targetStage);
            }
        }

        updateLogic(delta);

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        camera.update();
        spriteBatch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Apply screen shake
        if (shakeTimer > 0) {
            float dx = (float) (Math.random() * shakeMagnitude * 2) - shakeMagnitude;
            float dy = (float) (Math.random() * shakeMagnitude * 2) - shakeMagnitude;
            camera.translate(dx, dy);
            camera.update();
            spriteBatch.setProjectionMatrix(camera.combined);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shakeTimer--;
        }

        // --- 1. BACKGROUND ---
        spriteBatch.begin();
        if (backgroundImage != null) {
            spriteBatch.draw(backgroundImage, 0, fy(0, 832), 1253, 832);
        }
        spriteBatch.end();

        // Background Particles
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 191f / 255f, 1f, 150f / 255f));
        for (int i = 0; i < particles.length; i++) {
            shapeRenderer.rect(particles[i][0], fy(particles[i][1], particles[i][2]), particles[i][2], particles[i][2]);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- 2. AURA (Behind Boss) ---
        float spriteSize = 360f;
        float ex = 1253f * 0.74f - spriteSize / 2f;
        float ey = 832f * 0.50f - spriteSize;

        // Apply details to ex/ey based on bossState
        if (bossStage == 1) {
            if (enemyState == 0) {
                ex += 10;
                ey += -75;
            } else if (enemyState == 1 || enemyState == 3 || enemyState == 2 || enemyState == 4) {
                ex += 10;
                ey += 105;
            }
        } else if (bossStage == 2) {
            ex -= 15;
            ey -= 10;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (bossStage == 2 && enemy.isAlive()) {
            shapeRenderer.setColor(1f, 60f / 255f, 0f, 80f / 255f);
            shapeRenderer.ellipse(ex + spriteSize / 6f, fy(ey + spriteSize / 6f + enemyOffsetY, spriteSize * 0.67f),
                    spriteSize * 0.67f, spriteSize * 0.67f);
        } else if (bossStage == 3 && enemy.isAlive()) {
            shapeRenderer.setColor(160f / 255f, 0f, 1f, 90f / 255f);
            shapeRenderer.ellipse(ex + spriteSize / 6f, fy(ey + spriteSize / 6f + enemyOffsetY, spriteSize * 0.67f),
                    spriteSize * 0.67f, spriteSize * 0.67f);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- 3. CHARACTERS ---
        spriteBatch.begin();

        // Player
        if (player.isAlive()) {
            TextureRegion currentPlayerImg = null;
            if (playerState == 1 && playerIdleFrames != null && playerIdleFrames.length > 0) {
                currentPlayerImg = playerIdleFrames[idleFrameIndex];
            } else if (playerState == 2 && playerAttackFrames != null) {
                currentPlayerImg = playerAttackFrames[Math.min(playerAnimIndex, playerAttackFrames.length - 1)];
            } else if (playerState == 3 && playerHitFrames != null) {
                currentPlayerImg = playerHitFrames[Math.min(playerAnimIndex, playerHitFrames.length - 1)];
            } else if (playerState == 4 && playerDodgeFrames != null) {
                currentPlayerImg = playerDodgeFrames[Math.min(playerAnimIndex, playerDodgeFrames.length - 1)];
            } else if (playerState == 5 && playerHealFrames != null) {
                currentPlayerImg = playerHealFrames[Math.min(playerAnimIndex, playerHealFrames.length - 1)];
            }

            float pSize = 360f;
            float playerScaleMultiplier = 0.85f;
            float pWidth = pSize * playerScaleMultiplier;
            float pHeight = pSize * playerScaleMultiplier;
            if (currentPlayerImg != null) {
                float w = currentPlayerImg.getRegionWidth();
                float h = currentPlayerImg.getRegionHeight();
                if (w > 0 && h > 0) {
                    pHeight = pSize * playerScaleMultiplier;
                    pWidth = pSize * playerScaleMultiplier * (w / h);
                }
            }

            float px = 1253f * 0.27f - pWidth / 2f;
            float py = 832f * 0.75f - pHeight;

            if (currentPlayerImg != null) {
                spriteBatch.draw(currentPlayerImg, px, fy(py, pHeight), pWidth, pHeight);
            } else {
                spriteBatch.draw(playerSprite, px, fy(py, pHeight), pWidth, pHeight);
            }
        }

        // Enemy (Boss)
        boolean drawSprite = !hideEnemySprite;
        if (bossStage == 3 && enemyState == 4 && enemyAnimIndex >= 50 && enemyAnimIndex % 2 == 0) {
            drawSprite = false;
        }

        if (drawSprite) {
            TextureRegion[] currentEnemyFrames = null;
            if (bossStage == 1) {
                switch (enemyState) {
                    case 0:
                        currentEnemyFrames = stage1Spawn;
                        break;
                    case 1:
                    case 3:
                        currentEnemyFrames = stage1Idle;
                        break;
                    case 2:
                        currentEnemyFrames = stage1Attack;
                        break;
                    case 4:
                        currentEnemyFrames = stage1Dead;
                        break;
                }
            } else if (bossStage == 2) {
                switch (enemyState) {
                    case 1:
                    case 3:
                        currentEnemyFrames = stage2Idle;
                        break;
                    case 2:
                        currentEnemyFrames = stage2Attack;
                        break;
                    case 4:
                        currentEnemyFrames = stage2Dead;
                        break;
                }
            } else if (bossStage == 3) {
                switch (enemyState) {
                    case 1:
                    case 3:
                        currentEnemyFrames = stage3Idle;
                        break;
                    case 2:
                        currentEnemyFrames = stage3Attack;
                        break;
                    case 4:
                        currentEnemyFrames = stage3Dead;
                        break;
                }
            }

            TextureRegion currentEnemyImg = null;
            if (currentEnemyFrames != null && currentEnemyFrames.length > 0) {
                int frameIdx = Math.min(enemyAnimIndex, currentEnemyFrames.length - 1);
                currentEnemyImg = currentEnemyFrames[frameIdx];
            }

            float drawY = ey + enemyOffsetY;
            if (enemyState == 0) {
                drawY = ey + (spriteSize / 2f);
            }

            if (currentEnemyImg != null) {
                spriteBatch.draw(currentEnemyImg, ex, fy(drawY, spriteSize), spriteSize, spriteSize);
            } else if (enemyPortraitStill != null) {
                spriteBatch.draw(enemyPortraitStill, ex, fy(drawY, spriteSize), spriteSize, spriteSize);
            }
        }
        spriteBatch.end();

        // --- 4. HUD / PANELS ---
        if (enemy.isAlive() || enemyState == 4) {
            drawBossHpBarVertical();
        }

        drawActionOrderPanel();
        drawPlayerStatusPanel();

        if (showDialogue) {
            drawDialogueBox();
        } else if (buttonsEnabled && defeatTimer <= 0 && victoryTimer <= 0) {
            drawMenuButtons();
        }

        // --- 5. FLOATING POPUP TEXTS ---
        spriteBatch.begin();
        if (enemyDmgTimer > 0) {
            String fmt = formatStringWithMarkup(enemyDmgText);
            font32.draw(spriteBatch, fmt, ex + spriteSize / 2f - 40, fy(ey + enemyDmgY + enemyOffsetY, 0));
        }

        if (playerDmgTimer > 0) {
            String fmt = formatStringWithMarkup(playerDmgText);
            float px = 1253f * 0.27f;
            font32.draw(spriteBatch, fmt, px - 40, fy(832f * 0.75f - 180 + playerDmgY, 0));
        }
        spriteBatch.end();

        // --- 6. CINEMATIC OVERLAYS ---
        drawCinematicIntro();
        drawVictoryScreen();
        drawDefeatScreen();

        // Overlay Flashes
        if (blackFlashTicks > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 1f);
            shapeRenderer.rect(0, 0, 1253, 832);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (fadeInTimer > 0) {
            float alpha = (float) fadeInTimer / 50.0f;
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, Math.max(0.0f, Math.min(1.0f, alpha)));
            shapeRenderer.rect(0, 0, 1253, 832);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Restore camera
        if (shakeTimer > 0) {
            camera.setToOrtho(false, 1253, 832);
        }

        // Check input/clicks
        handleClicks();

        // ── Render DevConsole UI di screen-space (selalu di lapisan paling atas) ──────
        renderBattleConsole();
    }

    /** Render DevConsole di atas semua elemen, dalam koordinat layar. */
    private void renderBattleConsole() {
        if (devConsole == null) return;
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        com.badlogic.gdx.math.Matrix4 screenProj = new com.badlogic.gdx.math.Matrix4();
        screenProj.setToOrtho2D(0, 0, sw, sh);

        spriteBatch.setProjectionMatrix(screenProj);
        shapeRenderer.setProjectionMatrix(screenProj);

        // Gunakan font terkecil yang tersedia (font8)
        devConsole.render(spriteBatch, shapeRenderer, font8, sw, sh);

        // Kembalikan projection ke viewport normal
        camera.update();
        spriteBatch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);
    }

    private float fy(float swingY, float h) {
        return 832f - swingY - h;
    }

    private void updateLogic(float delta) {
        // Particles
        for (int i = 0; i < particles.length; i++) {
            particles[i][1] -= 2f * 30f * delta;
            if (particles[i][1] < 0) {
                particles[i][1] = 832;
                particles[i][0] = (float) (Math.random() * 1253);
            }
        }

        // Player animations
        if (playerState == 1) {
            idleFrameTicks++;
            if (idleFrameTicks >= 3) {
                idleFrameTicks = 0;
                if (playerIdleFrames != null && playerIdleFrames.length > 0) {
                    idleFrameIndex = (idleFrameIndex + 1) % playerIdleFrames.length;
                }
            }
        } else {
            int ticksPerFrame = 3;
            switch (playerState) {
                case 2:
                    ticksPerFrame = 3;
                    break;
                case 3:
                    ticksPerFrame = 1;
                    break;
                case 4:
                    ticksPerFrame = 1;
                    break;
                case 5:
                    ticksPerFrame = 2;
                    break;
            }
            playerAnimTicks++;
            if (playerAnimTicks >= ticksPerFrame) {
                playerAnimTicks = 0;
                playerAnimIndex++;
            }
        }

        // Enemy animations
        if (blackFlashTicks > 0) {
            blackFlashTicks--;
            if (blackFlashTicks == 0) {
                hideEnemySprite = true;
            }
        }

        int enemyTicksPerFrame = 3;
        boolean loop = false;
        TextureRegion[] currentEnemyFrames = null;

        if (bossStage == 1) {
            switch (enemyState) {
                case 0:
                    currentEnemyFrames = stage1Spawn;
                    enemyTicksPerFrame = 8;
                    loop = false;
                    break;
                case 1:
                case 3:
                    currentEnemyFrames = stage1Idle;
                    enemyTicksPerFrame = 3;
                    loop = true;
                    break;
                case 2:
                    currentEnemyFrames = stage1Attack;
                    enemyTicksPerFrame = 5;
                    loop = false;
                    break;
                case 4:
                    currentEnemyFrames = stage1Dead;
                    enemyTicksPerFrame = 1;
                    loop = false;
                    break;
            }
        } else if (bossStage == 2) {
            switch (enemyState) {
                case 1:
                case 3:
                    currentEnemyFrames = stage2Idle;
                    enemyTicksPerFrame = 3;
                    loop = true;
                    break;
                case 2:
                    currentEnemyFrames = stage2Attack;
                    enemyTicksPerFrame = 2;
                    loop = false;
                    break;
                case 4:
                    currentEnemyFrames = stage2Dead;
                    enemyTicksPerFrame = 3;
                    loop = false;
                    break;
            }
        } else if (bossStage == 3) {
            switch (enemyState) {
                case 1:
                case 3:
                    currentEnemyFrames = stage3Idle;
                    enemyTicksPerFrame = 3;
                    loop = true;
                    break;
                case 2:
                    currentEnemyFrames = stage3Attack;
                    enemyTicksPerFrame = 2;
                    loop = false;
                    break;
                case 4:
                    currentEnemyFrames = stage3Dead;
                    enemyTicksPerFrame = 1;
                    loop = false;
                    break;
            }
        }

        if (currentEnemyFrames != null && currentEnemyFrames.length > 0) {
            enemyAnimTicks++;
            if (enemyAnimTicks >= enemyTicksPerFrame) {
                enemyAnimTicks = 0;
                if (loop) {
                    enemyAnimIndex = (enemyAnimIndex + 1) % currentEnemyFrames.length;
                } else {
                    if (enemyAnimIndex < currentEnemyFrames.length - 1) {
                        enemyAnimIndex++;
                    }
                }
            }
        }

        // Timers update
        if (fadeInTimer > 0)
            fadeInTimer--;
        if (attackAnimTimer > 0) {
            if (attackType == 2) {
                enemyOffsetY = (int) (Math.sin(System.currentTimeMillis() / 100.0) * 20);
            }
            attackAnimTimer--;
        } else {
            enemyOffsetY = 0;
            attackType = 0;
        }

        // Damage text float
        if (enemyDmgTimer > 0) {
            enemyDmgY -= 1;
            enemyDmgTimer--;
        }
        if (playerDmgTimer > 0) {
            playerDmgY -= 1;
            playerDmgTimer--;
        }

        // Typewriter accumulator
        if (typewriterActive) {
            typewriterTimerAccumulator += delta;
            if (typewriterTimerAccumulator >= 0.030f) {
                typewriterTimerAccumulator = 0;
                if (textCharIndex < currentMessage.length()) {
                    textCharIndex++;
                } else {
                    typewriterActive = false;
                }
            }
        }
    }

    private void handleClicks() {
        if (!Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT))
            return;

        Vector3 rawMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(rawMouse);

        float mx = rawMouse.x;
        float my = 832 - rawMouse.y;

        // Defeat Retry Button Click
        if (defeatTimer > 50 && retryButtonRect != null) {
            if (retryButtonRect.contains(mx, my)) {
                resetBattle();
                return;
            }
        }

        // Click Dialogue advanced
        if (showDialogue && timelineActiveIsPlayer) {
            showDialogue = false;
            updateStatus();
            return;
        }

        // Click Menu Button Action
        if (buttonsEnabled && defeatTimer <= 0 && victoryTimer <= 0 && !showDialogue) {
            for (int i = 0; i < menuButtonRects.length; i++) {
                if (menuButtonRects[i].contains(mx, my)) {
                    triggerButtonAction(menuButtonKinds[i]);
                    return;
                }
            }
        }
    }

    private void triggerButtonAction(BattleMenuIcon kind) {
        switch (kind) {
            case ATTACK:
                setupAttackSubMenuClickZones();
                break;
            case INVENTORY:
                showMessage("SISTEM", "Inventori kosong. Tunggu update dari Rangga!");
                break;
            case RUN:
                attemptEscape();
                break;
            case PUNCH:
                executeAction("Punch");
                setupMainMenuClickZones();
                break;
            case SLASH:
                executeAction("Slash");
                setupMainMenuClickZones();
                break;
            case DODGE:
                executeAction("Dodge");
                setupMainMenuClickZones();
                break;
            case HEAL:
                executeAction("Heal");
                setupMainMenuClickZones();
                break;
            case BACK:
                setupMainMenuClickZones();
                break;
        }
    }

    public void showMessage(String speaker, String message) {
        this.showDialogue = true;
        this.currentSpeaker = speaker;
        this.currentMessage = message;
        this.textCharIndex = 0;
        this.typewriterActive = true;
        this.typewriterTimerAccumulator = 0f;
    }

    public void showPopupText(boolean isEnemy, String text, Color c) {
        if (isEnemy) {
            enemyDmgText = text;
            enemyDmgY = 0;
            enemyDmgTimer = 75;
            enemyDmgColor = c;
        } else {
            playerDmgText = text;
            playerDmgY = 0;
            playerDmgTimer = 75;
            playerDmgColor = c;
        }
    }

    public void triggerScreenShake(int duration, int magnitude) {
        this.shakeTimer = duration;
        this.shakeMagnitude = magnitude;
    }

    public void setPlayerTempState(int state, int durationMs) {
        cancelPlayerStateRevertTimer();
        playerAnimIndex = 0;
        playerAnimTicks = 0;
        playerState = state;

        playerStateRevertTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                playerStateRevertTask = null;
                playerState = 1;
            }
        }, durationMs / 1000f);
    }

    private void cancelPlayerStateRevertTimer() {
        if (playerStateRevertTask != null) {
            playerStateRevertTask.cancel();
            playerStateRevertTask = null;
        }
    }

    private void changeEnemyState(int state) {
        enemyState = state;
        enemyAnimIndex = 0;
        enemyAnimTicks = 0;
    }

    private void setEnemyTempState(int state, int durationMs) {
        cancelEnemyStateRevertTimer();
        changeEnemyState(state);

        enemyStateRevertTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                enemyStateRevertTask = null;
                if (enemy.isAlive() && enemyState != 4) {
                    changeEnemyState(1);
                }
            }
        }, durationMs / 1000f);
    }

    private void cancelEnemyStateRevertTimer() {
        if (enemyStateRevertTask != null) {
            enemyStateRevertTask.cancel();
            enemyStateRevertTask = null;
        }
    }

    private void updateStatus() {
    }

    private void toggleButtons(boolean state) {
        buttonsEnabled = state;
        timelineActiveIsPlayer = state;
    }

    private void executeAction(String action) {
        if (!player.isAlive() || !enemy.isAlive())
            return;

        int cost = 0;
        int recharge = 0;
        switch (action) {
            case "Punch":
                cost = 8;
                break;
            case "Slash":
                cost = 1;
                break;
            case "Dodge":
                cost = 7;
                break;
            case "Heal":
                cost = 5;
                recharge = (int) Math.round(player.getMp() * 0.25);
                break;
        }

        if (player.getMp() < cost) {
            showMessage("SISTEM", "Energi tidak cukup! Butuh " + cost + " EN.");
            setupAttackSubMenuClickZones();
            return;
        }

        player.setMp(player.getMp() - cost + recharge);
        toggleButtons(false);
        isDodging = false;
        showDialogue = false;

        switch (action) {
            case "Punch": {
                double rng = Math.random();
                int damage = (rng < 0.70) ? 15 + (int) (Math.random() * 11) : 26 + (int) (Math.random() * 15);
                showMessage(player.getName(), "Menyerang " + enemy.getName() + " dengan pukulan tinju!");
                enemy.defend(damage, game, 1000);
                startAnimation(1);
                setEnemyTempState(3, TREANT_HIT_HOLD_MS);
                setPlayerTempState(2, 2900);
                break;
            }
            case "Slash": {
                double rng = Math.random();
                int damage;
                if (rng < 0.48) {
                    damage = 1000 + (int) (Math.random() * 6);
                } else if (rng < 0.80) {
                    damage = 1000 + (int) (Math.random() * 7);
                } else {
                    damage = 1000 + (int) (Math.random() * 21);
                }
                showMessage(player.getName(), "Menyerang " + enemy.getName() + " dengan tebasan pedang tebal!");
                enemy.defend(damage, game, 1800);
                startAnimation(1);
                setEnemyTempState(3, TREANT_HIT_HOLD_MS);
                setPlayerTempState(2, 2900);
                break;
            }
            case "Dodge":
                isDodging = true;
                showMessage(player.getName(), "🛡️ Memasang posisi bertahan! (50% Peluang Hindar)");
                break;
            case "Heal": {
                int randPercent = 20 + (int) (Math.random() * 21);
                int healAmount = (int) Math.round(player.getMaxHp() * (randPercent / 100.0));
                player.heal(healAmount, recharge, game);
                setPlayerTempState(5, 2000);
                break;
            }
        }

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                showDialogue = true;
                updateStatus();

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        if (enemy.isAlive()) {
                            enemyTurn();
                        } else {
                            checkBattleEnd();
                        }
                    }
                }, 1.5f);
            }
        }, 1.0f);
    }

    private void checkBattleEnd() {
        if (!enemy.isAlive()) {
            cancelEnemyStateRevertTimer();
            if (bossStage == 1) {
                changeEnemyState(4);
                toggleButtons(false);
                showMessage("SISTEM", "⚠️ " + enemy.getName() + " telah dikalahkan! Tapi ia berubah wujud...");

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        bossStage = 2;
                        enemy.hp = enemy.getMaxHp();
                        enemy.attack = (int) (enemy.attack * 1.25);
                        enemy.defense = (int) (enemy.defense * 1.25);
                        changeEnemyState(1);
                        showMessage("SISTEM", "⚠️ " + enemy.getName() + " bangkit kembali sebagai Treant! (Tahap 2/3)");
                        triggerScreenShake(30, 35);
                        toggleButtons(true);
                        updateStatus();
                    }
                }, 2.600f);

            } else if (bossStage == 2) {
                changeEnemyState(4);
                toggleButtons(false);
                showMessage("SISTEM", "⚠️ " + enemy.getName() + " telah dikalahkan! Tapi ia tumbuh lebih kuat...");

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        bossStage = 3;
                        enemy.hp = enemy.getMaxHp();
                        enemy.attack = (int) (enemy.attack * 1.25);
                        enemy.defense = (int) (enemy.defense * 1.25);
                        changeEnemyState(1);
                        showMessage("SISTEM", "⚠️ " + enemy.getName() + " bangkit kembali sebagai Groot! (Tahap 3/3)");
                        triggerScreenShake(30, 35);
                        toggleButtons(true);
                        updateStatus();
                    }
                }, 0.800f);

            } else {
                changeEnemyState(4);
                toggleButtons(false);
                showMessage("SISTEM", enemy.getName() + " terpojok dan akan meledak!");

                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        blackFlashTicks = 15;
                        victoryTimer = 1;
                        showMessage("SISTEM", enemy.getName() + " telah hancur meledak!");
                        player.gainExp(50, game);

                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                game.setScreen(new ExplorationScreen(game));
                            }
                        }, 3.0f);
                    }
                }, 2.400f);
            }
        } else {
            toggleButtons(false);
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    enemyTurn();
                }
            }, 1.5f);
        }
    }

    private void attemptEscape() {
        if (Math.random() < 0.5) {
            showMessage("SISTEM", "🏃 Berhasil melarikan diri! Kembali ke eksplorasi...");
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    game.setScreen(new ExplorationScreen(game));
                }
            }, 1.5f);
        } else {
            showMessage("SISTEM", "❌ Gagal melarikan diri! Treant menghalangimu!");
            toggleButtons(false);
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    enemyTurn();
                }
            }, 1.5f);
        }
    }

    private void enemyTurn() {
        timelineActiveIsPlayer = false;
        showDialogue = false;
        final float endPhaseSec;

        if (isDodging && Math.random() < 0.5) {
            showMessage(enemy.getName(), "💨 Menyerang! Tapi Deo berhasil menghindar!");
            setPlayerTempState(4, 1500);
            isDodging = false;
            endPhaseSec = 2.0f;
        } else {
            setEnemyTempState(2, TREANT_ATTACK_HOLD_MS);
            enemy.attack(player, game);
            isDodging = false;
            endPhaseSec = (TREANT_ATTACK_HOLD_MS + ENEMY_ATTACK_UI_TAIL_MS) / 1000f;
        }

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                showDialogue = true;
                if (!player.isAlive()) {
                    showMessage("SISTEM", player.getName() + " gugur! GAME OVER.");
                    snapLevel = player.getLevel();
                    snapPlayerAtk = player.attack;
                    snapPlayerDef = player.defense;
                    snapPlayerMaxHp = player.getMaxHp();
                    snapEnemyName = enemy.getName();
                    snapEnemyAtk = enemy.attack;
                    snapEnemyDef = enemy.defense;
                    snapEnemyMaxHp = enemy.getMaxHp();

                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            defeatTimer = 1;
                        }
                    }, 1.8f);
                } else {
                    int regen = (int) (player.getMaxMp() * 0.1);
                    player.setMp(player.getMp() + regen);
                    showMessage("SISTEM", "Deo meregenerasi " + regen + " EN!");
                    toggleButtons(true);
                }
            }
        }, endPhaseSec);
    }

    private void resetBattle() {
        player = new Player("Deo", 100, 25, 5);
        enemy = new Enemy("Treant", 150, 20, 10, "Boss");

        defeatTimer = 0;
        victoryTimer = 0;
        bossStage = 1;
        retryButtonRect = null;
        showDialogue = true;
        timelineActiveIsPlayer = true;
        isDodging = false;
        attackAnimTimer = 0;
        attackType = 0;
        enemyOffsetY = 0;
        cancelPlayerStateRevertTimer();
        playerState = 1;

        hideEnemySprite = false;
        blackFlashTicks = 0;
        changeEnemyState(0);

        showMessage("SISTEM", "🔄 Pertempuran dimulai ulang!");
        toggleButtons(false);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                changeEnemyState(1);
                toggleButtons(true);
                updateStatus();
            }
        }, TREANT_SPAWN_HOLD_MS / 1000f);
    }

    private void startAnimation(int type) {
        attackType = type;
        attackAnimTimer = 30;
    }

    // --- RENDER HELPERS ---

    private void drawBossHpBarVertical() {
        float px = 1253f - 20f - 45f;
        float py = 20f;
        float pw = 45f;
        float ph = 350f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(28f / 255f, 10f / 255f, 10f / 255f, 0.88f);
        shapeRenderer.rect(px, fy(py, ph), pw, ph);

        // HP Bar fill
        double pct = enemy.getMaxHp() > 0 ? (double) Math.max(0, enemy.getHp()) / enemy.getMaxHp() : 0;
        Color hpFill = pct > 0.35f ? new Color(210f / 255f, 55f / 255f, 55f / 255f, 1f)
                : new Color(255f / 255f, 90f / 255f, 60f / 255f, 1f);
        float bx1 = px + 10f;
        float barY = py + 32f;
        float barH = ph - 42f;
        float barW1 = 14f;

        shapeRenderer.setColor(22f / 255f, 22f / 255f, 32f / 255f, 1f);
        shapeRenderer.rect(bx1, fy(barY, barH), barW1, barH);
        shapeRenderer.setColor(hpFill);
        float innerH = (float) (barH - 4f);
        float innerFilledH = (float) (innerH * pct);
        shapeRenderer.rect(bx1 + 2f, fy(barY + (innerH - innerFilledH) + 2f, innerFilledH), barW1 - 4f, innerFilledH);

        // Stage/Lives split blocks
        float bx2 = bx1 + barW1 + 3f;
        float barW2 = 8f;
        int lives = 4 - bossStage;
        if (!enemy.isAlive() && bossStage == 3)
            lives = 0;

        float blockH = (barH - 8f) / 3f;
        for (int i = 0; i < 3; i++) {
            float by = barY + i * (blockH + 4f);
            shapeRenderer.setColor(22f / 255f, 22f / 255f, 32f / 255f, 1f);
            shapeRenderer.rect(bx2, fy(by, blockH), barW2, blockH);

            boolean isActive = (i == 0 && lives >= 3) || (i == 1 && lives >= 2) || (i == 2 && lives >= 1);
            if (isActive) {
                shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f, 1f);
                shapeRenderer.rect(bx2 + 2f, fy(by + 2f, blockH - 4f), barW2 - 4f, blockH - 4f);
            }
        }
        shapeRenderer.end();

        // HP vertical outlines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(255f / 255f, 120f / 255f, 80f / 255f, 1f);
        shapeRenderer.rect(px, fy(py, ph), pw, ph);
        shapeRenderer.setColor(230f / 255f, 55f / 255f, 55f / 255f, 1f);
        shapeRenderer.rect(px + 3f, fy(py + 3f, ph - 6f), pw - 6f, ph - 6f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(bx1, fy(barY, barH), barW1, barH);
        for (int i = 0; i < 3; i++) {
            float by = barY + i * (blockH + 4f);
            boolean isActive = (i == 0 && lives >= 3) || (i == 1 && lives >= 2) || (i == 2 && lives >= 1);
            shapeRenderer.setColor(isActive ? new Color(255f / 255f, 240f / 255f, 150f / 255f, 1f)
                    : new Color(80f / 255f, 70f / 255f, 50f / 255f, 1f));
            shapeRenderer.rect(bx2, fy(by, blockH), barW2, blockH);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Texts
        spriteBatch.begin();
        String line1 = "B:S" + bossStage;
        String line2 = enemy.getName().toUpperCase();
        if (line2.length() > 6)
            line2 = line2.substring(0, 6);

        font8.draw(spriteBatch, line1, px + 5, fy(py + 10, 0));
        font8.draw(spriteBatch, line2, px + 5, fy(py + 22, 0));

        // HP vertical text label
        font8.draw(spriteBatch, "HP", bx1 - 1, fy(barY + barH / 2f - 10, 0));
        font8.draw(spriteBatch, String.valueOf(enemy.getHp()), bx1 - 1, fy(barY + barH / 2f + 5, 0));
        spriteBatch.end();
    }

    private void drawPlayerStatusPanel() {
        float px = 20f;
        float py = 20f;
        float pw = 52f;
        float ph = 350f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(10f / 255f, 14f / 255f, 28f / 255f, 0.88f);
        shapeRenderer.rect(px, fy(py, ph), pw, ph);

        // HP Bar Fill
        float bx1 = px + 10f;
        float barY = py + 32f;
        float barH = ph - 42f;
        float barW = 14f;
        double hpPct = player.getMaxHp() > 0 ? (double) player.getHp() / player.getMaxHp() : 0;
        Color hpCol = hpPct > 0.45 ? new Color(55f / 255f, 200f / 255f, 95f / 255f, 1f)
                : (hpPct > 0.2 ? new Color(230f / 255f, 200f / 255f, 60f / 255f, 1f)
                        : new Color(230f / 255f, 70f / 255f, 70f / 255f, 1f));

        shapeRenderer.setColor(22f / 255f, 22f / 255f, 32f / 255f, 1f);
        shapeRenderer.rect(bx1, fy(barY, barH), barW, barH);
        shapeRenderer.setColor(hpCol);
        float innerH = barH - 4f;
        float hpFilledH = (float) (innerH * hpPct);
        shapeRenderer.rect(bx1 + 2f, fy(barY + (innerH - hpFilledH) + 2f, hpFilledH), barW - 4f, hpFilledH);

        // MP Bar Fill
        float bx2 = bx1 + barW + 4f;
        double mpPct = player.getMaxMp() > 0 ? (double) player.getMp() / player.getMaxMp() : 0;

        shapeRenderer.setColor(22f / 255f, 22f / 255f, 32f / 255f, 1f);
        shapeRenderer.rect(bx2, fy(barY, barH), barW, barH);
        shapeRenderer.setColor(70f / 255f, 140f / 255f, 240f / 255f, 1f);
        float mpFilledH = (float) (innerH * mpPct);
        shapeRenderer.rect(bx2 + 2f, fy(barY + (innerH - mpFilledH) + 2f, mpFilledH), barW - 4f, mpFilledH);

        shapeRenderer.end();

        // HP/MP outlines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(255f / 255f, 200f / 255f, 100f / 255f, 1f);
        shapeRenderer.rect(px, fy(py, ph), pw, ph);
        shapeRenderer.setColor(80f / 255f, 170f / 255f, 255f / 255f, 1f);
        shapeRenderer.rect(px + 3f, fy(py + 3f, ph - 6f), pw - 6f, ph - 6f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(bx1, fy(barY, barH), barW, barH);
        shapeRenderer.rect(bx2, fy(barY, barH), barW, barH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Texts
        spriteBatch.begin();
        font8.draw(spriteBatch, "DEO", px + 8, fy(py + 10, 0));
        font8.draw(spriteBatch, "LV " + player.getLevel(), px + 5, fy(py + 22, 0));

        font8.draw(spriteBatch, "HP", bx1 - 1, fy(barY + barH / 2f - 10, 0));
        font8.draw(spriteBatch, String.valueOf(player.getHp()), bx1 - 1, fy(barY + barH / 2f + 5, 0));

        font8.draw(spriteBatch, "EN", bx2 - 1, fy(barY + barH / 2f - 10, 0));
        font8.draw(spriteBatch, String.valueOf(player.getMp()), bx2 - 1, fy(barY + barH / 2f + 5, 0));
        spriteBatch.end();
    }

    private void drawActionOrderPanel() {
        float ox = (1253f - 480f) / 2f;
        float oy = 20f;
        float colW = 480f;
        float colH = 46f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(6f / 255f, 8f / 255f, 18f / 255f, 0.50f);
        shapeRenderer.rect(ox, fy(oy, colH), colW, colH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(255f / 255f, 200f / 255f, 90f / 255f, 1f);
        shapeRenderer.rect(ox, fy(oy, colH), colW, colH);
        shapeRenderer.setColor(60f / 255f, 190f / 255f, 255f / 255f, 1f);
        shapeRenderer.rect(ox + 3f, fy(oy + 3f, colH - 6f), colW - 6f, colH - 6f);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Header Title Text
        spriteBatch.begin();
        String title = "ACTION ORDER";
        font8.draw(spriteBatch, title, ox + (colW - 80) / 2f, fy(oy + 1, 0));
        spriteBatch.end();

        // Cards list
        int maxCards = 6;
        boolean dual = enemy.isAlive();
        boolean[] actorIsPlayer = new boolean[maxCards];
        actorIsPlayer[0] = timelineActiveIsPlayer;
        for (int i = 1; i < maxCards; i++) {
            actorIsPlayer[i] = dual ? !actorIsPlayer[i - 1] : true;
        }

        float pad = 6f;
        float gap = 3f;
        float cardY = oy + 12f;
        float cardH = colH - 16f;
        float availableW = colW - 2 * pad - (maxCards - 1) * gap;
        float cardW = availableW / maxCards;

        float x = ox + pad;
        for (int i = 0; i < maxCards; i++) {
            drawActionOrderCard(x, cardY, cardW, cardH, actorIsPlayer[i], i == 0);
            x += cardW + gap;
        }
    }

    private void drawActionOrderCard(float x, float y, float cw, float ch, boolean isPlayer, boolean active) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(14f / 255f, 22f / 255f, 48f / 255f, 0.72f);
        shapeRenderer.rect(x, fy(y, ch), cw, ch);

        // Active notch dot
        float notchY = y + ch / 2f - 4f;
        if (isPlayer) {
            shapeRenderer.setColor(active ? Color.CYAN : new Color(0f, 160f / 255f, 200f / 255f, 1f));
        } else {
            shapeRenderer.setColor(active ? new Color(255f / 255f, 120f / 255f, 80f / 255f, 1f)
                    : new Color(180f / 255f, 70f / 255f, 55f / 255f, 1f));
        }
        if (active) {
            shapeRenderer.circle(x + 5f, fy(notchY + 3.5f, 0), 3.5f);
        } else {
            shapeRenderer.circle(x + 5f, fy(notchY + 2.5f, 0), 2.5f);
        }
        shapeRenderer.end();

        // Card outlines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(active ? new Color(255f / 255f, 210f / 255f, 72f / 255f, 1f)
                : new Color(70f / 255f, 150f / 255f, 210f / 255f, 1f));
        shapeRenderer.rect(x, fy(y, ch), cw, ch);
        if (active) {
            shapeRenderer.rect(x + 1f, fy(y + 1f, ch - 2f), cw - 2f, ch - 2f);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Card Sprites / Labels
        spriteBatch.begin();
        float ph = active ? 20f : 16f;
        float pxx = x + 8f;
        float pyy = y + (ch - ph) / 2f;

        Texture face = isPlayer ? playerSprite : enemyPortraitStill;
        if (face != null) {
            spriteBatch.draw(face, pxx, fy(pyy, ph), ph, ph);
        }

        // Draw name role text
        float nameX = pxx + ph + 3f;
        float nameAreaW = x + cw - nameX - 3f;
        if (nameAreaW > 10f) {
            Color accentCol = isPlayer ? (active ? Color.CYAN : new Color(0f, 160f / 255f, 210f / 255f, 1f))
                    : (active ? new Color(255f / 255f, 130f / 255f, 60f / 255f, 1f)
                            : new Color(190f / 255f, 80f / 255f, 40f / 255f, 1f));
            String charName = isPlayer ? "DEO" : enemy.getName().toUpperCase();
            if (charName.length() > 4)
                charName = charName.substring(0, 4);

            float nameY = y + (ch / 2f) + 3f;
            font8.setColor(accentCol);
            font8.draw(spriteBatch, charName, nameX, fy(nameY, 0));

            float roleY = nameY + 6f;
            font8.setColor(new Color(160f / 255f, 180f / 255f, 210f / 255f, 0.8f));
            font8.draw(spriteBatch, isPlayer ? "PLAYER" : "BOSS", nameX, fy(roleY, 0));
        }
        spriteBatch.end();
    }

    private void drawDialogueBox() {
        float x = 20f;
        float y = 832f - 150f - 20f;
        float w = 1253f - 40f;
        float h = 150f;

        spriteBatch.begin();
        if (dialogBoxImage != null) {
            spriteBatch.draw(dialogBoxImage, x, fy(y, h), w, h);
        }
        spriteBatch.end();

        if (dialogBoxImage == null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(180f / 255f, 160f / 255f, 100f / 255f, 1f);
            shapeRenderer.rect(x, fy(y, h), w, h);
            shapeRenderer.setColor(20f / 255f, 20f / 255f, 20f / 255f, 1f);
            shapeRenderer.rect(x + 3, fy(y + 3, h - 6), w - 6, h - 6);
            shapeRenderer.end();
        }

        // Draw Speaker Tag
        float innerLeft = x + 20f + (dialogBoxImage != null ? 48f : 0f);
        float innerTop = y + 20f;
        float nameW = 150f;
        float nameH = 22f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(16f / 255f, 22f / 255f, 38f / 255f, 1f);
        shapeRenderer.rect(innerLeft, fy(innerTop, nameH), nameW, nameH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(200f / 255f, 175f / 255f, 95f / 255f, 1f);
        shapeRenderer.rect(innerLeft, fy(innerTop, nameH), nameW, nameH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.begin();
        font10.setColor(Color.WHITE);
        font10.draw(spriteBatch, currentSpeaker.toUpperCase(), innerLeft + 10, fy(innerTop + 5, 0));

        // Draw Speech Text
        String displayedText = currentMessage.substring(0, textCharIndex);
        String fmt = formatStringWithMarkup(displayedText);
        font11.draw(spriteBatch, fmt, innerLeft, fy(innerTop + nameH + 15f, 0));

        // Arrow indicator
        float innerRight = x + w - 20f - (dialogBoxImage != null ? 48f : 0f);
        float innerBottom = y + h - 20f;
        float ax = innerRight - 22f;
        float ay = innerBottom - 8f;
        spriteBatch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(255f / 255f, 230f / 255f, 140f / 255f, 1f);
        shapeRenderer.triangle(ax - 8f, fy(ay - 10f, 0), ax, fy(ay, 0), ax + 8f, fy(ay - 10f, 0));
        shapeRenderer.end();
    }

    private void drawMenuButtons() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(6f / 255f, 8f / 255f, 18f / 255f, 0.40f);
        shapeRenderer.rect(0, fy(832f - 132f, 132f), 1253f, 132f);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        Vector3 rawMouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(rawMouse);
        float mx = rawMouse.x;
        float my = 832 - rawMouse.y;

        for (int i = 0; i < menuButtonRects.length; i++) {
            Rectangle rect = menuButtonRects[i];
            BattleMenuIcon kind = menuButtonKinds[i];
            Texture icon = getIconFor(kind);

            boolean hovered = rect.contains(mx, my);
            boolean pressed = hovered && Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.LEFT);

            float scale = hovered ? 1.05f : 1f;
            float finalW = rect.width * scale;
            float finalH = rect.height * scale;
            float finalX = rect.x - (finalW - rect.width) / 2f;
            float finalY = rect.y - (finalH - rect.height) / 2f;

            drawButton(finalX, finalY, finalW, finalH, icon, hovered, pressed);
        }
    }

    private Texture getIconFor(BattleMenuIcon kind) {
        switch (kind) {
            case ATTACK:
                return battleIconAttack;
            case INVENTORY:
                return battleIconInventory;
            case RUN:
                return battleIconRun;
            case PUNCH:
                return battleIconPunch;
            case SLASH:
                return battleIconSlash;
            case DODGE:
                return battleIconDodge;
            case HEAL:
                return battleIconHeal;
            case BACK:
                return battleIconBack;
            default:
                return null;
        }
    }

    private void drawButton(float x, float y, float w, float h, Texture icon, boolean hovered, boolean pressed) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (pressed) {
            shapeRenderer.setColor(25f / 255f, 35f / 255f, 75f / 255f, 1f);
        } else if (hovered) {
            shapeRenderer.setColor(45f / 255f, 60f / 255f, 110f / 255f, 1f);
        } else {
            shapeRenderer.setColor(35f / 255f, 45f / 255f, 90f / 255f, 1f);
        }
        shapeRenderer.rect(x, fy(y, h), w, h);

        shapeRenderer.setColor(Color.WHITE);
        float pad = 5f;
        float dotSize = 3f;
        shapeRenderer.rect(x + pad, fy(y + pad, dotSize), dotSize, dotSize);
        shapeRenderer.rect(x + w - pad - dotSize, fy(y + pad, dotSize), dotSize, dotSize);
        shapeRenderer.rect(x + pad, fy(y + h - pad - dotSize, dotSize), dotSize, dotSize);
        shapeRenderer.rect(x + w - pad - dotSize, fy(y + h - pad - dotSize, dotSize), dotSize, dotSize);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(x + 1f, fy(y + 1f, h - 2f), w - 2f, h - 2f);
        shapeRenderer.end();

        if (hovered) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1f, 1f, 1f, 30f / 255f);
            shapeRenderer.rect(x + 1f, fy(y + 1f, h - 2f), w - 2f, h - 2f);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (icon != null) {
            spriteBatch.begin();
            float iconSize = Math.min(w, h) - 8f;
            spriteBatch.draw(icon, x + (w - iconSize) / 2f, fy(y + (h - iconSize) / 2f, iconSize), iconSize, iconSize);
            spriteBatch.end();
        }
    }

    private void drawCinematicIntro() {
        if (introTimer <= 0)
            return;

        float h = 832f;
        float w = 1253f;
        float barSize = (float) (h * 0.5f * (introTimer / 100f));

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(0, fy(0, barSize), w, barSize);
        shapeRenderer.rect(0, fy(h - barSize, barSize), w, barSize);
        shapeRenderer.end();

        if (introTimer > 50) {
            float alpha = (introTimer - 50) / 50.0f;
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1f, 1f, 1f, alpha);
            shapeRenderer.rect(0, 0, w, h);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            spriteBatch.begin();
            font60.setColor(new Color(0f, 0.7f, 1f, alpha));
            String title = "THE LAST ANCESTOR";
            font60.draw(spriteBatch, title, (w - 780) / 2f, fy(h / 2f, 0));
            spriteBatch.end();
        }

        introTimer -= 1;
    }

    private void drawVictoryScreen() {
        if (victoryTimer <= 0)
            return;

        float h = 832f;
        float w = 1253f;
        float alpha = Math.min(1.0f, victoryTimer / 50.0f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.05f, 0.2f, alpha * 0.6f);
        shapeRenderer.rect(0, 0, w, h);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.begin();
        String text = "VICTORY";
        float offset = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 5);
        font80.setColor(new Color(0f, 0f, 0f, alpha * 0.8f));
        font80.draw(spriteBatch, text, (w - 480) / 2f + offset, fy(h / 2f + offset, 0));

        font80.setColor(new Color(1f, 0.8f, 0f, alpha));
        font80.draw(spriteBatch, text, (w - 480) / 2f, fy(h / 2f, 0));

        if (victoryTimer > 30) {
            font20.setColor(Color.WHITE);
            String sub = "ENEMY DEFEATED - EXP GAINED";
            font20.draw(spriteBatch, sub, (w - 560) / 2f, fy(h / 2f + 50f, 0));
        }
        spriteBatch.end();

        if (victoryTimer > 60) {
            float fadeOutAlpha = (victoryTimer - 60) / 40.0f;
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, Math.max(0.0f, Math.min(1.0f, fadeOutAlpha)));
            shapeRenderer.rect(0, 0, w, h);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (victoryTimer < 100)
            victoryTimer++;
    }

    private void drawDefeatScreen() {
        if (defeatTimer <= 0)
            return;

        float w = 1253f;
        float h = 832f;
        float alpha = Math.min(1.0f, defeatTimer / 50.0f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.14f, 0.0f, 0.02f, alpha * 0.80f);
        shapeRenderer.rect(0, 0, w, h);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.begin();
        String titleText = "DEFEAT";
        float titleY = h / 2f - 60f;
        float osc = (float) (Math.sin(System.currentTimeMillis() / 220.0) * 5);
        font80.setColor(new Color(0f, 0f, 0f, alpha * 0.8f));
        font80.draw(spriteBatch, titleText, (w - 480) / 2f + osc, fy(titleY + osc, 0));

        font80.setColor(new Color(255f / 255f, 60f / 255f, 20f / 255f, alpha));
        font80.draw(spriteBatch, titleText, (w - 480) / 2f, fy(titleY, 0));
        spriteBatch.end();

        if (defeatTimer > 15) {
            float panelAlpha = Math.min(1.0f, (defeatTimer - 15) / 25.0f) * alpha;
            float panelW = 520f;
            float panelH = 185f;
            float panelX = (w - panelW) / 2f;
            float panelY = titleY + 28f;

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(8f / 255f, 0f, 4f / 255f, panelAlpha * 0.90f);
            shapeRenderer.rect(panelX, fy(panelY, panelH), panelW, panelH);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(170f / 255f, 25f / 255f, 25f / 255f, panelAlpha);
            shapeRenderer.rect(panelX, fy(panelY, panelH), panelW, panelH);
            shapeRenderer.setColor(255f / 255f, 60f / 255f, 30f / 255f, panelAlpha * 0.3f);
            shapeRenderer.rect(panelX + 3, fy(panelY + 3, panelH - 6), panelW - 6, panelH - 6);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            spriteBatch.begin();
            font10.setColor(new Color(255f / 255f, 100f / 255f, 60f / 255f, panelAlpha));
            String header = "─── BATTLE REPORT ───";
            font10.draw(spriteBatch, header, panelX + (panelW - 200) / 2f, fy(panelY + 22f, 0));
            spriteBatch.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(130f / 255f, 20f / 255f, 20f / 255f, panelAlpha * 0.7f);
            shapeRenderer.rect(panelX + 12, fy(panelY + 28f, 1f), panelW - 24, 1);
            shapeRenderer.end();

            spriteBatch.begin();
            float col1X = panelX + 18f;
            float col2X = panelX + panelW / 2f + 8f;
            float rowY = panelY + 48f;
            float rowGap = 20f;

            font10.setColor(Color.CYAN);
            font10.draw(spriteBatch, "[ DEO ]", col1X, fy(rowY, 0));
            font10.setColor(new Color(180f / 255f, 215f / 255f, 255f / 255f, panelAlpha));
            font10.draw(spriteBatch, "Level  : " + snapLevel, col1X, fy(rowY + rowGap, 0));
            font10.draw(spriteBatch, "Max HP : " + snapPlayerMaxHp, col1X, fy(rowY + rowGap * 2f, 0));
            font10.draw(spriteBatch, "ATK    : " + snapPlayerAtk, col1X, fy(rowY + rowGap * 3f, 0));
            font10.draw(spriteBatch, "DEF    : " + snapPlayerDef, col1X, fy(rowY + rowGap * 4f, 0));

            String eName = snapEnemyName.length() > 9 ? snapEnemyName.substring(0, 9).toUpperCase()
                    : snapEnemyName.toUpperCase();
            font10.setColor(new Color(255f / 255f, 110f / 255f, 55f / 255f, panelAlpha));
            font10.draw(spriteBatch, "[ " + eName + " ]", col2X, fy(rowY, 0));
            font10.setColor(new Color(255f / 255f, 200f / 255f, 175f / 255f, panelAlpha));
            font10.draw(spriteBatch, "Max HP : " + snapEnemyMaxHp, col2X, fy(rowY + rowGap, 0));
            font10.draw(spriteBatch, "ATK    : " + snapEnemyAtk, col2X, fy(rowY + rowGap * 2f, 0));
            font10.draw(spriteBatch, "DEF    : " + snapEnemyDef, col2X, fy(rowY + rowGap * 3f, 0));

            int netDmg = Math.max(0, snapEnemyAtk - snapPlayerDef);
            font10.setColor(new Color(230f / 255f, 190f / 255f, 80f / 255f, panelAlpha * 0.86f));
            String hint = "Enemy dealt " + netDmg + " net dmg/hit - Level up to raise DEF!";
            font10.draw(spriteBatch, hint, panelX + (panelW - 380) / 2f, fy(panelY + panelH - 12f, 0));
            spriteBatch.end();

            if (defeatTimer > 50) {
                float btnAlpha = Math.min(1.0f, (defeatTimer - 50) / 20.0f);
                float btnW = 180f;
                float btnH = 38f;
                float btnX = (w - btnW) / 2f;
                float btnY = panelY + panelH + 14f;
                retryButtonRect = new Rectangle(btnX, btnY, btnW, btnH);

                float pulse = 0.65f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 280.0);
                Gdx.gl.glEnable(GL20.GL_BLEND);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(Math.min(1f, 0.55f * pulse), 0.04f, 0.04f, btnAlpha);
                shapeRenderer.rect(btnX, fy(btnY, btnH), btnW, btnH);
                shapeRenderer.end();

                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(230f / 255f, 55f / 255f, 55f / 255f, btnAlpha);
                shapeRenderer.rect(btnX, fy(btnY, btnH), btnW, btnH);
                shapeRenderer.end();
                Gdx.gl.glDisable(GL20.GL_BLEND);

                spriteBatch.begin();
                font11.setColor(new Color(255f / 255f, 215f / 255f, 200f / 255f, btnAlpha));
                font11.draw(spriteBatch, "RETRY BATTLE", btnX + (btnW - 130) / 2f, fy(btnY + 11f, 0));
                spriteBatch.end();
            }
        }

        if (defeatTimer < 100)
            defeatTimer++;
    }

    private String formatStringWithMarkup(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int start = i;
            boolean isDigit = java.lang.Character.isDigit(s.charAt(i));
            while (i < s.length() && java.lang.Character.isDigit(s.charAt(i)) == isDigit) {
                i++;
            }
            String segment = s.substring(start, i);
            if (isDigit) {
                boolean isBossNameNum = false;
                if (start >= 3) {
                    String before = s.substring(start - 3, start);
                    if (before.equalsIgnoreCase("Mk-")) {
                        isBossNameNum = true;
                    }
                }
                if (isBossNameNum) {
                    sb.append(segment);
                } else {
                    boolean isEnergyNum = false;
                    int lookAheadMax = Math.min(s.length(), i + 6);
                    String after = s.substring(i, lookAheadMax).toUpperCase();
                    if (after.contains("EN") || after.contains("ENERGY")) {
                        isEnergyNum = true;
                    }

                    if (isEnergyNum) {
                        sb.append("[#50bef0]").append(segment).append("[]");
                    } else if (s.toLowerCase().contains("heal") || s.toLowerCase().contains("memulihkan")
                            || s.toLowerCase().contains("potion") || segment.startsWith("+")) {
                        sb.append("[#50dc64]").append(segment).append("[]");
                    } else {
                        sb.append("[#ff5a5a]").append(segment).append("[]");
                    }
                }
            } else {
                sb.append(segment);
            }
        }
        return sb.toString();
    }

    public DevConsole getDevConsole() {
        return devConsole;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        spriteBatch.dispose();
        shapeRenderer.dispose();

        font8.dispose();
        font10.dispose();
        font11.dispose();
        font13.dispose();
        font14.dispose();
        font20.dispose();
        font32.dispose();
        font60.dispose();
        font80.dispose();

        for (Texture tex : allTextures) {
            tex.dispose();
        }
    }
}
