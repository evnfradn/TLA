package com.mygame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ExplorationScreen implements Screen {

    private final TheLastAncestorsGame game;

    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Tiled Map
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private TiledMapTileLayer collisionLayer;
    private int mapWidth;
    private int mapHeight;

    private float playerX = 877f;
    private float playerY = 447f;
    private static final float MOVEMENT_SPEED = 120f; // px per second

    // Player animations
    private Animation<TextureRegion> walkNorth;
    private Animation<TextureRegion> walkEast;
    private Animation<TextureRegion> walkSouth;
    private Animation<TextureRegion> walkWest;

    private Animation<TextureRegion> idleNorth;
    private Animation<TextureRegion> idleEast;
    private Animation<TextureRegion> idleSouth;
    private Animation<TextureRegion> idleWest;

    private enum Direction {
        NORTH, EAST, SOUTH, WEST
    }

    private Direction currentDirection = Direction.SOUTH;
    private boolean isMoving = false;
    private float stateTime = 0f;

    private Array<Texture> allTextures = new Array<>();
    private BitmapFont font;

    // Battle trigger states
    private boolean showBattlePrompt = false;
    private boolean inTriggerZone = false;
    private boolean promptCooldown = false;

    public ExplorationScreen(TheLastAncestorsGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        spriteBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();

        // Viewport 1253x832, tapi kita zoom in kameranya (zoom = 0.5f)
        // untuk mendapatkan sensasi grafis pixel-art retro premium yang pas.
        viewport = new FitViewport(1253, 832, camera);
        camera.zoom = 0.5f;

        // Load Tiled Map (.tmx)
        map = new TmxMapLoader().load("Eksplore/Classroom/Classroom_Map.tmx");
        renderer = new OrthogonalTiledMapRenderer(map);

        // Ambil info ukuran map
        MapProperties prop = map.getProperties();
        mapWidth = prop.get("width", Integer.class);
        mapHeight = prop.get("height", Integer.class);

        // Ambil Collision Layer
        collisionLayer = (TiledMapTileLayer) map.getLayers().get("Collision");
        if (collisionLayer != null) {
            collisionLayer.setVisible(false);
        }

        // Load Deo animations
        walkNorth = loadWalkAnimation("North");
        walkEast = loadWalkAnimation("East");
        walkSouth = loadWalkAnimation("South");
        walkWest = loadWalkAnimation("West");

        idleNorth = loadIdleAnimation("North");
        idleEast = loadIdleAnimation("East");
        idleSouth = loadIdleAnimation("South");
        idleWest = loadIdleAnimation("West");

        // Generate retro pixel font for coordinate display
        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("Battle/PressStart2P.ttf"));
        FreeTypeFontParameter fontParameter = new FreeTypeFontParameter();
        fontParameter.borderWidth = 1.0f;
        fontParameter.borderColor = Color.BLACK;
        fontParameter.size = 10;
        font = fontGenerator.generateFont(fontParameter);
        fontGenerator.dispose();
    }

    private Animation<TextureRegion> loadWalkAnimation(String direction) {
        TextureRegion[] frames = new TextureRegion[9];
        for (int i = 1; i <= 9; i++) {
            String path = "Eksplore/ASWD/" + direction + "/" + direction + "/" + direction + "_" + i + ".png";
            Texture tex = new Texture(Gdx.files.internal(path));
            allTextures.add(tex);
            frames[i - 1] = new TextureRegion(tex);
        }
        Animation<TextureRegion> anim = new Animation<>(0.08f, frames);
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }

    private Animation<TextureRegion> loadIdleAnimation(String direction) {
        TextureRegion[] frames = new TextureRegion[2];
        for (int i = 1; i <= 2; i++) {
            String path = "Eksplore/ASWD/" + direction + "/Idle_" + direction + "/Idle_" + direction + "_" + i + ".png";
            Texture tex = new Texture(Gdx.files.internal(path));
            allTextures.add(tex);
            frames[i - 1] = new TextureRegion(tex);
        }
        Animation<TextureRegion> anim = new Animation<>(0.25f, frames);
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }

    @Override
    public void render(float delta) {
        stateTime += delta;

        // 1. Proses input keyboard & pergerakan
        handleInput(delta);

        // 2. Bersihkan screen
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        // 3. Update kamera & posisi render
        updateCamera();
        renderer.setView(camera);

        if (collisionLayer != null) {
            collisionLayer.setVisible(false);
        }

        TiledMapTileLayer chairLayer = (TiledMapTileLayer) map.getLayers().get("Chair");
        int chairLayerIndex = map.getLayers().getIndex("Chair");
        boolean overChair = isOverlappingChair();

        if (overChair && chairLayer != null && chairLayerIndex != -1) {
            // Sembunyikan layer "Chair" terlebih dahulu saat render peta dasar
            chairLayer.setVisible(false);
            renderer.render();

            // 4. Render Karakter Deo di atas peta dasar
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
            TextureRegion currentFrame = getPlayerFrame();
            spriteBatch.draw(currentFrame, playerX, playerY, 84f, 84f);
            drawCoordinates();
            spriteBatch.end();

            // Render layer "Chair" di atas player!
            chairLayer.setVisible(true);
            renderer.render(new int[]{ chairLayerIndex });
        } else {
            // Tampilkan normal (Chair di bawah player)
            if (chairLayer != null) {
                chairLayer.setVisible(true);
            }
            renderer.render();

            // 4. Render Karakter Deo di atas semuanya
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
            TextureRegion currentFrame = getPlayerFrame();
            spriteBatch.draw(currentFrame, playerX, playerY, 84f, 84f);
            drawCoordinates();
            spriteBatch.end();
        }

        // 5. Render Dialog Battle Prompt jika aktif
        if (showBattlePrompt) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.05f, 0.05f, 0.1f, 0.9f); // Background gelap transparan
            shapeRenderer.rect(camera.position.x - 170f, camera.position.y - 65f, 340f, 130f);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1.0f, 0.8f, 0.0f, 1.0f); // Border emas
            shapeRenderer.rect(camera.position.x - 170f, camera.position.y - 65f, 340f, 130f);
            shapeRenderer.setColor(1.0f, 1.0f, 1.0f, 0.2f); // Border putih dalam tipis
            shapeRenderer.rect(camera.position.x - 167f, camera.position.y - 62f, 334f, 124f);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            spriteBatch.begin();
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "Anda akan memasuki mode battle", camera.position.x - 150f, camera.position.y + 35f);
            font.draw(spriteBatch, "Lanjut/Batal?", camera.position.x - 60f, camera.position.y + 15f);

            font.setColor(Color.GREEN);
            font.draw(spriteBatch, "[1] / ENTER - Lanjut", camera.position.x - 110f, camera.position.y - 15f);

            font.setColor(Color.RED);
            font.draw(spriteBatch, "[2] / ESC   - Batal", camera.position.x - 110f, camera.position.y - 35f);
            spriteBatch.end();
        }
    }


    private void handleInput(float delta) {
        // Cek input prompt dialog pertarungan terlebih dahulu jika aktif
        if (showBattlePrompt) {
            isMoving = false;
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
                showBattlePrompt = false;
                game.setScreen(new BattleScreen(game));
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)
                    || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
                showBattlePrompt = false;
                promptCooldown = true; // Kunci pemicuan sampai keluar area
            }
            return; // Kunci pergerakan karakter saat dialog aktif
        }

        // Cek pemicuan koordinat (X: 139 Y: 478)
        float targetX = 139f;
        float targetY = 478f;
        float dist = (float) Math.hypot(playerX - targetX, playerY - targetY);

        if (dist < 24f) { // Radius pemicuan 24 piksel
            if (!inTriggerZone && !promptCooldown) {
                inTriggerZone = true;
                showBattlePrompt = true;
                isMoving = false;
                return;
            }
        } else if (dist > 40f) { // Reset cooldown saat berjalan menjauh
            inTriggerZone = false;
            promptCooldown = false;
        }

        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            dy = MOVEMENT_SPEED * delta;
            currentDirection = Direction.NORTH;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            dy = -MOVEMENT_SPEED * delta;
            currentDirection = Direction.SOUTH;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            dx = MOVEMENT_SPEED * delta;
            currentDirection = Direction.EAST;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            dx = -MOVEMENT_SPEED * delta;
            currentDirection = Direction.WEST;
        }

        isMoving = (dx != 0f || dy != 0f);

        if (isMoving) {
            // Coba pergerakan secara X dan Y dengan deteksi tabrakan
            float newX = playerX + dx;
            float newY = playerY + dy;

            // X-movement check
            if (!isColliding(newX, playerY)) {
                playerX = newX;
            }

            // Y-movement check
            if (!isColliding(playerX, newY)) {
                playerY = newY;
            }
        }
    }

    private boolean isColliding(float x, float y) {
        // Kotak pembatas tabrakan Deo diletakkan hanya pada bagian telapak kaki agar
        // sangat kecil
        // dan mudah melewati celah/koridor sempit berukuran 32px (1 tile).
        float colWidth = 12f; // Lebar kecil agar muat di koridor 32px dengan toleransi besar
        float colHeight = 24f; // Tinggi kecil di area telapak kaki saja
        float offsetX = (84f - colWidth) / 2f; // Center horizontal terhadap ukuran visual 84f
        float offsetY = 4f; // Offset dari bawah kaki karakter

        // Cek 4 titik sudut dari kotak pembatas
        float[][] corners = {
                { x + offsetX, y + offsetY },
                { x + offsetX + colWidth, y + offsetY },
                { x + offsetX, y + offsetY + colHeight },
                { x + offsetX + colWidth, y + offsetY + colHeight }
        };

        float mapWidthPixels = mapWidth * 32f;
        float mapHeightPixels = mapHeight * 32f;

        for (float[] p : corners) {
            // Di luar batas map eksplorasi dianggap menabrak
            if (p[0] < 0 || p[0] >= mapWidthPixels || p[1] < 0 || p[1] >= mapHeightPixels) {
                return true;
            }

            int tileX = (int) (p[0] / 32f);
            int tileY = (int) (p[1] / 32f);

            // Jika layer Collision memiliki cell aktif pada posisi tile tersebut
            if (collisionLayer != null && collisionLayer.getCell(tileX, tileY) != null) {
                return true;
            }
        }
        return false;
    }

    private void updateCamera() {
        // Map total pixels
        float mapWidthPixels = mapWidth * 32f;
        float mapHeightPixels = mapHeight * 32f;

        // Bounded camera dimensions
        float viewWidth = viewport.getWorldWidth() * camera.zoom;
        float viewHeight = viewport.getWorldHeight() * camera.zoom;

        // Batasi player agar tidak pernah keluar peta
        playerX = Math.max(0, Math.min(playerX, mapWidthPixels - 32));
        playerY = Math.max(0, Math.min(playerY, mapHeightPixels - 32));

        // Letakkan kamera di titik tengah Deo
        float camX = playerX + 16f;
        float camY = playerY + 16f;

        // Bounded Camera agar tidak melihat area kosong hitam di luar map
        float minCamX = viewWidth / 2f;
        float maxCamX = mapWidthPixels - minCamX;
        float minCamY = viewHeight / 2f;
        float maxCamY = mapHeightPixels - minCamY;

        if (mapWidthPixels < viewWidth) {
            camera.position.x = mapWidthPixels / 2f;
        } else {
            camera.position.x = Math.max(minCamX, Math.min(camX, maxCamX));
        }

        if (mapHeightPixels < viewHeight) {
            camera.position.y = mapHeightPixels / 2f;
        } else {
            camera.position.y = Math.max(minCamY, Math.min(camY, maxCamY));
        }

        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    private TextureRegion getPlayerFrame() {
        TextureRegion currentFrame;
        if (isMoving) {
            switch (currentDirection) {
                case NORTH: currentFrame = walkNorth.getKeyFrame(stateTime); break;
                case EAST: currentFrame = walkEast.getKeyFrame(stateTime); break;
                case WEST: currentFrame = walkWest.getKeyFrame(stateTime); break;
                case SOUTH: default: currentFrame = walkSouth.getKeyFrame(stateTime); break;
            }
        } else {
            switch (currentDirection) {
                case NORTH: currentFrame = idleNorth.getKeyFrame(stateTime); break;
                case EAST: currentFrame = idleEast.getKeyFrame(stateTime); break;
                case WEST: currentFrame = idleWest.getKeyFrame(stateTime); break;
                case SOUTH: default: currentFrame = idleSouth.getKeyFrame(stateTime); break;
            }
        }
        return currentFrame;
    }

    private void drawCoordinates() {
        float viewWidth = viewport.getWorldWidth() * camera.zoom;
        float viewHeight = viewport.getWorldHeight() * camera.zoom;
        float textX = camera.position.x - viewWidth / 2f + 15f;
        float textY = camera.position.y + viewHeight / 2f - 15f;
        
        font.setColor(Color.YELLOW);
        font.draw(spriteBatch, "X: " + (int) playerX + "  Y: " + (int) playerY, textX, textY);
    }

    private boolean isOverlappingChair() {
        TiledMapTileLayer chairLayer = (TiledMapTileLayer) map.getLayers().get("Chair");
        if (chairLayer == null) return false;

        // Cek titik sudut dari badan Deo (menggunakan bounding box badan Deo)
        float colWidth = 24f;
        float colHeight = 24f;
        float offsetX = (84f - colWidth) / 2f;
        float offsetY = 12f;

        float[][] points = {
            { playerX + offsetX, playerY + offsetY },
            { playerX + offsetX + colWidth, playerY + offsetY },
            { playerX + offsetX, playerY + offsetY + colHeight },
            { playerX + offsetX + colWidth, playerY + offsetY + colHeight }
        };

        for (float[] p : points) {
            int tileX = (int) (p[0] / 32f);
            int tileY = (int) (p[1] / 32f);
            if (tileX >= 0 && tileX < mapWidth && tileY >= 0 && tileY < mapHeight) {
                if (chairLayer.getCell(tileX, tileY) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        shapeRenderer.dispose();
        renderer.dispose();
        map.dispose();
        for (Texture tex : allTextures) {
            tex.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
