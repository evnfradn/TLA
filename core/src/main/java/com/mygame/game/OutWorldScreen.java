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
 * OutWorldScreen — layar dunia luar yang dituju setelah keluar dari gua.
 *
 * Spawn: dekat gerbang kiri, X: 32, Y: 277.
 * Map: Eksplore/AssetOutworld/OutWorld_Map.tmx
 */
public class OutWorldScreen implements Screen {

    // ── Referensi utama ──────────────────────────────────────────────────────
    private final TheLastAncestorsGame game;

    // ── Rendering ────────────────────────────────────────────────────────────
    private SpriteBatch     spriteBatch;
    private ShapeRenderer   shapeRenderer;
    private OrthographicCamera camera;
    private Viewport        viewport;
    private OrthographicCamera uiCamera;

    // ── Tiled Map ─────────────────────────────────────────────────────────────
    private TiledMap                  map;
    private OrthogonalTiledMapRenderer renderer;
    private int mapWidth, mapHeight;

    // OutWorld layers
    private TiledMapTileLayer         groundLayer;
    private TiledMapTileLayer         pathLayer;
    private TiledMapTileLayer         plateu1Layer;
    private TiledMapTileLayer         plateu2Layer;
    private TiledMapTileLayer         plateu3Layer;
    private TiledMapTileLayer         plantLayer;
    private TiledMapTileLayer         treesLayer;
    private TiledMapTileLayer         collisionLayer;

    // ── Player ────────────────────────────────────────────────────────────────
    // Spawn: X: 687f, Y: 291f
    private float playerX = 687f;
    private float playerY = 291f;
    private static final float SPEED = 120f;

    private Animation<TextureRegion> walkNorth, walkEast, walkSouth, walkWest;
    private Animation<TextureRegion> idleNorth, idleEast, idleSouth, idleWest;

    private enum Direction { NORTH, EAST, SOUTH, WEST }
    private Direction currentDirection = Direction.EAST; // spawn facing east
    private boolean   isMoving   = false;
    private float     stateTime  = 0f;

    // ── Fonts & Textures ─────────────────────────────────────────────────────
    private BitmapFont font;
    private BitmapFont headerFont;
    private Array<Texture> allTextures = new Array<>();

    // ── Game Menu ─────────────────────────────────────────────────────────────
    private GameMenu gameMenu;

    // ── Fade-in dari transition ───────────────────────────────────────────────
    private float fadeInAlpha = 1f; // starts white, fades to 0

    // ── Save toast ────────────────────────────────────────────────────────────
    private boolean showSaveToast = false;
    private float   saveToastTimer = 0f;

    // ── Dev console ───────────────────────────────────────────────────────────
    private DevConsole devConsole;

