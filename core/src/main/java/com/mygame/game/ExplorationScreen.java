package com.mygame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

public class ExplorationScreen implements Screen {

    private final TheLastAncestorsGame game;
    private boolean isDisposed = false;

    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Tiled Map
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private TiledMapTileLayer collisionLayer;
    private TiledMapTileLayer chairLayer;
    private TiledMapTileLayer tableLayer;
    private TiledMapTileLayer table2Layer;
    private TiledMapTileLayer table3Layer;
    private TiledMapTileLayer table4Layer;
    private TiledMapTileLayer propsLayer;
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

    // Dev Console (cheat terminal)
    private DevConsole devConsole;

    // NPCs
    private Array<NPC> npcs;

    // Dialogue System
    private boolean isDialogueActive = false;
    private Array<DialogueLine> dialogueLines;
    private int currentDialogueIndex = 0;
    private float dialogueTypingTimer = 0f;
    private int dialogueCharIndex = 0;
    private static final float TYPING_SPEED = 0.03f;
    private Texture dialogBoxImage;
    private Animation<TextureRegion> teacherLeftAnimation;
    private BitmapFont dialogueFont;
    private BitmapFont nameFont;

    // UI Quest system fields
    private OrthographicCamera uiCamera;
    private BitmapFont questTitleFont;
    private BitmapFont questHeaderFont;
    private BitmapFont questDescFont;
    private boolean isQuestUiOpen = false;
    private String activeQuestTab = "Daily";
    private Array<String> interactedNpcNames = new Array<>();
    
    private Rectangle questBtnRect = new Rectangle(1090, 784, 140, 36);
    private Rectangle closeBtnRect = new Rectangle(935, 605, 30, 30);
    private Rectangle tabDailyRect = new Rectangle(281, 576, 180, 36);
    private Rectangle tabAchievementsRect = new Rectangle(476, 576, 220, 36);
    private Rectangle[] questActionBtnRects = {
        new Rectangle(840, 450 + 20, 110, 36),
        new Rectangle(840, 340 + 20, 110, 36),
        new Rectangle(840, 230 + 20, 110, 36)
    };
    
    private String bannerText = "";
    private float bannerTimer = 0f;
    private float bannerAnimY = 0f;
    private Array<FloatingText> floatingTexts = new Array<>();
    private Array<String> completedBannersShown = new Array<>();

    // ── Window Bang Cutscene ──────────────────────────────────────────────────
    private enum CutsceneState {
        NONE,
        SITTING_DELAY,    // 2 detik setelah duduk
        KRAKKK_SHOW,      // tampilkan kotak suara + flashbang
        NPC_LOOK_PHASE,   // semua NPC menoleh ke jendela
        WALK_TO_WINDOW,   // player digerakkan user ke jendela
        WINDOW_POPUP,     // popup "apakah anda ingin melihat?"
        FADE_TO_CAVE      // transisi putih ke cave
    }
    private CutsceneState cutsceneState = CutsceneState.NONE;
    private float cutsceneTimer    = 0f;
    private float flashbangAlpha   = 0f; // efek silau putih (0=transparan, 1=putih penuh)
    private float fadeOutAlpha     = 0f; // white screen fade untuk transisi ke cave
    private boolean krakkkBoxShown = false;

    // Posisi jendela di kelas (sisi kiri, area tirai)
    private static final float WINDOW_TARGET_X = 64f;
    private static final float WINDOW_TARGET_Y = 130f;
    private static final float WINDOW_REACH_DIST = 80f;

    // ── Game Menu (Pause) ────────────────────────────────────────────────────
    private GameMenu gameMenu;
    private BitmapFont menuTitleFont;
    private boolean showSaveToast = false;
    private float   saveToastTimer = 0f;

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

        // Ambil Layer untuk rendering atas/bawah player
        chairLayer = (TiledMapTileLayer) map.getLayers().get("Chair");
        tableLayer = (TiledMapTileLayer) map.getLayers().get("Table");
        if (tableLayer == null) {
            tableLayer = (TiledMapTileLayer) map.getLayers().get("Table_1");
        }
        table2Layer = (TiledMapTileLayer) map.getLayers().get("Table_2");
        table3Layer = (TiledMapTileLayer) map.getLayers().get("Table_3");
        table4Layer = (TiledMapTileLayer) map.getLayers().get("Table_4");
        propsLayer = (TiledMapTileLayer) map.getLayers().get("Props");

        // Load Deo animations
        walkNorth = loadWalkAnimation("North");
        walkEast = loadWalkAnimation("East");
        walkSouth = loadWalkAnimation("South");
        walkWest = loadWalkAnimation("West");

