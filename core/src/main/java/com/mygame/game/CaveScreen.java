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

        gen.dispose();

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

        // ── HUD (location label) ───────────────────────────────────────────
        com.badlogic.gdx.math.Matrix4 uiProj = new com.badlogic.gdx.math.Matrix4();
        uiProj.setToOrtho2D(0, 0, 1253, 832);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiProj);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.04f, 0.06f, 0.12f, 0.88f);
        shapeRenderer.rect(0, 792, 1253, 40);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.setProjectionMatrix(uiProj);
        spriteBatch.begin();
        headerFont.setColor(new Color(0.6f, 0.8f, 1f, 1f));
        headerFont.draw(spriteBatch, "♦  GUA MISTERIUS  ♦", 420f, 822f);

        // Draw coordinates in UI space (top left, just below HUD bar)
        font.setColor(Color.YELLOW);
        font.draw(spriteBatch, "X: " + (int)playerX + "  Y: " + (int)playerY, 20f, 770f);
        spriteBatch.end();

        // ── Exit Confirm Popup ─────────────────────────────────────────────
        if (showExitConfirm) {
            drawExitConfirmPopup();
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
            }
            if (devConsole.consumeBattle()) {
                game.setScreen(new BattleScreen(game));
                this.dispose();
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
                float mx = Gdx.input.getX();
                float my = 832f - Gdx.input.getY();

                // box dimensions: boxW = 500, boxH = 200
                // boxX = (1253 - 500) / 2 = 376.5f
                // boxY = (832 - 200) / 2 = 316f
                // YES button: boxX + 50f to boxX + 200f => 426.5f to 576.5f
                // NO button: boxX + 300f to boxX + 450f => 676.5f to 826.5f
                // Button Y: boxY + 40f to boxY + 80f => 356f to 396f

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
            case SAVE:
                GameSave.save("cave", playerX, playerY, game.getGold(), game.getGems());
                showSaveToast = true;
                saveToastTimer = 2.5f;
                break;
            case EXIT:
                Gdx.app.exit();
                break;
            case CONTINUE:
            case NONE:
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

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(16f / 255f, 22f / 255f, 38f / 255f, 0.98f);
        shapeRenderer.rect(boxX, boxY, boxW, boxH);
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.rect(boxX, boxY, boxW, 3f);
        shapeRenderer.rect(boxX, boxY + boxH - 3f, boxW, 3f);
        shapeRenderer.rect(boxX, boxY, 3f, boxH);
        shapeRenderer.rect(boxX + boxW - 3f, boxY, 3f, boxH);

        // Hover effect on buttons
        float mx = Gdx.input.getX();
        float my = 832f - Gdx.input.getY();

        boolean hoverYes = (mx >= boxX + 50f && mx <= boxX + 200f && my >= boxY + 40f && my <= boxY + 80f);
        shapeRenderer.setColor(hoverYes ? new Color(0f, 0.6f, 0f, 1f) : new Color(0f, 0.4f, 0f, 1f));
        shapeRenderer.rect(boxX + 50f, boxY + 40f, 150f, 40f);

        boolean hoverNo = (mx >= boxX + boxW - 200f && mx <= boxX + boxW - 50f && my >= boxY + 40f && my <= boxY + 80f);
        shapeRenderer.setColor(hoverNo ? new Color(0.7f, 0f, 0f, 1f) : new Color(0.5f, 0f, 0f, 1f));
        shapeRenderer.rect(boxX + boxW - 200f, boxY + 40f, 150f, 40f);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        headerFont.setColor(Color.WHITE);
        headerFont.draw(spriteBatch, "Coba pergi keluar Goa?", boxX, boxY + boxH - 50f, boxW, com.badlogic.gdx.utils.Align.center, false);

        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "Yes", boxX + 50f, boxY + 65f, 150f, com.badlogic.gdx.utils.Align.center, false);
        font.draw(spriteBatch, "No", boxX + boxW - 200f, boxY + 65f, 150f, com.badlogic.gdx.utils.Align.center, false);
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

    @Override
    public void dispose() {
        spriteBatch.dispose();
        shapeRenderer.dispose();
        renderer.dispose();
        map.dispose();
        for (Texture t : allTextures) t.dispose();
        if (font != null)       font.dispose();
        if (headerFont != null) headerFont.dispose();
        isDisposed = true;
    }
}
