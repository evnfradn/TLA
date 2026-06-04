package com.mygame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.Rectangle;

/**
 * CaveScreen — layar gua yang dituju setelah melihat dari jendela kelas.
 *
 * Spawn: tengah map secara horizontal, 9 tile dari bawah.
 * Map: Eksplore/Cave/Cave_Map.tmx
 */
public class CaveScreen implements Screen {

    // ── Referensi utama ──────────────────────────────────────────────────────
    private final TheLastAncestorsGame game;
    private boolean isDisposed = false;

    // ── Rendering ────────────────────────────────────────────────────────────
    private SpriteBatch     spriteBatch;
    private ShapeRenderer   shapeRenderer;
    private OrthographicCamera camera;
    private Viewport        viewport;
    private OrthographicCamera uiCamera;

    // ── Tiled Map ─────────────────────────────────────────────────────────────
    private TiledMap                  map;
    private OrthogonalTiledMapRenderer renderer;
    private TiledMapTileLayer         groundLayer;
    private TiledMapTileLayer         pathLayer;
    private TiledMapTileLayer         wallLayer;
    private TiledMapTileLayer         collisionLayer;
    private int mapWidth, mapHeight;

    // ── Player ────────────────────────────────────────────────────────────────
    // Spawn: tengah horizontal, 9 tile dari bawah
    // mapWidth = 29, center tile X = 14 → pixelX = 14*32 - 42 = 406
    // 9 tiles from bottom → pixelY = 9*32 = 288
    private float playerX = 406f;
    private float playerY = 288f;
    private static final float SPEED = 120f;

    private Animation<TextureRegion> walkNorth, walkEast, walkSouth, walkWest;
    private Animation<TextureRegion> idleNorth, idleEast, idleSouth, idleWest;

    private enum Direction { NORTH, EAST, SOUTH, WEST }
    private Direction currentDirection = Direction.SOUTH;
    private boolean   isMoving   = false;
    private float     stateTime  = 0f;

    // ── Fonts & Textures ─────────────────────────────────────────────────────
    private BitmapFont font;
    private BitmapFont headerFont;
    private Array<Texture> allTextures = new Array<>();

    // ── Game Menu ─────────────────────────────────────────────────────────────
    private GameMenu gameMenu;

    // ── Fade-in dari transition ───────────────────────────────────────────────
    private float fadeInAlpha = 1f; // starts black, fades to 0
    private float fadeOutAlpha = 0f; // starts 0, fades to black
    private boolean isFadingOut = false;
    private boolean showExitConfirm = false;
    private Texture dialogBoxImage;
    private Texture checkBoxYesTexture;
    private Texture checkBoxNoTexture;
    private boolean showOptionConfirm = false;
    private boolean showQuestWarning = false;

    // UI Quest system fields
    private BitmapFont questTitleFont;
    private BitmapFont questHeaderFont;
    private BitmapFont questDescFont;
    private boolean isQuestUiOpen = false;
    private String activeQuestTab = "Daily";
    private Rectangle questBtnRect = new Rectangle(1090, 784, 140, 36);
    private Rectangle closeBtnRect = new Rectangle(935, 605, 30, 30);
    private Rectangle tabDailyRect = new Rectangle(281, 576, 180, 36);
    private Rectangle tabAchievementsRect = new Rectangle(476, 576, 220, 36);
    private Rectangle[] questActionBtnRects = {
        new Rectangle(840, 450 + 20, 110, 36),
        new Rectangle(840, 340 + 20, 110, 36),
        new Rectangle(840, 230 + 20, 110, 36)
    };
    private Array<FloatingText> floatingTexts = new Array<>();
    private String bannerText = "";
    private float bannerTimer = 0f;
    private float bannerAnimY = 0f;
    private Array<String> completedBannersShown = new Array<>();

    // ── Save toast ────────────────────────────────────────────────────────────
    private boolean showSaveToast = false;
    private float   saveToastTimer = 0f;

    // ── Cave ambient darkness overlay alpha ───────────────────────────────────
    private static final float CAVE_DARK = 0.45f;

    // ── Dev console (reused) ──────────────────────────────────────────────────
    private DevConsole devConsole;

    // =========================================================================
    public CaveScreen(TheLastAncestorsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        spriteBatch   = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera        = new OrthographicCamera();
        viewport      = new FitViewport(1253, 832, camera);
        camera.zoom   = 0.5f;

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, 1253, 832);

        // Load cave map
        map      = new TmxMapLoader().load("Eksplore/Cave/Cave_Map.tmx");
        renderer = new OrthogonalTiledMapRenderer(map);

        MapProperties prop = map.getProperties();
        mapWidth  = prop.get("width",  Integer.class);
        mapHeight = prop.get("height", Integer.class);

        groundLayer = (TiledMapTileLayer) map.getLayers().get("Ground");
        pathLayer   = (TiledMapTileLayer) map.getLayers().get("Path");
        wallLayer   = (TiledMapTileLayer) map.getLayers().get("Wall");
        collisionLayer = (TiledMapTileLayer) map.getLayers().get("Collision");
        if (collisionLayer != null) collisionLayer.setVisible(false);

        // Player animations (same ASWD sprites)
        walkNorth = loadWalkAnim("North");
        walkEast  = loadWalkAnim("East");
        walkSouth = loadWalkAnim("South");
        walkWest  = loadWalkAnim("West");
        idleNorth = loadIdleAnim("North");
        idleEast  = loadIdleAnim("East");
        idleSouth = loadIdleAnim("South");
        idleWest  = loadIdleAnim("West");