        idleNorth = loadIdleAnimation("North");
        idleEast = loadIdleAnimation("East");
        idleSouth = loadIdleAnimation("South");
        idleWest = loadIdleAnimation("West");

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, 1253, 832);

        if (GameSave.hasSave() && "classroom".equalsIgnoreCase(GameSave.loadMap())) {
            playerX = GameSave.loadX();
            playerY = GameSave.loadY();
        }

        // Generate retro pixel font for coordinate display and dialogue UI
        FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("Battle/PressStart2P.ttf"));
        FreeTypeFontParameter fontParameter = new FreeTypeFontParameter();
        fontParameter.borderWidth = 1.0f;
        fontParameter.borderColor = Color.BLACK;
        fontParameter.size = 10;
        font = fontGenerator.generateFont(fontParameter);

        FreeTypeFontParameter dialogueParam = new FreeTypeFontParameter();
        dialogueParam.borderWidth = 1.5f;
        dialogueParam.borderColor = Color.BLACK;
        dialogueParam.size = 12;
        dialogueFont = fontGenerator.generateFont(dialogueParam);

        FreeTypeFontParameter nameParam = new FreeTypeFontParameter();
        nameParam.borderWidth = 1.2f;
        nameParam.borderColor = Color.BLACK;
        nameParam.size = 10;
        nameFont = fontGenerator.generateFont(nameParam);

        FreeTypeFontParameter questTitleParam = new FreeTypeFontParameter();
        questTitleParam.borderWidth = 2.0f;
        questTitleParam.borderColor = Color.BLACK;
        questTitleParam.size = 18;
        questTitleFont = fontGenerator.generateFont(questTitleParam);

        FreeTypeFontParameter questHeaderParam = new FreeTypeFontParameter();
        questHeaderParam.borderWidth = 1.5f;
        questHeaderParam.borderColor = Color.BLACK;
        questHeaderParam.size = 12;
        questHeaderFont = fontGenerator.generateFont(questHeaderParam);

        FreeTypeFontParameter questDescParam = new FreeTypeFontParameter();
        questDescParam.borderWidth = 1.0f;
        questDescParam.borderColor = Color.BLACK;
        questDescParam.size = 8;
        questDescFont = fontGenerator.generateFont(questDescParam);

        fontGenerator.dispose();

        // Buat font terpisah untuk menu (ukuran lebih besar)
        FreeTypeFontGenerator menuGen = new FreeTypeFontGenerator(Gdx.files.internal("Battle/PressStart2P.ttf"));
        FreeTypeFontParameter menuTitleParam = new FreeTypeFontParameter();
        menuTitleParam.size = 20; menuTitleParam.borderWidth = 2f; menuTitleParam.borderColor = Color.BLACK;
        menuTitleFont = menuGen.generateFont(menuTitleParam);
        FreeTypeFontParameter menuItemParam = new FreeTypeFontParameter();
        menuItemParam.size = 12; menuItemParam.borderWidth = 1.5f; menuItemParam.borderColor = Color.BLACK;
        BitmapFont menuItemFont = menuGen.generateFont(menuItemParam);
        menuGen.dispose();
        gameMenu = new GameMenu(game, menuTitleFont, menuItemFont);

        // Inisialisasi DevConsole dan pasang InputProcessor untuk keyTyped
        devConsole = DevConsole.getInstance();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                return devConsole.keyTyped(character);
            }
        });

        // Inisialisasi dan muat NPC
        npcs = new Array<>();
        npcs.add(new NPC("Teacher", 135f, 477f, "NPC_School_Teacher", "down", allTextures));
        npcs.add(new NPC("NPC 1", 103f, 323f, "NPC_School_Female_1", "up", allTextures));
        npcs.add(new NPC("NPC 2", 359f, 323f, "NPC_School_Female_2", "up", allTextures));
        npcs.add(new NPC("NPC 3", 611f, 323f, "NPC_School_Female_3", "up", allTextures));
        npcs.add(new NPC("NPC 4", 230f, 227f, "NPC_School_Female_4", "up", allTextures));
        npcs.add(new NPC("NPC 5", 358f, 227f, "NPC_School_Male_1", "up", allTextures));
        npcs.add(new NPC("NPC 6", 744f, 227f, "NPC_School_Male_2", "up", allTextures));
        npcs.add(new NPC("NPC 7", 232f, 131f, "NPC_School_Male_3", "up", allTextures));
        npcs.add(new NPC("NPC 8", 614f, 131f, "NPC_School_Male_4", "up", allTextures));
        npcs.add(new NPC("NPC 9", 227f, 35f, "NPC_School_Male_5", "up", allTextures));
        npcs.add(new NPC("NPC 10", 355f, 35f, "NPC_School_Male_6", "up", allTextures));
        npcs.add(new NPC("NPC 11", 484f, 35f, "NPC_School_Male_7", "up", allTextures));

        // Preload animasi "lihat kiri" untuk semua NPC (digunakan saat event jendela)
        for (NPC npc : npcs) {
            npc.loadLookLeftAnimation(allTextures);
        }

        // Initialize Dialogue Box Image
        dialogBoxImage = new Texture(Gdx.files.internal("Battle/Menu/Dialog_Box.png"));
        allTextures.add(dialogBoxImage);

        // Initialize Dialogue Lines
        dialogueLines = new Array<>();
        dialogueLines.add(new DialogueLine("Deo", "Halo pak guru"));
        dialogueLines.add(new DialogueLine("Pak guru", "Kenapa deo? pembelajaaran akan segera dimulai. silakan kembali ke mejamu sekarang."));
        dialogueLines.add(new DialogueLine("Deo", "Iya pak"));

        // Initialize Teacher Left Portrait Animation
        TextureRegion[] teacherLeftFrames = new TextureRegion[2];
        for (int i = 1; i <= 2; i++) {
            String path = "Eksplore/NPC School/NPC_School_Teacher/standard/idle/left/" + i + ".png";
            Texture tex = new Texture(Gdx.files.internal(path));
            allTextures.add(tex);
            teacherLeftFrames[i - 1] = new TextureRegion(tex);
        }
        teacherLeftAnimation = new Animation<>(0.25f, teacherLeftFrames);
        teacherLeftAnimation.setPlayMode(Animation.PlayMode.LOOP);
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
        if (isDisposed) return;
        stateTime += delta;

        // Update typing effect if dialogue is active
        if (isDialogueActive) {
            dialogueTypingTimer += delta;
            DialogueLine line = dialogueLines.get(currentDialogueIndex);
            if (dialogueTypingTimer >= TYPING_SPEED) {
                dialogueTypingTimer = 0f;
                if (dialogueCharIndex < line.text.length()) {
                    dialogueCharIndex++;
                }
            }
        }

        // Update DevConsole animasi slide
        devConsole.update(delta);

        // Update NPC
        if (npcs != null) {
            for (NPC npc : npcs) {
                npc.update(delta);
            }
        }

        // Update cutscene state machine
        updateCutscene(delta);

        // 1. Proses input keyboard & pergerakan
        handleInput(delta);
        if (isDisposed) return;

        // 2. Bersihkan screen
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        // 3. Update kamera & posisi render
        updateCamera();
        renderer.setView(camera);

        // Semua objek (karakter + tile yang perlu depth-sort) akan dirender dalam satu
        // pass Y-sorting.
        // Render base map tanpa layer-layer yang perlu depth-sort (Chair, Table,
        // Props).
        for (com.badlogic.gdx.maps.MapLayer layer : map.getLayers()) {
            boolean isDepthSorted = (layer == chairLayer)
                    || (layer == tableLayer)
                    || (layer == table2Layer)
                    || (layer == table3Layer)
                    || (layer == table4Layer)
                    || (layer == propsLayer)
                    || (layer == collisionLayer);
            layer.setVisible(!isDepthSorted);
        }

        // 3. Render base map (lantai, dinding, dll — semua yang bukan depth-sorted)
        renderer.render();

        // 4. Kumpulkan semua renderable: Player, NPC, dan semua tile yang butuh
        // depth-sort
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        java.util.List<RenderableCharacter> renderables = new java.util.ArrayList<>();

        // Tambahkan Player dengan Y-sorting default (playerSortingY = playerY)
        float playerSortingY = playerY;
        renderables.add(new RenderableCharacter(playerY, playerSortingY, getPlayerFrame(), playerX, 84f, 84f));

        // Tambahkan semua NPC (Teacher NPC digeser +70f agar tertutup meja, student NPCs default agar di atas meja)
        if (npcs != null) {
            for (NPC npc : npcs) {
                float npcSortingY = npc.getY();
                if ("Teacher".equalsIgnoreCase(npc.getName())) {
                    npcSortingY += 70f;
                }
                renderables.add(new RenderableCharacter(npc.getY(), npcSortingY, npc.getCurrentFrame(), npc.getX(), 84f, 84f));
            }
        }

        // Fungsi pembantu: tambahkan semua tile dari sebuah layer ke dalam Y-sorting
        TiledMapTileLayer[] depthLayers = { chairLayer, tableLayer, table2Layer, table3Layer, table4Layer, propsLayer };
        for (TiledMapTileLayer dLayer : depthLayers) {
            if (dLayer == null)
                continue;
            for (int tx = 0; tx < mapWidth; tx++) {
                for (int ty = 0; ty < mapHeight; ty++) {
                    TiledMapTileLayer.Cell cell = dLayer.getCell(tx, ty);
                    if (cell != null && cell.getTile() != null) {
                        TextureRegion reg = cell.getTile().getTextureRegion();
                        if (reg != null) {
                            float tileY = ty * 32f;
                            float tileSortingY = tileY;

                            if (dLayer == chairLayer) {
                                // Murid berada di bawah kursi
                                tileSortingY = tileY - 35f;
                            } else {
                                boolean isTable = (dLayer == tableLayer || dLayer == table2Layer || dLayer == table3Layer || dLayer == table4Layer);
                                boolean isProp = (dLayer == propsLayer);

                                if (isTable || isProp) {
                                    boolean isActive = false;
                                    if (dLayer == table2Layer || (isProp && ty >= 12 && tx < 10)) {
                                        if (ty >= 12) {
                                            isActive = (playerY >= 476f && playerY < 520f);
                                        } else {
                                            isActive = (playerY >= 284f && playerY < 320f);
                                        }
                                    } else if (dLayer == tableLayer || (isProp && ty >= 10 && ty <= 12)) {
                                        isActive = (playerY >= 380f && playerY < 420f);
                                    } else if (isProp && ty >= 7 && ty <= 9) {
                                        isActive = (playerY >= 284f && playerY < 320f);
                                    } else if (dLayer == table3Layer || (isProp && ty >= 4 && ty <= 6)) {
                                        isActive = (playerY >= 188f && playerY < 220f);
                                    } else if (dLayer == table4Layer || (isProp && ty >= 1 && ty <= 3)) {
                                        isActive = (playerY >= 92f && playerY < 120f);
                                    }

                                    if (isActive) {
                                        tileSortingY = playerY - 1f;
                                    } else {
                                        if (tileY < playerY) {
                                            tileSortingY = playerY + 1f;
                                        } else {
                                            tileSortingY = tileY;
                                        }
                                    }

                                    if (isProp) {
                                        // Layer Props selalu berada di atas layer Table
                                        tileSortingY -= 0.5f;
                                    }
                                }
                            }

                            renderables.add(new RenderableCharacter(tileY, tileSortingY, reg, tx * 32f, 32f, 32f));
                        }
                    }
                }
            }
        }

        // Urutkan semua renderable berdasarkan sortingY descending
        renderables.sort(new java.util.Comparator<RenderableCharacter>() {
            @Override
            public int compare(RenderableCharacter a, RenderableCharacter b) {
                return Float.compare(b.sortingY, a.sortingY);
            }
        });

        for (RenderableCharacter rc : renderables) {
            spriteBatch.draw(rc.frame, rc.x, rc.y, rc.width, rc.height);
        }

        // Draw seat indicator (pulsing gold circle)
        drawQuestBeacon();

        // Draw floating RPG button "Interact" if player is near teacher and not in dialogue
        drawInteractPrompt();

        drawCoordinates();
        spriteBatch.end();

        // Restore visibility untuk operasi map lain (misal isOverlappingChair)
        for (com.badlogic.gdx.maps.MapLayer layer : map.getLayers()) {
            layer.setVisible(layer != collisionLayer);
        }

        // (Render Dialog Battle Prompt dihapus)

        // ── Render hitbox debug overlay ───────────────────────────────
        if (devConsole.isShowHitbox()) {
            renderHitboxOverlay();
        }

        // ── Render collision debug overlay ────────────────────────────
        if (devConsole.isShowCollision()) {
            renderCollisionOverlay();
        }

        // ── Render FPS monitor ────────────────────────────────────────
        if (devConsole.isShowFps()) {
            renderFpsOverlay();
        }

        // Render Dialogue UI (cinematic screen space)
        drawDialogueUI();

        // Render HUD, Quest UI, Banner, and Floating Texts (screen space)
        drawTopBarHUD();
        drawQuestUiPanel();
        drawBannerNotification(delta);
        drawFloatingTexts(delta);

        // ── Render DevConsole UI (screen-space) ───────────────────────
        // Reset projection ke koordinat layar agar console selalu di pojok atas
        spriteBatch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);
        float sw = viewport.getWorldWidth();
        float sh = viewport.getWorldHeight();
        // Gunakan koordinat kamera untuk offset posisi console
        float consoleOffX = camera.position.x - sw * camera.zoom / 2f;
        float consoleOffY = camera.position.y + sh * camera.zoom / 2f;
        // Render di viewport-space dengan projection ortho terpisah
        renderConsoleInScreenSpace();

        // ── Cutscene overlays (di atas segalanya, screen-space) ─────────────────
        if (flashbangAlpha > 0f) drawFlashbang();
        if (cutsceneState == CutsceneState.KRAKKK_SHOW
                || cutsceneState == CutsceneState.NPC_LOOK_PHASE
                || cutsceneState == CutsceneState.WALK_TO_WINDOW) {
            drawKrakkkBox();
        }
        if (cutsceneState == CutsceneState.WINDOW_POPUP) drawWindowPopup();
        if (fadeOutAlpha > 0f)  drawWhiteFade();

        // ── Game Menu overlay ──────────────────────────────────────────
        if (gameMenu.isOpen()) {
            gameMenu.render(spriteBatch, shapeRenderer, delta);
        }
        // Save toast notification
        if (showSaveToast) drawSaveToast(delta);
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

    /** Render overlay FPS & memory di pojok kanan atas world-space. */
    private void renderFpsOverlay() {
        float viewWidth = viewport.getWorldWidth() * camera.zoom;
        float viewHeight = viewport.getWorldHeight() * camera.zoom;
        float textX = camera.position.x + viewWidth / 2f - 200f;
        float textY = camera.position.y + viewHeight / 2f - 15f;

        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        font.setColor(Color.CYAN);
        font.draw(spriteBatch,
                "FPS: " + Gdx.graphics.getFramesPerSecond() +
                        "  MEM: " + (int) (Gdx.app.getJavaHeap() / 1024 / 1024) + "MB",
                textX, textY);
        spriteBatch.end();
    }

    private void handleInput(float delta) {
        // ── DevConsole input (prioritas tertinggi, bisa dibuka di mana saja & kapan saja) ──
        if (devConsole.handleInput()) {
            if (devConsole.consumeReload()) {
                game.setScreen(new ExplorationScreen(game));
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

        // ── ESC → Game Menu (bisa dibuka kapan saja kecuali saat fade ke cave) ─
        if (cutsceneState != CutsceneState.FADE_TO_CAVE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                if (isQuestUiOpen) {
                    isQuestUiOpen = false;
                } else {
                    gameMenu.toggle();
                }
                isMoving = false;
                return;
            }
        }

        // ── Jika game menu terbuka, proses hanya input menu ─
        if (gameMenu.isOpen()) {
            GameMenu.Action action = gameMenu.handleInput();
            switch (action) {
                case SAVE:
                    GameSave.save("classroom", playerX, playerY, game.getGold(), game.getGems());
                    showSaveToast = true;
                    saveToastTimer = 2.5f;
                    break;
                case EXIT:
                    Gdx.app.exit();
                    break;
                default:
                    break;
            }
            isMoving = false;
            return;
        }

        if (isDialogueActive) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.E) || 
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || 
                Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
                Gdx.input.justTouched()) {
                
                DialogueLine line = dialogueLines.get(currentDialogueIndex);
                if (dialogueCharIndex < line.text.length()) {
                    dialogueCharIndex = line.text.length();
                } else {
                    currentDialogueIndex++;
                    if (currentDialogueIndex >= dialogueLines.size) {
                        isDialogueActive = false;
                        currentDialogueIndex = 0;

                        // Setelah semua dialogue lesson selesai → mulai cutscene window bang
                        Quest seatQuest2 = game.getQuestById("seat");
                        if (seatQuest2 != null && seatQuest2.isCompleted()
                                && cutsceneState == CutsceneState.NONE) {
                            game.getQuestById("learn_start").setProgress(1);
                            // Mulai 2 detik penantian sebelum suara krakkkk
                            cutsceneState = CutsceneState.SITTING_DELAY;
                            cutsceneTimer  = 2.0f;
                        }
                    } else {
                        dialogueCharIndex = 0;
                        dialogueTypingTimer = 0f;
                    }
                }
            }
            isMoving = false;
            return;
        }

        // Toggle Quest UI with Key Q
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            isQuestUiOpen = !isQuestUiOpen;
            isMoving = false;
            return;
        }

        if (isQuestUiOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                isQuestUiOpen = false;
            }
            handleQuestUiClicks();
            isMoving = false;
            return;
        }

        // ── Saat cutscene aktif: batasi input sesuai state ─────────────────────
        if (cutsceneState == CutsceneState.SITTING_DELAY
                || cutsceneState == CutsceneState.KRAKKK_SHOW
                || cutsceneState == CutsceneState.NPC_LOOK_PHASE
                || cutsceneState == CutsceneState.FADE_TO_CAVE) {
            playerX = 102f; playerY = 130f;
            currentDirection = Direction.NORTH;
            isMoving = false;
            return;
        }

        if (cutsceneState == CutsceneState.WINDOW_POPUP) {
            // Proses klik tombol di popup
            if (Gdx.input.justTouched()) {
                float mx = Gdx.input.getX();
                float my = 832f - Gdx.input.getY();
                // Tombol IYA (kiri) ~ x=426-576, y=360-400
                if (mx >= 426f && mx <= 576f && my >= 360f && my <= 400f) {
                    // Mulai fade putih ke cave
                    cutsceneState = CutsceneState.FADE_TO_CAVE;
                    cutsceneTimer = 0f;
                }
                // Tombol TIDAK (kanan) ~ x=676-826, y=360-400
                if (mx >= 676f && mx <= 826f && my >= 360f && my <= 400f) {
                    // Tidak → player bebas bergerak
                    cutsceneState = CutsceneState.NONE;
                    // Kembalikan NPC ke arah semula
                    for (NPC npc : npcs) npc.setLookingLeft(false);
                }
            }
            isMoving = false;
            return;
        }

        // Check seat coordinates (player sudah duduk, terkunci)
        Quest seatQuest = game.getQuestById("seat");
        if (seatQuest != null && seatQuest.isCompleted() && !seatQuest.isClaimed()) {
            playerX = 102f;
            playerY = 130f;
            currentDirection = Direction.NORTH;
            isMoving = false;
            
            // Still allow opening Quest UI or DevConsole
            if (Gdx.input.justTouched()) {
                com.badlogic.gdx.math.Vector3 clickPoint = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                uiCamera.unproject(clickPoint, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
                if (questBtnRect.contains(clickPoint.x, clickPoint.y)) {
                    isQuestUiOpen = true;
                }
            }
            
            if (devConsole.handleInput()) {
                if (devConsole.consumeRestart()) {
                    GameSave.clear();
                    game.initDefaultQuests();
                    game.setScreen(new ExplorationScreen(game));
                }
                if (devConsole.consumeBattle()) game.setScreen(new BattleScreen(game));
            }
            return;
        }

        if (seatQuest != null && !seatQuest.isCompleted()) {
            float dxSeat = playerX - 102f;
            float dySeat = playerY - 130f;
            float distSeat = (float) Math.sqrt(dxSeat * dxSeat + dySeat * dySeat);
            if (distSeat < 20f) {
                seatQuest.setProgress(1);
                seatQuest.setClaimed(true); // Klaim otomatis
                playerX = 102f;
                playerY = 130f;
                currentDirection = Direction.NORTH;
                isMoving = false;

                // Picu dialogue lesson secara otomatis
                isQuestUiOpen = false;
                dialogueLines.clear();
                dialogueLines.add(new DialogueLine("Deo", "Akhirnya aku kembali ke tempat dudukku. Capek juga berjalan keliling kelas."));
                dialogueLines.add(new DialogueLine("Teacher", "Baik anak-anak, sekarang semua sudah berada di bangku masing-masing."));
                dialogueLines.add(new DialogueLine("Teacher", "Mari kita mulai pembelajaran hari ini. Hari ini kita akan membahas tentang legenda 'The Last Ancestors'..."));
                dialogueLines.add(new DialogueLine("Teacher", "Yaitu leluhur agung kita yang mengorbankan diri mereka untuk menyegel kekuatan kegelapan."));
                dialogueLines.add(new DialogueLine("NPC 7", "Pak Guru, apakah segel itu masih aman hingga sekarang?"));
                dialogueLines.add(new DialogueLine("Teacher", "Tentu saja, segel itu dijaga oleh kuil suci di..."));

                isDialogueActive = true;
                currentDialogueIndex = 0;
                dialogueCharIndex = 0;
                dialogueTypingTimer = 0f;
                return;
            }
        }

        // Top Bar click detection
        if (Gdx.input.justTouched()) {
            com.badlogic.gdx.math.Vector3 clickPoint = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            uiCamera.unproject(clickPoint, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
            if (questBtnRect.contains(clickPoint.x, clickPoint.y)) {
                isQuestUiOpen = true;
                isMoving = false;
                return;
            }
        }

        // Proximity and interaction check with NPCs
        NPC nearbyNpc = null;
        float minNpcDist = Float.MAX_VALUE;
        for (NPC npc : npcs) {
            float distNpcX = playerX - npc.getX();
            float distNpcY = playerY - npc.getY();
            float distNpc = (float) Math.sqrt(distNpcX * distNpcX + distNpcY * distNpcY);
            if (distNpc < 50f && distNpc < minNpcDist) {
                minNpcDist = distNpc;
                nearbyNpc = npc;
            }
        }

        if (nearbyNpc != null && !isDialogueActive) {
            boolean clickedInteract = false;
            if (Gdx.input.justTouched()) {
                com.badlogic.gdx.math.Vector3 touchPoint = new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
                camera.unproject(touchPoint);
                
                float promptW = 110f;
                float promptH = 20f;
                float bounceY = (float) Math.sin(stateTime * 5f) * 3f;
                float promptX = nearbyNpc.getX() - 13f;
                float promptY = nearbyNpc.getY() + 85f + bounceY;

                if (touchPoint.x >= promptX && touchPoint.x <= promptX + promptW &&
                    touchPoint.y >= promptY && touchPoint.y <= promptY + promptH) {
                    clickedInteract = true;
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.E) || clickedInteract) {
                startNpcDialogue(nearbyNpc);
                isDialogueActive = true;
                currentDialogueIndex = 0;
                dialogueCharIndex = 0;
                dialogueTypingTimer = 0f;
                isMoving = false;
                
                // Track talk_npc achievement
                if (!nearbyNpc.getName().equals("Teacher") && !interactedNpcNames.contains(nearbyNpc.getName(), false)) {
                    interactedNpcNames.add(nearbyNpc.getName());
                    Quest talkQuest = game.getQuestById("talk_npc");
                    if (talkQuest != null) {
                        talkQuest.addProgress(1);
                    }
                }
                return;
            }
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

        // Terapkan speed multiplier dari DevConsole
        dx *= devConsole.getSpeedMulti();
        dy *= devConsole.getSpeedMulti();

        isMoving = (dx != 0f || dy != 0f);

        if (isMoving) {
            float oldX = playerX;
            float oldY = playerY;
            float newX = playerX + dx;
            float newY = playerY + dy;

            // Jika noclip aktif, lewati collision check
            if (devConsole.isNoClip()) {
                playerX = newX;
                playerY = newY;
            } else {
                // X-movement check
                if (!isColliding(newX, playerY)) {
                    playerX = newX;
                }

                // Y-movement check
                if (!isColliding(playerX, newY)) {
                    playerY = newY;
                }
            }

            float distMoved = (float) Math.sqrt((playerX - oldX) * (playerX - oldX) + (playerY - oldY) * (playerY - oldY));
            if (distMoved > 0f) {
                Quest exploreQuest = game.getQuestById("explore_classroom");
                if (exploreQuest != null && !exploreQuest.isCompleted()) {
                    exploreQuest.addProgress((int) distMoved);
                }
                Quest stepsQuest = game.getQuestById("first_steps");
                if (stepsQuest != null && !stepsQuest.isCompleted()) {
                    stepsQuest.addProgress((int) distMoved);
                }
            }
        }
    }

    private void startNpcDialogue(NPC npc) {
        dialogueLines.clear();
        String name = npc.getName();
        if (name.equals("Teacher")) {
            Quest seatQuest = game.getQuestById("seat");
            if (seatQuest != null && !seatQuest.isCompleted()) {
                dialogueLines.add(new DialogueLine("Deo", "Halo Pak Guru."));
                dialogueLines.add(new DialogueLine("Teacher", "Halo Deo. Pembelajaran akan segera dimulai. Silakan kembali ke mejamu sekarang di baris ketiga sebelah kiri dekat jendela."));
                dialogueLines.add(new DialogueLine("Deo", "Baik, Pak."));
                
                // Complete greet_teacher quest
                Quest greetQuest = game.getQuestById("greet_teacher");
                if (greetQuest != null && !greetQuest.isCompleted()) {
                    greetQuest.setProgress(1);
                }
            } else {
                dialogueLines.add(new DialogueLine("Teacher", "Silakan duduk tenang di mejamu, Deo. Pelajaran akan segera dimulai."));
            }
        } else {
            // Student dialogues
            String text = "Halo Deo!";
            if (name.equals("NPC 1")) text = "Semoga hari ini tidak ada ujian mendadak ya... Aku belum belajar.";
            else if (name.equals("NPC 2")) text = "Deo! Kamu sudah membaca buku materi bab 3?";
            else if (name.equals("NPC 3")) text = "Duh, aku lupa membawa pensil cadangan. Semoga tidak patah.";
            else if (name.equals("NPC 4")) text = "Selamat pagi Deo! Hari yang cerah untuk belajar.";
            else if (name.equals("NPC 5")) text = "Pelajaran Pak Guru selalu menarik, aku sangat menyukainya.";
            else if (name.equals("NPC 6")) text = "Hei Deo, nanti istirahat kita ke kantin bersama ya?";
            else if (name.equals("NPC 7")) text = "Kursimu ada di belakangku, Deo. Baris ketiga sebelah kiri dekat jendela.";
            else if (name.equals("NPC 8")) text = "Aku lelah sekali pagi ini, semalam aku begadang bermain game.";
            else if (name.equals("NPC 9")) text = "Jangan berisik, Pak Guru sedang bersiap memulai kelas.";
            else if (name.equals("NPC 10")) text = "Selamat pagi Deo, ayo bersiap untuk belajar.";
            else if (name.equals("NPC 11")) text = "Kursimu kosong tuh di sebelah kiri baris ketiga, cepatlah duduk.";
            
            dialogueLines.add(new DialogueLine(name, text));
        }
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
                    
                    // Specific trigger for seat quest claimed -> starts the lesson cutscene dialogue!
                    if (q.getId().equals("seat")) {
                        isQuestUiOpen = false;
                        
                        dialogueLines.clear();
                        dialogueLines.add(new DialogueLine("Deo", "Akhirnya aku kembali ke tempat dudukku. Capek juga berjalan keliling kelas."));
                        dialogueLines.add(new DialogueLine("Teacher", "Baik anak-anak, sekarang semua sudah berada di bangku masing-masing."));
                        dialogueLines.add(new DialogueLine("Teacher", "Mari kita mulai pembelajaran hari ini. Hari ini kita akan membahas tentang legenda 'The Last Ancestors'..."));
                        dialogueLines.add(new DialogueLine("Teacher", "Yaitu leluhur agung kita yang mengorbankan diri mereka untuk menyegel kekuatan kegelapan."));
                        dialogueLines.add(new DialogueLine("NPC 7", "Pak Guru, apakah segel itu masih aman hingga sekarang?"));
                        dialogueLines.add(new DialogueLine("Teacher", "Tentu saja, segel itu dijaga oleh kuil suci di..."));
                        dialogueLines.add(new DialogueLine("SISTEM", "*RUMBLE! BUMMM! Tiba-tiba terjadi getaran gempa bumi yang sangat dahsyat!*"));
                        dialogueLines.add(new DialogueLine("Teacher", "Ada apa ini?! Gempa bumi? Semua siswa harap tenang!"));
                        dialogueLines.add(new DialogueLine("SISTEM", "*Dinding kelas sebelah kanan hancur! Monster akar raksasa Treant menerobos masuk!*"));
                        dialogueLines.add(new DialogueLine("Teacher", "Ya Tuhan! Itu monster Treant purba dari hutan Mystic Forest! Bagaimana dia bisa menembus dinding sekolah?!"));
                        dialogueLines.add(new DialogueLine("Teacher", "Deo! Gunakan pedang pusaka yang selalu kau bawa untuk menahannya sementara siswa lain melarikan diri!"));
                        dialogueLines.add(new DialogueLine("Deo", "Baik, Pak Guru! Serahkan padaku!"));
                        
                        isDialogueActive = true;
                        currentDialogueIndex = 0;
                        dialogueCharIndex = 0;
                        dialogueTypingTimer = 0f;
                    }
                } else if (!q.isCompleted()) {
                    // "Go Now" -> close menu
                    isQuestUiOpen = false;
                }
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
                case NORTH:
                    currentFrame = walkNorth.getKeyFrame(stateTime);
                    break;
                case EAST:
                    currentFrame = walkEast.getKeyFrame(stateTime);
                    break;
                case WEST:
                    currentFrame = walkWest.getKeyFrame(stateTime);
                    break;
                case SOUTH:
                default:
                    currentFrame = walkSouth.getKeyFrame(stateTime);
                    break;
            }
        } else {
            switch (currentDirection) {
                case NORTH:
                    currentFrame = idleNorth.getKeyFrame(stateTime);
                    break;
                case EAST:
                    currentFrame = idleEast.getKeyFrame(stateTime);
                    break;
                case WEST:
                    currentFrame = idleWest.getKeyFrame(stateTime);
                    break;
                case SOUTH:
                default:
                    currentFrame = idleSouth.getKeyFrame(stateTime);
                    break;
            }
        }
        return currentFrame;
    }

    private void drawCoordinates() {
        float viewWidth = viewport.getWorldWidth() * camera.zoom;
        float viewHeight = viewport.getWorldHeight() * camera.zoom;
        float textX = camera.position.x - viewWidth / 2f + 15f;
        float textY = camera.position.y + viewHeight / 2f - 50f;

        font.setColor(Color.YELLOW);
        font.draw(spriteBatch, "X: " + (int) playerX + "  Y: " + (int) playerY, textX, textY);
    }

    private boolean isOverlappingChair() {
        TiledMapTileLayer chairLayer = (TiledMapTileLayer) map.getLayers().get("Chair");
        if (chairLayer == null)
            return false;

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

    private void drawInteractPrompt() {
        if (isDialogueActive) return;

        // Teacher coordinates
        float teacherX = 135f;
        float teacherY = 477f;

        float distX = playerX - teacherX;
        float distY = playerY - teacherY;
        float dist = (float) Math.sqrt(distX * distX + distY * distY);

        if (dist < 70f) {
            // Draw floating RPG button "Interact"
            float promptW = 110f;
            float promptH = 20f;
            float bounceY = (float) Math.sin(stateTime * 5f) * 3f;
            float promptX = teacherX - 13f; // Centered beautifully
            float promptY = teacherY + 85f + bounceY;

            spriteBatch.end();

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(camera.combined);
            
            // 1. Gold border
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(255f / 255f, 215f / 255f, 0f / 255f, 1f);
            shapeRenderer.rect(promptX, promptY, promptW, promptH);
            shapeRenderer.end();

            // 2. Inner boxes
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            // Keycap Background (Light Gray)
            shapeRenderer.setColor(230f / 255f, 230f / 255f, 230f / 255f, 1f);
            shapeRenderer.rect(promptX + 1f, promptY + 1f, 18f, promptH - 2f);
            
            // Text Background (Dark Slate)
            shapeRenderer.setColor(16f / 255f, 22f / 255f, 38f / 255f, 0.95f);
            shapeRenderer.rect(promptX + 19f, promptY + 1f, promptW - 20f, promptH - 2f);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            spriteBatch.begin();
            // Draw 'E' in black
            font.setColor(Color.BLACK);
            font.draw(spriteBatch, "E", promptX + 6f, promptY + 14f);

            // Draw 'Interact' in white
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "Interact", promptX + 27f, promptY + 14f);
        }
    }

    private void drawDialogueUI() {
        if (!isDialogueActive) return;

        com.badlogic.gdx.math.Matrix4 origProj = new com.badlogic.gdx.math.Matrix4(spriteBatch.getProjectionMatrix());

        com.badlogic.gdx.math.Matrix4 uiProj = new com.badlogic.gdx.math.Matrix4();
        uiProj.setToOrtho2D(0, 0, 1253, 832);

        spriteBatch.setProjectionMatrix(uiProj);
        shapeRenderer.setProjectionMatrix(uiProj);

        // 1. Draw cinematic overlay
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.4f);
        shapeRenderer.rect(0, 0, 1253, 832);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        DialogueLine line = dialogueLines.get(currentDialogueIndex);
        boolean isDeoSpeaking = line.speaker.equalsIgnoreCase("Deo");

        // 2. Draw character portraits with relative speaker opacity (WAIST-UP / HUGE PORTRAITS)
        spriteBatch.begin();
        
        float deoOpacity = isDeoSpeaking ? 1.0f : 0.35f;
        TextureRegion deoFrame = idleEast.getKeyFrame(stateTime);
        spriteBatch.setColor(1f, 1f, 1f, deoOpacity);
        // Scale to 380x380 and place closer to left screen edge
        spriteBatch.draw(deoFrame, 60f, 170f, 380f, 380f);

        if (!line.speaker.equalsIgnoreCase("SISTEM")) {
            float npcOpacity = !isDeoSpeaking ? 1.0f : 0.35f;
            NPC speakingNpc = null;
            for (NPC n : npcs) {
                if (n.getName().equalsIgnoreCase(line.speaker)) {
                    speakingNpc = n;
                    break;
                }
            }
            TextureRegion rightPortraitFrame = (speakingNpc != null) ? speakingNpc.getCurrentFrame() : teacherLeftAnimation.getKeyFrame(stateTime);
            spriteBatch.setColor(1f, 1f, 1f, npcOpacity);
            // Scale to 380x380 and place closer to right screen edge
            spriteBatch.draw(rightPortraitFrame, 1253f - 60f - 380f, 170f, 380f, 380f);
        }

        spriteBatch.setColor(1f, 1f, 1f, 1f);
        spriteBatch.end();

        // 3. Draw Dialogue Box
        float boxX = 100f;
        float boxY = 40f;
        float boxW = 1253f - 200f;
        float boxH = 150f;

        spriteBatch.begin();
        if (dialogBoxImage != null) {
            spriteBatch.draw(dialogBoxImage, boxX, boxY, boxW, boxH);
        }
        spriteBatch.end();

        if (dialogBoxImage == null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(16f / 255f, 22f / 255f, 38f / 255f, 0.9f);
            shapeRenderer.rect(boxX, boxY, boxW, boxH);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(200f / 255f, 175f / 255f, 95f / 255f, 1f);
            shapeRenderer.rect(boxX, boxY, boxW, boxH);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // 4. Draw Speaker Tag Box
        float tagW = 180f;
        float tagH = 30f;
        // Shift tag to align with the start of the padded text!
        float tagX = isDeoSpeaking ? boxX + 160f : boxX + boxW - 160f - tagW;
        float tagY = boxY + boxH - 15f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(24f / 255f, 32f / 255f, 54f / 255f, 1f);
        shapeRenderer.rect(tagX, tagY, tagW, tagH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(200f / 255f, 175f / 255f, 95f / 255f, 1f);
        shapeRenderer.rect(tagX, tagY, tagW, tagH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Draw Speaker Name Text
        spriteBatch.begin();
        nameFont.setColor(isDeoSpeaking ? Color.YELLOW : Color.CYAN);
        String speakerName = line.speaker.toUpperCase();
        nameFont.draw(spriteBatch, speakerName, tagX + 15f, tagY + 20f);

        // 5. Draw Speech Text (WITH AUTO-WRAPPING & PERFECT PADDING!)
        dialogueFont.setColor(Color.WHITE);
        String displayedText = line.text.substring(0, dialogueCharIndex);
        // Draw wrapped text inside a beautifully centered padded zone
        dialogueFont.draw(spriteBatch, displayedText, boxX + 160f, boxY + boxH - 45f, boxW - 320f, com.badlogic.gdx.utils.Align.left, true);

        // Draw next prompt
        font.setColor(Color.LIGHT_GRAY);
        font.draw(spriteBatch, "Press [E/Space/Click] to next", boxX + boxW - 250f, boxY + 25f);
        spriteBatch.end();

        spriteBatch.setProjectionMatrix(origProj);
        shapeRenderer.setProjectionMatrix(origProj);
    }

    private void drawQuestBeacon() {
        Quest seatQuest = game.getQuestById("seat");
        if (seatQuest != null && !seatQuest.isCompleted()) {
            spriteBatch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float pulse = 0.4f + 0.3f * (float) Math.sin(stateTime * 6f);
            shapeRenderer.setColor(255f/255f, 215f/255f, 0f/255f, pulse);
            shapeRenderer.circle(102f + 16f, 130f + 16f, 18f);
            shapeRenderer.end();
            
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(255f/255f, 215f/255f, 0f/255f, pulse * 0.7f);
            shapeRenderer.circle(102f + 16f, 130f + 16f, 24f + 4f * (float) Math.sin(stateTime * 4f));
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
            spriteBatch.begin();
        }
    }

    private void drawTopBarHUD() {
        if (isDialogueActive) return;

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
            
            spriteBatch.begin();
            questHeaderFont.setColor(Color.YELLOW);
            questHeaderFont.draw(spriteBatch, bannerText, bannerX, bannerAnimY + 32f, bannerW, Align.center, false);
            spriteBatch.end();
        }
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
        if (dialogueFont != null) {
            dialogueFont.dispose();
        }
        if (nameFont != null) {
            nameFont.dispose();
        }
        if (questTitleFont != null) {
            questTitleFont.dispose();
        }
        if (questHeaderFont != null) {
            questHeaderFont.dispose();
        }
        if (questDescFont != null) {
            questDescFont.dispose();
        }
        if (menuTitleFont != null) {
            menuTitleFont.dispose();
        }
        isDisposed = true;
    }

    // ── Update Cutscene State Machine ────────────────────────────────────────
    private void updateCutscene(float delta) {
        // Selalu decel flashbang alpha jika ada
        if (flashbangAlpha > 0f) {
            flashbangAlpha = Math.max(0f, flashbangAlpha - delta * 0.8f);
        }

        switch (cutsceneState) {
            case SITTING_DELAY:
                cutsceneTimer -= delta;
                if (cutsceneTimer <= 0f) {
                    // Jeda 2 detik selesai -> Picu bunyi keras "krakkkk" & flashbang
                    cutsceneState = CutsceneState.KRAKKK_SHOW;
                    flashbangAlpha = 1f; // full white bright flashbang
                    krakkkBoxShown = true;
                    cutsceneTimer = 2.5f; // tampilkan box selama 2.5 detik
                }
                break;

            case KRAKKK_SHOW:
                cutsceneTimer -= delta;
                if (cutsceneTimer <= 0f) {
                    // Bunyi keras selesai -> Semua NPC reflek melihat ke jendela
                    cutsceneState = CutsceneState.NPC_LOOK_PHASE;
                    for (NPC npc : npcs) {
                        npc.setLookingLeft(true);
                    }
                    cutsceneTimer = 1.0f; // Jeda 1 detik agar efek menoleh terasa
                }
                break;

            case NPC_LOOK_PHASE:
                cutsceneTimer -= delta;
                if (cutsceneTimer <= 0f) {
                    // NPC selesai menoleh -> User mulai mengendalikan player berjalan ke jendela
                    cutsceneState = CutsceneState.WALK_TO_WINDOW;
                }
                break;

            case WALK_TO_WINDOW:
                // Cek apakah player sudah mendekati jendela di sebelah kiri
                // Posisi jendela: WINDOW_TARGET_X = 64f, WINDOW_TARGET_Y = 130f
                float dx = playerX - WINDOW_TARGET_X;
                float dy = playerY - WINDOW_TARGET_Y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < WINDOW_REACH_DIST) {
                    // Sampai di jendela -> Muncul popup pilihan melihat
                    cutsceneState = CutsceneState.WINDOW_POPUP;
                }
                break;

            case FADE_TO_CAVE:
                fadeOutAlpha = Math.min(1f, fadeOutAlpha + delta * 1.5f);
                if (fadeOutAlpha >= 1f) {
                    // Pindah ke Cave_Map.tmx dengan screen baru
                    game.setScreen(new CaveScreen(game));
                }
                break;

            case NONE:
            case WINDOW_POPUP:
            default:
                break;
        }
    }

    // ── Helper Drawing untuk Cutscene & UI Tambahan ───────────────────────────
    private void drawFlashbang() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, flashbangAlpha);
        shapeRenderer.rect(0, 0, 1253, 832);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawKrakkkBox() {
        // Tampilkan kotak box dialog di bagian bawah berisi suara krakkk
        float boxX = 150f;
        float boxY = 50f;
        float boxW = 1253f - 300f;
        float boxH = 90f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(16f / 255f, 22f / 255f, 38f / 255f, 0.95f);
        shapeRenderer.rect(boxX, boxY, boxW, boxH);
        shapeRenderer.setColor(Color.RED); // border merah melambangkan ketegangan/suara keras
        shapeRenderer.rect(boxX, boxY, boxW, 3f);
        shapeRenderer.rect(boxX, boxY + boxH - 3f, boxW, 3f);
        shapeRenderer.rect(boxX, boxY, 3f, boxH);
        shapeRenderer.rect(boxX + boxW - 3f, boxY, 3f, boxH);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        questHeaderFont.setColor(Color.RED);
        questHeaderFont.draw(spriteBatch, "* KRAKKKK!!! *", boxX, boxY + 62f, boxW, Align.center, false);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(spriteBatch, "(Suara keras yang sangat mengagetkan terdengar dari arah jendela!)", boxX, boxY + 30f, boxW, Align.center, false);
        spriteBatch.end();
    }

    private void drawWindowPopup() {
        // Dialog popup "Apakah anda ingin melihat?"
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

        // Hover effect pada tombol
        float mx = Gdx.input.getX();
        float my = 832f - Gdx.input.getY();

        // Tombol IYA (kiri) ~ x=426-576, y=360-400
        boolean hoverIya = (mx >= boxX + 50f && mx <= boxX + 200f && my >= boxY + 40f && my <= boxY + 80f);
        shapeRenderer.setColor(hoverIya ? new Color(0f, 0.6f, 0f, 1f) : new Color(0f, 0.4f, 0f, 1f));
        shapeRenderer.rect(boxX + 50f, boxY + 40f, 150f, 40f);

        // Tombol TIDAK (kanan) ~ x=676-826, y=360-400
        boolean hoverTidak = (mx >= boxX + boxW - 200f && mx <= boxX + boxW - 50f && my >= boxY + 40f && my <= boxY + 80f);
        shapeRenderer.setColor(hoverTidak ? new Color(0.7f, 0f, 0f, 1f) : new Color(0.5f, 0f, 0f, 1f));
        shapeRenderer.rect(boxX + boxW - 200f, boxY + 40f, 150f, 40f);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        questHeaderFont.setColor(Color.WHITE);
        questHeaderFont.draw(spriteBatch, "Apakah anda ingin melihat?", boxX, boxY + boxH - 50f, boxW, Align.center, false);

        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "IYA", boxX + 50f, boxY + 65f, 150f, Align.center, false);
        font.draw(spriteBatch, "TIDAK", boxX + boxW - 200f, boxY + 65f, 150f, Align.center, false);
        spriteBatch.end();
    }

    private void drawWhiteFade() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, fadeOutAlpha);
        shapeRenderer.rect(0, 0, 1253, 832);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawSaveToast(float delta) {
        saveToastTimer -= delta;
        if (saveToastTimer <= 0) {
            showSaveToast = false;
            return;
        }

        float alpha = Math.min(1f, saveToastTimer);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.04f, 0.18f, 0.04f, 0.9f * alpha);
        shapeRenderer.rect(426, 30, 400, 50);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        spriteBatch.setProjectionMatrix(uiCamera.combined);
        spriteBatch.begin();
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(spriteBatch, "✓  Game Tersimpan!", 470f, 63f);
        spriteBatch.end();
    }


    private static class DialogueLine {
        final String speaker;
        final String text;

        DialogueLine(String speaker, String text) {
            this.speaker = speaker;
            this.text = text;
        }
    }

    private static class RenderableCharacter {
        final float y;
        final float sortingY;
        final TextureRegion frame;
        final float x;
        final float width;
        final float height;

        RenderableCharacter(float y, float sortingY, TextureRegion frame, float x, float width, float height) {
            this.y = y;
            this.sortingY = sortingY;
            this.frame = frame;
            this.x = x;
            this.width = width;
            this.height = height;
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
}