    // =========================================================================
    public OutWorldScreen(TheLastAncestorsGame game) {
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

        // Load outworld map
        map      = new TmxMapLoader().load("Eksplore/AssetOutworld/OutWorld_Map.tmx");
        renderer = new OrthogonalTiledMapRenderer(map);

        MapProperties prop = map.getProperties();
        mapWidth  = prop.get("width",  Integer.class);
        mapHeight = prop.get("height", Integer.class);

        // Get OutWorld layers
        groundLayer  = (TiledMapTileLayer) map.getLayers().get("Ground");
        pathLayer    = (TiledMapTileLayer) map.getLayers().get("Path");
        plateu1Layer = (TiledMapTileLayer) map.getLayers().get("Plateu 1");
        plateu2Layer = (TiledMapTileLayer) map.getLayers().get("Plateu 2");
        plateu3Layer = (TiledMapTileLayer) map.getLayers().get("Plateu 3");
        plantLayer   = (TiledMapTileLayer) map.getLayers().get("Plant");
        treesLayer   = (TiledMapTileLayer) map.getLayers().get("Trees");
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
        devConsole = new DevConsole();
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
        stateTime += delta;

        // ── Fade-in ────────────────────────────────────────────────────────
        if (fadeInAlpha > 0f) fadeInAlpha = Math.max(0f, fadeInAlpha - delta * 1.5f);

        // ── Input ──────────────────────────────────────────────────────────
        if (gameMenu.isOpen()) {
            GameMenu.Action action = gameMenu.handleInput();
            handleMenuAction(action);
        } else {
            handleInput(delta);
        }

        // ── Clear & render map ─────────────────────────────────────────────
        ScreenUtils.clear(0.15f, 0.28f, 0.45f, 1f); // Sky blue themed clearing color
        updateCamera();
        renderer.setView(camera);

        // Pass 1: Render background layers (Ground, Path, Plateus, Plants) underneath player
        if (groundLayer != null) groundLayer.setVisible(true);
        if (pathLayer != null) pathLayer.setVisible(true);
        if (plateu1Layer != null) plateu1Layer.setVisible(true);
        if (plateu2Layer != null) plateu2Layer.setVisible(true);
        if (plateu3Layer != null) plateu3Layer.setVisible(true);
        if (plantLayer != null) plantLayer.setVisible(true);
        if (treesLayer != null) treesLayer.setVisible(false);
        if (collisionLayer != null) collisionLayer.setVisible(false);
        renderer.render();

        // ── Render player ──────────────────────────────────────────────────
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        spriteBatch.draw(getPlayerFrame(), playerX, playerY, 84f, 84f);
        spriteBatch.end();

        // Pass 2: Render foreground layer (Trees) on top of the player
        if (groundLayer != null) groundLayer.setVisible(false);
        if (pathLayer != null) pathLayer.setVisible(false);
        if (plateu1Layer != null) plateu1Layer.setVisible(false);
        if (plateu2Layer != null) plateu2Layer.setVisible(false);
        if (plateu3Layer != null) plateu3Layer.setVisible(false);
        if (plantLayer != null) plantLayer.setVisible(false);
        if (treesLayer != null) treesLayer.setVisible(true);
        renderer.render();

        // ── HUD (location label) ───────────────────────────────────────────
        com.badlogic.gdx.math.Matrix4 uiProj = new com.badlogic.gdx.math.Matrix4();
        uiProj.setToOrtho2D(0, 0, 1253, 832);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiProj);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.04f, 0.12f, 0.08f, 0.88f); // Dark forest green HUD themed bar
        shapeRenderer.rect(0, 792, 1253, 40);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.setProjectionMatrix(uiProj);
        spriteBatch.begin();
        headerFont.setColor(new Color(0.7f, 1.0f, 0.8f, 1f));
        headerFont.draw(spriteBatch, "♦  OUTWORLD  ♦", 500f, 822f);

        // Draw coordinates in UI space (top left, just below HUD bar)
        font.setColor(Color.YELLOW);
        font.draw(spriteBatch, "X: " + (int)playerX + "  Y: " + (int)playerY, 20f, 770f);
        spriteBatch.end();

        // ── Game Menu ─────────────────────────────────────────────────────
        if (gameMenu.isOpen()) {
            gameMenu.render(spriteBatch, shapeRenderer, delta);
        }

        // ── Save toast ────────────────────────────────────────────────────
        if (showSaveToast) drawSaveToast(delta);

        // ── White fade-in ─────────────────────────────────────────────────
        if (fadeInAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(uiProj);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1f, 1f, 1f, fadeInAlpha);
            shapeRenderer.rect(0, 0, 1253, 832);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    private void handleInput(float delta) {
        // Trigger battle screen at X: 289, Y: 156
        if (Math.abs(playerX - 289f) < 15f && Math.abs(playerY - 156f) < 15f) {
            game.setScreen(new BattleScreen(game));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            gameMenu.toggle();
            return;
        }

        devConsole.handleInput();

        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    { dy =  SPEED * delta; currentDirection = Direction.NORTH; }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))   { dy = -SPEED * delta; currentDirection = Direction.SOUTH; }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))  { dx =  SPEED * delta; currentDirection = Direction.EAST;  }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))   { dx = -SPEED * delta; currentDirection = Direction.WEST;  }

        isMoving = (dx != 0 || dy != 0);
        if (isMoving) {
            if (!isColliding(playerX + dx, playerY)) playerX += dx;
            if (!isColliding(playerX, playerY + dy)) playerY += dy;
        }
    }

    private void handleMenuAction(GameMenu.Action action) {
        switch (action) {
            case SAVE:
                GameSave.save("outworld", playerX, playerY, game.getGold(), game.getGems());
                showSaveToast = true;
                saveToastTimer = 2.5f;
                break;
            case EXIT:
                Gdx.app.exit();
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
    }
}