        // Fonts
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("Battle/PressStart2P.ttf"));

        FreeTypeFontParameter p1 = new FreeTypeFontParameter();
        p1.size = 10; p1.borderWidth = 1f; p1.borderColor = Color.BLACK;
        font = gen.generateFont(p1);

        FreeTypeFontParameter p2 = new FreeTypeFontParameter();
        p2.size = 14; p2.borderWidth = 1.5f; p2.borderColor = Color.BLACK;
        headerFont = gen.generateFont(p2);

        FreeTypeFontParameter questTitleParam = new FreeTypeFontParameter();
        questTitleParam.borderWidth = 2.0f;
        questTitleParam.borderColor = Color.BLACK;
        questTitleParam.size = 18;
        questTitleFont = gen.generateFont(questTitleParam);

        FreeTypeFontParameter questHeaderParam = new FreeTypeFontParameter();
        questHeaderParam.borderWidth = 1.5f;
        questHeaderParam.borderColor = Color.BLACK;
        questHeaderParam.size = 12;
        questHeaderFont = gen.generateFont(questHeaderParam);

        FreeTypeFontParameter questDescParam = new FreeTypeFontParameter();
        questDescParam.borderWidth = 1.0f;
        questDescParam.borderColor = Color.BLACK;
        questDescParam.size = 8;
        questDescFont = gen.generateFont(questDescParam);

        gen.dispose();

        // Dialog textures
        dialogBoxImage = new Texture(Gdx.files.internal("Eksplore/UI/Dialog_Box.png"));
        checkBoxYesTexture = new Texture(Gdx.files.internal("Eksplore/UI/Check_Box_Yes.png"));
        checkBoxNoTexture = new Texture(Gdx.files.internal("Eksplore/UI/Check_Box_No.png"));
        allTextures.add(dialogBoxImage);
        allTextures.add(checkBoxYesTexture);
        allTextures.add(checkBoxNoTexture);

        // Game menu
        gameMenu = new GameMenu(game, headerFont, font);

        // Dev console
        devConsole = DevConsole.getInstance();
        Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter() {
            @Override public boolean keyTyped(char c) { return devConsole.keyTyped(c); }
        });
    }

    // ── Animation loaders ────────────────────────────────────────────────────
    private Animation<TextureRegion> loadWalkAnim(String dir) {
        TextureRegion[] frames = new TextureRegion[9];
        for (int i = 1; i <= 9; i++) {
            String path = "Eksplore/ASWD/" + dir + "/" + dir + "/" + dir + "_" + i + ".png";
            Texture tex = new Texture(Gdx.files.internal(path));
            allTextures.add(tex);
            frames[i-1] = new TextureRegion(tex);
        }
        Animation<TextureRegion> a = new Animation<>(0.08f, frames);
        a.setPlayMode(Animation.PlayMode.LOOP);
        return a;
    }

    private Animation<TextureRegion> loadIdleAnim(String dir) {
        TextureRegion[] frames = new TextureRegion[2];
        for (int i = 1; i <= 2; i++) {
            String path = "Eksplore/ASWD/" + dir + "/Idle_" + dir + "/Idle_" + dir + "_" + i + ".png";
            Texture tex = new Texture(Gdx.files.internal(path));
            allTextures.add(tex);
            frames[i-1] = new TextureRegion(tex);
        }
        Animation<TextureRegion> a = new Animation<>(0.25f, frames);
        a.setPlayMode(Animation.PlayMode.LOOP);
        return a;
    }

    // =========================================================================
    @Override
    public void render(float delta) {
        if (isDisposed) return;
        stateTime += delta;
        devConsole.update(delta);

        // ── Fade-in ────────────────────────────────────────────────────────
        if (fadeInAlpha > 0f) fadeInAlpha = Math.max(0f, fadeInAlpha - delta * 1.5f);

        // ── Fade-out ───────────────────────────────────────────────────────
        if (isFadingOut) {
            fadeOutAlpha = Math.min(1f, fadeOutAlpha + delta * 1.5f);
            if (fadeOutAlpha >= 1f) {
                game.setScreen(new OutWorldScreen(game));
                return;
            }
        }

        // ── Input ──────────────────────────────────────────────────────────
        if (gameMenu.isOpen()) {
            GameMenu.Action action = gameMenu.handleInput();
            handleMenuAction(action);
        } else {
            handleInput(delta);
        }
        if (isDisposed) return;

        // ── Clear & render map ─────────────────────────────────────────────
        ScreenUtils.clear(0.05f, 0.04f, 0.08f, 1f);
        updateCamera();
        renderer.setView(camera);

        boolean wallOnTop = isWallOnTop();

        // Pass 1: Render background layers underneath player
        if (groundLayer != null) groundLayer.setVisible(true);
        if (pathLayer != null) pathLayer.setVisible(true);
        if (wallLayer != null) wallLayer.setVisible(!wallOnTop);
        if (collisionLayer != null) collisionLayer.setVisible(false);
        renderer.render();

        // ── Render player ──────────────────────────────────────────────────
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        spriteBatch.draw(getPlayerFrame(), playerX, playerY, 84f, 84f);
        spriteBatch.end();

        // Pass 2: Render Wall layer on top of player if it is on top
        if (wallOnTop) {
            if (groundLayer != null) groundLayer.setVisible(false);
            if (pathLayer != null) pathLayer.setVisible(false);
            if (wallLayer != null) wallLayer.setVisible(true);
            renderer.render();
        }

        // ── Cave darkness overlay ──────────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, CAVE_DARK);
        float hw = mapWidth  * 32f;
        float hh = mapHeight * 32f;
        shapeRenderer.rect(0, 0, hw, hh);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── HUD (universal top bar) ───────────────────────────────────────
        com.badlogic.gdx.math.Matrix4 uiProj = new com.badlogic.gdx.math.Matrix4();
        uiProj.setToOrtho2D(0, 0, 1253, 832);

        drawTopBarHUD();
        drawQuestUiPanel();
        drawBannerNotification(delta);
        drawFloatingTexts(delta);

        // Draw coordinates in UI space (top left, just below HUD bar)
        spriteBatch.setProjectionMatrix(uiProj);
        spriteBatch.begin();
        font.setColor(Color.YELLOW);
        font.draw(spriteBatch, "X: " + (int)playerX + "  Y: " + (int)playerY, 20f, 750f);
        spriteBatch.end();

        // ── Exit Confirm Popup ─────────────────────────────────────────────
        if (showExitConfirm) {
            drawExitConfirmPopup();
        }
        if (showQuestWarning) {
            drawQuestWarningPopup();
        }
        if (showOptionConfirm) {
            drawOptionConfirmPopup();
        }

        // ── Game Menu ─────────────────────────────────────────────────────
        if (gameMenu.isOpen()) {
            gameMenu.render(spriteBatch, shapeRenderer, delta);
        }

        // ── Save toast ────────────────────────────────────────────────────
        if (showSaveToast) drawSaveToast(delta);

        // ── Black fade-in ─────────────────────────────────────────────────
        if (fadeInAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(uiProj);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, fadeInAlpha);
            shapeRenderer.rect(0, 0, 1253, 832);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // ── Render hitbox debug overlay ───────────────────────────────
        if (devConsole.isShowHitbox()) {
            renderHitboxOverlay();
        }

        // ── Render collision debug overlay ────────────────────────────
        if (devConsole.isShowCollision()) {
            renderCollisionOverlay();
        }

        // ── Black fade-out ─────────────────────────────────────────────────
        if (isFadingOut) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(uiProj);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, fadeOutAlpha);
            shapeRenderer.rect(0, 0, 1253, 832);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // ── Render DevConsole UI (screen-space) ───────────────────────
        renderConsoleInScreenSpace();
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    private void handleInput(float delta) {
        if (isQuestUiOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                isQuestUiOpen = false;
            }
            handleQuestUiClicks();
            isMoving = false;
            return;
        }

        if (Gdx.input.justTouched()) {
            com.badlogic.gdx.math.Vector3 clickPoint = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            uiCamera.unproject(clickPoint, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
            if (questBtnRect.contains(clickPoint.x, clickPoint.y)) {
                isQuestUiOpen = true;
                isMoving = false;
                return;
            }
        }

        // ── DevConsole input (prioritas tertinggi, bisa dibuka di mana saja & kapan saja) ──
        if (devConsole.handleInput()) {
            if (devConsole.consumeReload()) {
                game.setScreen(new CaveScreen(game));
                this.dispose();
                return;
            }
            if (devConsole.consumeTeleport()) {
                playerX = devConsole.getTeleportX();
                playerY = devConsole.getTeleportY();
            }
            if (devConsole.consumeRestart()) {
                GameSave.clear();
                game.initDefaultQuests();
                game.setScreen(new ExplorationScreen(game));
                this.dispose();
                return;
            }
            if (devConsole.consumeBattle()) {
                game.setScreen(new BattleScreen(game));
                this.dispose();
                return;
            }
            if (devConsole.isOpen()) {
                isMoving = false;
                return;
            }
        }

        if (isFadingOut) {
            isMoving = false;
            return;
        }

        if (showExitConfirm) {
            if (Gdx.input.justTouched()) {
                com.badlogic.gdx.math.Vector3 touchPoint = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                uiCamera.unproject(touchPoint, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
                float mx = touchPoint.x;
                float my = touchPoint.y;

                // YES button: 426.5f to 576.5f, Y: 356f to 396f
                if (mx >= 426.5f && mx <= 576.5f && my >= 356f && my <= 396f) {
                    showExitConfirm = false;
                    isFadingOut = true;
                }
                if (mx >= 676.5f && mx <= 826.5f && my >= 356f && my <= 396f) {
                    showExitConfirm = false;
                    playerX = 10f; // Push player back
                }
            }
            isMoving = false;
            return;
        }

        if (showOptionConfirm) {
            if (Gdx.input.justTouched()) {
                com.badlogic.gdx.math.Vector3 touchPoint = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                uiCamera.unproject(touchPoint, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
                float mx = touchPoint.x;
                float my = touchPoint.y;
                // YES button: 426.5f to 576.5f, Y: 356f to 396f
                if (mx >= 426.5f && mx <= 576.5f && my >= 356f && my <= 396f) {
                    GameSave.save("cave", playerX, playerY, game.getGold(), game.getGems());
                    Gdx.app.exit();
                }
                // NO button: 676.5f to 826.5f, Y: 356f to 396f
                if (mx >= 676.5f && mx <= 826.5f && my >= 356f && my <= 396f) {
                    showOptionConfirm = false;
                }
            }
            isMoving = false;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            gameMenu.toggle();
            return;
        }

        // Trigger exit cave popup
        if (playerX <= 0f && Math.abs(playerY - 277f) < 20f) {
            showExitConfirm = true;
            isMoving = false;
            return;
        }

        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    { dy =  SPEED * delta; currentDirection = Direction.NORTH; }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))   { dy = -SPEED * delta; currentDirection = Direction.SOUTH; }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))  { dx =  SPEED * delta; currentDirection = Direction.EAST;  }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))   { dx = -SPEED * delta; currentDirection = Direction.WEST;  }

        dx *= devConsole.getSpeedMulti();
        dy *= devConsole.getSpeedMulti();

        isMoving = (dx != 0 || dy != 0);
        if (isMoving) {
            if (devConsole.isNoClip()) {
                playerX += dx;
                playerY += dy;
            } else {
                if (!isColliding(playerX + dx, playerY)) playerX += dx;
                if (!isColliding(playerX, playerY + dy)) playerY += dy;
            }
        }
    }

    private void handleMenuAction(GameMenu.Action action) {
        switch (action) {
            case TASK:
                isQuestUiOpen = true;
                activeQuestTab = "Daily";
                gameMenu.close();
                break;
            case ACHIEVEMENT:
                isQuestUiOpen = true;
                activeQuestTab = "Achievements";
                gameMenu.close();
                break;
            case OPTION:
                showOptionConfirm = true;
                gameMenu.close();
                break;
            default:
                break;
        }
    }

    // ── Collision ─────────────────────────────────────────────────────────────
    private boolean isColliding(float x, float y) {
        float colW = 12f, colH = 24f;
        float offX = (84f - colW) / 2f, offY = 4f;
        float[][] corners = {
            { x+offX,      y+offY },
            { x+offX+colW, y+offY },
            { x+offX,      y+offY+colH },
            { x+offX+colW, y+offY+colH }
        };
        float mapWpx = mapWidth  * 32f;
        float mapHpx = mapHeight * 32f;
        for (float[] p : corners) {
            if (p[0] < 0 || p[0] >= mapWpx || p[1] < 0 || p[1] >= mapHpx) return true;
            int tx = (int)(p[0]/32f), ty = (int)(p[1]/32f);
            if (collisionLayer != null && collisionLayer.getCell(tx, ty) != null) return true;
        }
        return false;
    }

    // ── Camera ────────────────────────────────────────────────────────────────
    private void updateCamera() {
        float mapWpx = mapWidth  * 32f, mapHpx = mapHeight * 32f;
        float vw = viewport.getWorldWidth() * camera.zoom;
        float vh = viewport.getWorldHeight() * camera.zoom;
        playerX = Math.max(0, Math.min(playerX, mapWpx - 32));
        playerY = Math.max(0, Math.min(playerY, mapHpx - 32));
        float cx = Math.max(vw/2f, Math.min(playerX+16f, mapWpx - vw/2f));
        float cy = Math.max(vh/2f, Math.min(playerY+16f, mapHpx - vh/2f));
        camera.position.set(cx, cy, 0);
        camera.update();
    }

    // ── Player frame ──────────────────────────────────────────────────────────
    private TextureRegion getPlayerFrame() {
        if (isMoving) {
            switch (currentDirection) {
                case NORTH:
                    return walkNorth.getKeyFrame(stateTime);
                case EAST:
                    return walkEast.getKeyFrame(stateTime);
                case WEST:
                    return walkWest.getKeyFrame(stateTime);
                default:
                    return walkSouth.getKeyFrame(stateTime);
            }
        } else {
            switch (currentDirection) {
                case NORTH:
                    return idleNorth.getKeyFrame(stateTime);
                case EAST:
                    return idleEast.getKeyFrame(stateTime);
                case WEST:
                    return idleWest.getKeyFrame(stateTime);
                default:
                    return idleSouth.getKeyFrame(stateTime);
            }
        }
    }

    // ── Save toast ────────────────────────────────────────────────────────────
    private void drawSaveToast(float delta) {
        saveToastTimer -= delta;
        if (saveToastTimer <= 0) { showSaveToast = false; return; }

        com.badlogic.gdx.math.Matrix4 uiProj = new com.badlogic.gdx.math.Matrix4();
        uiProj.setToOrtho2D(0, 0, 1253, 832);
        float alpha = Math.min(1f, saveToastTimer);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiProj);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.04f, 0.18f, 0.04f, 0.9f * alpha);
        shapeRenderer.rect(426, 30, 400, 50);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.setProjectionMatrix(uiProj);
        spriteBatch.begin();
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(spriteBatch, "✓  Game Tersimpan!", 470f, 63f);
        spriteBatch.end();
    }

    private void drawExitConfirmPopup() {
        float boxW = 500f;
        float boxH = 200f;
        float boxX = (1253f - boxW) / 2f;
        float boxY = (832f - boxH) / 2f;

        float mx = Gdx.input.getX();
        float my = 832f - Gdx.input.getY();

        boolean hoverYes = (mx >= boxX + 50f && mx <= boxX + 200f && my >= boxY + 40f && my <= boxY + 80f);
        boolean hoverNo = (mx >= boxX + boxW - 200f && mx <= boxX + boxW - 50f && my >= boxY + 40f && my <= boxY + 80f);

        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();

        if (dialogBoxImage != null) {
            spriteBatch.draw(dialogBoxImage, boxX, boxY, boxW, boxH);
        }

        if (checkBoxYesTexture != null) {
            if (hoverYes) {
                spriteBatch.setColor(1f, 1f, 0.8f, 1f);
                spriteBatch.draw(checkBoxYesTexture, boxX + 50f - 3.75f, boxY + 40f - 1f, 157.5f, 42f);
                spriteBatch.setColor(Color.WHITE);
            } else {
                spriteBatch.draw(checkBoxYesTexture, boxX + 50f, boxY + 40f, 150f, 40f);
            }
        }

        if (checkBoxNoTexture != null) {
            if (hoverNo) {
                spriteBatch.setColor(1f, 1f, 0.8f, 1f);
                spriteBatch.draw(checkBoxNoTexture, boxX + boxW - 200f - 3.75f, boxY + 40f - 1f, 157.5f, 42f);
                spriteBatch.setColor(Color.WHITE);
            } else {
                spriteBatch.draw(checkBoxNoTexture, boxX + boxW - 200f, boxY + 40f, 150f, 40f);
            }
        }

        headerFont.setColor(Color.WHITE);
        headerFont.draw(spriteBatch, "Coba pergi keluar Goa?", boxX, boxY + boxH - 50f, boxW, com.badlogic.gdx.utils.Align.center, false);
        spriteBatch.end();
    }

    private void drawQuestWarningPopup() {
        float boxW = 500f;
        float boxH = 200f;
        float boxX = (1253f - boxW) / 2f;
        float boxY = (832f - boxH) / 2f;

        float mx = Gdx.input.getX();
        float my = 832f - Gdx.input.getY();

        // Centered OK/TUTUP button
        boolean hoverOk = (mx >= boxX + 175f && mx <= boxX + 325f && my >= boxY + 40f && my <= boxY + 80f);

        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();

        if (dialogBoxImage != null) {
            spriteBatch.draw(dialogBoxImage, boxX, boxY, boxW, boxH);
        }

        if (checkBoxYesTexture != null) {
            if (hoverOk) {
                spriteBatch.setColor(1f, 1f, 0.8f, 1f);
                spriteBatch.draw(checkBoxYesTexture, boxX + 175f - 3.75f, boxY + 40f - 1f, 157.5f, 42f);
                spriteBatch.setColor(Color.WHITE);
            } else {
                spriteBatch.draw(checkBoxYesTexture, boxX + 175f, boxY + 40f, 150f, 40f);
            }
        }

        headerFont.setColor(Color.WHITE);
        headerFont.draw(spriteBatch, "Quest & Achievement\nhanya dapat dibuka di kelas!", boxX + 20f, boxY + boxH - 50f, boxW - 40f, com.badlogic.gdx.utils.Align.center, true);
        spriteBatch.end();
    }

    private void drawOptionConfirmPopup() {
        float boxW = 500f;
        float boxH = 200f;
        float boxX = (1253f - boxW) / 2f;
        float boxY = (832f - boxH) / 2f;

        float mx = Gdx.input.getX();
        float my = 832f - Gdx.input.getY();

        boolean hoverYes = (mx >= boxX + 50f && mx <= boxX + 200f && my >= boxY + 40f && my <= boxY + 80f);
        boolean hoverNo = (mx >= boxX + boxW - 200f && mx <= boxX + boxW - 50f && my >= boxY + 40f && my <= boxY + 80f);

        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();

        if (dialogBoxImage != null) {
            spriteBatch.draw(dialogBoxImage, boxX, boxY, boxW, boxH);
        }

        if (checkBoxYesTexture != null) {
            if (hoverYes) {
                spriteBatch.setColor(1f, 1f, 0.8f, 1f);
                spriteBatch.draw(checkBoxYesTexture, boxX + 50f - 3.75f, boxY + 40f - 1f, 157.5f, 42f);
                spriteBatch.setColor(Color.WHITE);
            } else {
                spriteBatch.draw(checkBoxYesTexture, boxX + 50f, boxY + 40f, 150f, 40f);
            }
        }

        if (checkBoxNoTexture != null) {
            if (hoverNo) {
                spriteBatch.setColor(1f, 1f, 0.8f, 1f);
                spriteBatch.draw(checkBoxNoTexture, boxX + boxW - 200f - 3.75f, boxY + 40f - 1f, 157.5f, 42f);
                spriteBatch.setColor(Color.WHITE);
            } else {
                spriteBatch.draw(checkBoxNoTexture, boxX + boxW - 200f, boxY + 40f, 150f, 40f);
            }
        }

        headerFont.setColor(Color.WHITE);
        headerFont.draw(spriteBatch, "Simpan & Keluar Permainan?", boxX, boxY + boxH - 50f, boxW, com.badlogic.gdx.utils.Align.center, false);
        spriteBatch.end();
    }

    /** Render kotak hitbox / collision player di atas map. */
    private void renderHitboxOverlay() {
        float colWidth = 12f;
        float colHeight = 24f;
        float offsetX = (84f - colWidth) / 2f;
        float offsetY = 4f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0f, 1f, 0f, 1f);
        shapeRenderer.rect(playerX + offsetX, playerY + offsetY, colWidth, colHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Render kotak collision layer map (merah semi-transparan). */
    private void renderCollisionOverlay() {
        if (collisionLayer == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 0f, 0f, 0.35f);

        for (int tx = 0; tx < mapWidth; tx++) {
            for (int ty = 0; ty < mapHeight; ty++) {
                if (collisionLayer.getCell(tx, ty) != null) {
                    shapeRenderer.rect(tx * 32f, ty * 32f, 32f, 32f);
                }
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private boolean isWallOnTop() {
        // Y: 515 s.d. 444
        if (playerY >= 444f && playerY <= 515f) return true;
        // Y: 412
        if (Math.abs(playerY - 412f) < 4f) return true;
        // Y: 188
        if (Math.abs(playerY - 188f) < 4f) return true;
        // Y: 184 s.d. 92
        if (playerY >= 92f && playerY <= 184f) return true;

        return false;
    }

    /** Render DevConsole dalam ruang layar penuh (bukan world-space). */
    private void renderConsoleInScreenSpace() {
        // Simpan projection asli
        com.badlogic.gdx.math.Matrix4 origProj = new com.badlogic.gdx.math.Matrix4(spriteBatch.getProjectionMatrix());

        // Set projection ke ukuran layar fisik agar console muncul di pojok atas layar
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        com.badlogic.gdx.math.Matrix4 screenProj = new com.badlogic.gdx.math.Matrix4();
        screenProj.setToOrtho2D(0, 0, screenW, screenH);

        spriteBatch.setProjectionMatrix(screenProj);
        shapeRenderer.setProjectionMatrix(screenProj);

        devConsole.render(spriteBatch, shapeRenderer, font, screenW, screenH);

        // Kembalikan projection asli
        spriteBatch.setProjectionMatrix(origProj);
        shapeRenderer.setProjectionMatrix(origProj);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override public void resize(int w, int h)  { viewport.update(w, h, true); }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    private void drawTopBarHUD() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        
        // 1. Translucent dark bar at the top
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(16f / 255f, 22f / 255f, 38f / 255f, 0.85f);
        shapeRenderer.rect(0, 772, 1253, 60);
        
        // 2. Avatar box
        shapeRenderer.setColor(24f / 255f, 32f / 255f, 54f / 255f, 0.95f);
        shapeRenderer.rect(20, 782, 40, 40);
        shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f); // gold border
        shapeRenderer.rect(20, 782, 40, 2);
        shapeRenderer.rect(20, 820, 40, 2);
        shapeRenderer.rect(20, 782, 2, 40);
        shapeRenderer.rect(60, 782, 2, 40);
        
        // Draw avatar face inside (cute cyan slime!)
        shapeRenderer.setColor(64f / 255f, 224f / 255f, 208f / 255f, 1f); // turquoise/cyan slime
        shapeRenderer.circle(40, 802, 12);
        
        // 3. Gold container capsule
        shapeRenderer.setColor(12f / 255f, 16f / 255f, 28f / 255f, 0.95f);
        shapeRenderer.rect(80, 784, 130, 36);
        shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 0.8f); // gold border
        shapeRenderer.rect(80, 784, 130, 2);
        shapeRenderer.rect(80, 818, 130, 2);
        shapeRenderer.rect(80, 784, 2, 36);
        shapeRenderer.rect(210, 784, 2, 36);
        
        // Draw gold coin icon
        shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f);
        shapeRenderer.circle(98, 802, 8);
        
        // 4. Gems container capsule
        shapeRenderer.setColor(12f / 255f, 16f / 255f, 28f / 255f, 0.95f);
        shapeRenderer.rect(230, 784, 130, 36);
        shapeRenderer.setColor(186f / 255f, 85f / 255f, 211f / 255f, 0.8f); // medium orchid border
        shapeRenderer.rect(230, 784, 130, 2);
        shapeRenderer.rect(230, 818, 130, 2);
        shapeRenderer.rect(230, 784, 2, 36);
        shapeRenderer.rect(360, 784, 2, 36);
        
        // Draw gem icon (diamond)
        shapeRenderer.setColor(186f / 255f, 85f / 255f, 211f / 255f, 1f);
        shapeRenderer.circle(248, 802, 7, 4); // diamond
        
        // 5. Quest Button
        shapeRenderer.setColor(115f / 255f, 71f / 255f, 46f / 255f, 1f);
        shapeRenderer.rect(1090, 784, 140, 36);
        shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f); // gold border
        shapeRenderer.rect(1090, 784, 140, 2);
        shapeRenderer.rect(1090, 818, 140, 2);
        shapeRenderer.rect(1090, 784, 2, 36);
        shapeRenderer.rect(1230, 784, 2, 36);
        
        // Check if there are unclaimed rewards (red notification dot)
        boolean hasNotification = false;
        for (Quest q : game.getQuests()) {
            if (q.isCompleted() && !q.isClaimed()) {
                hasNotification = true;
                break;
            }
        }
        
        if (hasNotification) {
            shapeRenderer.setColor(230f / 255f, 40f / 255f, 40f / 255f, 1f); // red
            shapeRenderer.circle(1225, 815, 6);
        }
        
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        // Slime eyes & mouth
        font.setColor(Color.BLACK);
        font.draw(spriteBatch, ".", 35, 807);
        font.draw(spriteBatch, ".", 43, 807);
        font.draw(spriteBatch, "_", 38, 800);
        
        // Draw Gold text
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "" + game.getGold(), 116, 808);
        
        // Draw Gems text
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "" + game.getGems(), 266, 808);
        
        // Draw Quest Button text
        questHeaderFont.setColor(Color.YELLOW);
        questHeaderFont.draw(spriteBatch, "QUEST", 1125, 808);
        
        spriteBatch.end();
    }

    private void drawQuestUiPanel() {
        if (!isQuestUiOpen) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        
        // 1. Semi-transparent black backdrop
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        shapeRenderer.rect(0, 0, 1253, 832);
        
        // 2. Main wooden dialog frame (750x500 centered)
        float boxX = 251f;
        float boxY = 166f;
        float boxW = 750f;
        float boxH = 500f;
        
        // Wood brown background
        shapeRenderer.setColor(90f / 255f, 50f / 255f, 30f / 255f, 1f);
        shapeRenderer.rect(boxX, boxY, boxW, boxH);
        
        // Gold outer border
        shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f);
        shapeRenderer.rect(boxX, boxY, boxW, 4f);
        shapeRenderer.rect(boxX, boxY + boxH - 4f, boxW, 4f);
        shapeRenderer.rect(boxX, boxY, 4f, boxH);
        shapeRenderer.rect(boxX + boxW - 4f, boxY, 4f, boxH);
        
        // Inner content dark background
        shapeRenderer.setColor(50f / 255f, 28f / 255f, 17f / 255f, 1f);
        shapeRenderer.rect(boxX + 20f, boxY + 20f, boxW - 40f, boxH - 90f);
        
        // 3. Tab Daily Button
        if (activeQuestTab.equals("Daily")) {
            shapeRenderer.setColor(140f / 255f, 85f / 255f, 50f / 255f, 1f); // lighter brown
            shapeRenderer.rect(281, 576, 180, 36);
            shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f); // gold border
            shapeRenderer.rect(281, 576, 180, 2);
            shapeRenderer.rect(281, 610, 180, 2);
            shapeRenderer.rect(281, 576, 2, 36);
            shapeRenderer.rect(461, 576, 2, 36);
        } else {
            shapeRenderer.setColor(65f / 255f, 35f / 255f, 20f / 255f, 1f); // darker brown
            shapeRenderer.rect(281, 576, 180, 36);
            shapeRenderer.setColor(40f / 255f, 20f / 255f, 10f / 255f, 1f); // dark border
            shapeRenderer.rect(281, 576, 180, 2);
            shapeRenderer.rect(281, 610, 180, 2);
            shapeRenderer.rect(281, 576, 2, 36);
            shapeRenderer.rect(461, 576, 2, 36);
        }
        
        // 4. Tab Achievements Button
        if (activeQuestTab.equals("Achievements")) {
            shapeRenderer.setColor(140f / 255f, 85f / 255f, 50f / 255f, 1f);
            shapeRenderer.rect(476, 576, 220, 36);
            shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f);
            shapeRenderer.rect(476, 576, 220, 2);
            shapeRenderer.rect(476, 610, 220, 2);
            shapeRenderer.rect(476, 576, 2, 36);
            shapeRenderer.rect(696, 576, 2, 36);
        } else {
            shapeRenderer.setColor(65f / 255f, 35f / 255f, 20f / 255f, 1f);
            shapeRenderer.rect(476, 576, 220, 36);
            shapeRenderer.setColor(40f / 255f, 20f / 255f, 10f / 255f, 1f);
            shapeRenderer.rect(476, 576, 220, 2);
            shapeRenderer.rect(476, 610, 220, 2);
            shapeRenderer.rect(476, 576, 2, 36);
            shapeRenderer.rect(696, 576, 2, 36);
        }
        
        // 5. Close Button [X]
        shapeRenderer.setColor(180f / 255f, 40f / 255f, 40f / 255f, 1f); // red
        shapeRenderer.rect(935, 605, 30, 30);
        shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f); // gold border
        shapeRenderer.rect(935, 605, 30, 2);
        shapeRenderer.rect(935, 633, 30, 2);
        shapeRenderer.rect(935, 605, 2, 30);
        shapeRenderer.rect(965, 605, 2, 30);
        
        // Filter quests for active tab
        Array<Quest> filteredQuests = new Array<>();
        for (Quest q : game.getQuests()) {
            if (q.getTab().equalsIgnoreCase(activeQuestTab)) {
                filteredQuests.add(q);
            }
        }
        
        // Draw Quest Cards
        for (int i = 0; i < Math.min(3, filteredQuests.size); i++) {
            Quest q = filteredQuests.get(i);
            float itemY = 450f - i * 110f;
            
            // Parchment beige card background
            shapeRenderer.setColor(235f / 255f, 220f / 255f, 195f / 255f, 1f);
            shapeRenderer.rect(281f, itemY, 690f, 95f);
            shapeRenderer.setColor(90f / 255f, 50f / 255f, 30f / 255f, 1f); // brown border
            shapeRenderer.rect(281f, itemY, 690f, 2f);
            shapeRenderer.rect(281f, itemY + 93f, 690f, 2f);
            shapeRenderer.rect(281f, itemY, 2f, 95f);
            shapeRenderer.rect(969f, itemY, 2f, 95f);
            
            // Progress Bar Background (Dark Gray)
            shapeRenderer.setColor(50f / 255f, 50f / 255f, 50f / 255f, 1f);
            shapeRenderer.rect(296f, itemY + 15f, 280f, 16f);
            
            // Progress Bar Filled (Cyan/Blue)
            float ratio = (float) q.getProgress() / q.getTarget();
            if (ratio > 0f) {
                shapeRenderer.setColor(50f / 255f, 150f / 255f, 230f / 255f, 1f);
                shapeRenderer.rect(296f, itemY + 15f, 280f * ratio, 16f);
            }
            
            // Reward Icon
            if (q.getRewardType().equals("Gems")) {
                shapeRenderer.setColor(186f / 255f, 85f / 255f, 211f / 255f, 1f); // magenta diamond
                shapeRenderer.circle(615f, itemY + 36f, 8f, 4);
            } else if (q.getRewardType().equals("Gold")) {
                shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f); // gold circle
                shapeRenderer.circle(615f, itemY + 36f, 8f);
            } else if (q.getRewardType().equals("Scroll")) {
                shapeRenderer.setColor(245f / 255f, 222f / 255f, 179f / 255f, 1f); // wheat color
                shapeRenderer.rect(605f, itemY + 28f, 20f, 16f);
                shapeRenderer.setColor(139f / 255f, 69f / 255f, 19f / 255f, 1f); // brown border scroll
                shapeRenderer.rect(605f, itemY + 28f, 20f, 2f);
                shapeRenderer.rect(605f, itemY + 42f, 20f, 2f);
                shapeRenderer.rect(605f, itemY + 28f, 2f, 16f);
                shapeRenderer.rect(623f, itemY + 28f, 2f, 16f);
            }
            
            // Action Button
            if (q.isCompleted() && !q.isClaimed()) {
                shapeRenderer.setColor(40f / 255f, 180f / 255f, 40f / 255f, 1f); // Green
            } else if (q.isClaimed()) {
                shapeRenderer.setColor(120f / 255f, 120f / 255f, 120f / 255f, 1f); // Gray
            } else {
                shapeRenderer.setColor(50f / 255f, 120f / 255f, 200f / 255f, 1f); // Blue
            }
            float btnX = questActionBtnRects[i].x;
            float btnY = questActionBtnRects[i].y;
            float btnW = questActionBtnRects[i].width;
            float btnH = questActionBtnRects[i].height;
            shapeRenderer.rect(btnX, btnY, btnW, btnH);
            
            shapeRenderer.setColor(255f / 255f, 255f / 255f, 255f / 255f, 0.4f);
            shapeRenderer.rect(btnX, btnY + btnH - 2f, btnW, 2f); // highlight line
            shapeRenderer.setColor(0f, 0f, 0f, 0.3f);
            shapeRenderer.rect(btnX, btnY, btnW, 2f); // shadow line
        }
        
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        
        // Quest Main Header Title
        questTitleFont.setColor(Color.YELLOW);
        questTitleFont.draw(spriteBatch, "QUEST", 570, 642);
        
        // Tab Text Daily
        if (activeQuestTab.equals("Daily")) {
            questHeaderFont.setColor(Color.YELLOW);
        } else {
            questHeaderFont.setColor(Color.LIGHT_GRAY);
        }
        questHeaderFont.draw(spriteBatch, "DAILY", 330, 602);
        
        // Tab Text Achievements
        if (activeQuestTab.equals("Achievements")) {
            questHeaderFont.setColor(Color.YELLOW);
        } else {
            questHeaderFont.setColor(Color.LIGHT_GRAY);
        }
        questHeaderFont.draw(spriteBatch, "ACHIEVEMENTS", 500, 602);
        
        // Close cross X text
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "X", 945, 626);
        
        // Draw Cards Text details
        for (int i = 0; i < Math.min(3, filteredQuests.size); i++) {
            Quest q = filteredQuests.get(i);
            float itemY = 450f - i * 110f;
            
            // Quest Title
            questHeaderFont.setColor(Color.BLACK);
            questHeaderFont.draw(spriteBatch, (i+1) + ". " + q.getTitle(), 296f, itemY + 80f);
            
            // Quest Description
            questDescFont.setColor(Color.DARK_GRAY);
            questDescFont.draw(spriteBatch, q.getDescription(), 296f, itemY + 58f);
            
            // Progress Bar Text
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, q.getProgress() + "/" + q.getTarget(), 410f, itemY + 28f);
            
            // Reward Amount
            font.setColor(Color.BLACK);
            font.draw(spriteBatch, "x" + q.getRewardAmount(), 635f, itemY + 41f);
            
            // Action Button Text
            String btnText = "GO NOW";
            if (q.isCompleted() && !q.isClaimed()) {
                btnText = "CLAIM";
                font.setColor(Color.YELLOW);
            } else if (q.isClaimed()) {
                btnText = "CLAIMED";
                font.setColor(Color.LIGHT_GRAY);
            } else {
                font.setColor(Color.WHITE);
            }
            float btnX = questActionBtnRects[i].x;
            font.draw(spriteBatch, btnText, btnX + 15f, itemY + 42f);
        }
        
        spriteBatch.end();
    }

    private void handleQuestUiClicks() {
        if (!Gdx.input.justTouched()) return;

        com.badlogic.gdx.math.Vector3 clickPoint = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        uiCamera.unproject(clickPoint, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());

        // Close button
        if (closeBtnRect.contains(clickPoint.x, clickPoint.y)) {
            isQuestUiOpen = false;
            return;
        }

        // Tabs
        if (tabDailyRect.contains(clickPoint.x, clickPoint.y)) {
            activeQuestTab = "Daily";
            return;
        }
        if (tabAchievementsRect.contains(clickPoint.x, clickPoint.y)) {
            activeQuestTab = "Achievements";
            return;
        }

        // Action buttons
        Array<Quest> filteredQuests = new Array<>();
        for (Quest q : game.getQuests()) {
            if (q.getTab().equalsIgnoreCase(activeQuestTab)) {
                filteredQuests.add(q);
            }
        }

        for (int i = 0; i < Math.min(3, filteredQuests.size); i++) {
            Quest q = filteredQuests.get(i);
            if (questActionBtnRects[i].contains(clickPoint.x, clickPoint.y)) {
                if (q.isCompleted() && !q.isClaimed()) {
                    q.setClaimed(true);
                    if (q.getRewardType().equals("Gems")) {
                        game.addGems(q.getRewardAmount());
                    } else if (q.getRewardType().equals("Gold")) {
                        game.addGold(q.getRewardAmount());
                    }
                    floatingTexts.add(new FloatingText("+" + q.getRewardAmount() + " " + q.getRewardType(), clickPoint.x, clickPoint.y, Color.GREEN));
                } else if (!q.isCompleted()) {
                    // "Go Now" -> close menu
                    isQuestUiOpen = false;
                }
            }
        }
    }

    private void drawFloatingTexts(float delta) {
        spriteBatch.begin();
        spriteBatch.setProjectionMatrix(uiCamera.combined);
        for (int i = floatingTexts.size - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.timer -= delta;
            ft.y += 40f * delta; // rise upwards
            if (ft.timer <= 0) {
                floatingTexts.removeIndex(i);
            } else {
                font.setColor(ft.color.r, ft.color.g, ft.color.b, ft.timer / 1.5f);
                font.draw(spriteBatch, ft.text, ft.x, ft.y);
            }
        }
        spriteBatch.end();
    }

    private void drawBannerNotification(float delta) {
        // Check for new completed quests to trigger banner
        for (Quest q : game.getQuests()) {
            if (q.isCompleted() && !completedBannersShown.contains(q.getId(), false)) {
                completedBannersShown.add(q.getId());
                bannerText = "QUEST SELESAI: " + q.getTitle();
                bannerTimer = 3.0f;
                bannerAnimY = 832f; // Start from top
            }
        }

        if (bannerTimer > 0) {
            bannerTimer -= delta;
            float bannerW = 650f;
            float bannerH = 50f;
            float bannerX = (1253f - bannerW) / 2f;
            
            // Slide animation from top (Y=832) to Y=700
            float targetY = 700f;
            if (bannerTimer > 2.5f) {
                float progress = (3.0f - bannerTimer) / 0.5f;
                bannerAnimY = 832f - (832f - targetY) * progress;
            } else if (bannerTimer < 0.5f) {
                float progress = bannerTimer / 0.5f;
                bannerAnimY = 832f - (832f - targetY) * progress;
            } else {
                bannerAnimY = targetY;
            }
            
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            
            shapeRenderer.setColor(16f / 255f, 22f / 255f, 38f / 255f, 0.95f);
            shapeRenderer.rect(bannerX, bannerAnimY, bannerW, bannerH);
            
            shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f);
            shapeRenderer.rect(bannerX, bannerAnimY, bannerW, 2f);
            shapeRenderer.rect(bannerX, bannerAnimY + bannerH - 2f, bannerW, 2f);
            shapeRenderer.rect(bannerX, bannerAnimY, 2f, bannerH);
            shapeRenderer.rect(bannerX + bannerW - 2f, bannerAnimY, 2f, bannerH);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            
            spriteBatch.setProjectionMatrix(uiCamera.combined);
            spriteBatch.begin();
            questHeaderFont.setColor(Color.YELLOW);
            questHeaderFont.draw(spriteBatch, bannerText, bannerX, bannerAnimY + 32f, bannerW, com.badlogic.gdx.utils.Align.center, false);
            spriteBatch.end();
        }
    }

    private static class FloatingText {
        final String text;
        float x;
        float y;
        float timer;
        final Color color;

        FloatingText(String text, float x, float y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.timer = 1.5f;
            this.color = color;
        }
    }

    @Override
    public void dispose() {
        if (gameMenu != null) {
            gameMenu.dispose();
        }
        spriteBatch.dispose();
        shapeRenderer.dispose();
        renderer.dispose();
        map.dispose();
        for (Texture t : allTextures) t.dispose();
        if (font != null)       font.dispose();
        if (headerFont != null) headerFont.dispose();
        if (questTitleFont != null)  questTitleFont.dispose();
        if (questHeaderFont != null) questHeaderFont.dispose();
        if (questDescFont != null)   questDescFont.dispose();
        isDisposed = true;
    }
}
