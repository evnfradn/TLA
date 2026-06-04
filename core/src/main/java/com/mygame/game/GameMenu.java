package com.mygame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

/**
 * GameMenu — overlay pause menu yang dapat digunakan di semua screen.
 *
 * Cara penggunaan:
 *   1. Buat instance di show() screen: menu = new GameMenu(game, font, headerFont);
 *   2. Di render(): if (menu.isOpen()) menu.render(batch, shape, delta); else game logic;
 *   3. Di handleInput(): menu.handleInput() mengembalikan GameMenu.Action.
 *   4. Di dispose(): menu.dispose();
 */
public class GameMenu {

    public enum Action { NONE, CONTINUE, OPTION, ACHIEVEMENT, TASK }

    // ── Referensi ────────────────────────────────────────────────────────────
    private final TheLastAncestorsGame game;
    private final BitmapFont titleFont;
    private final BitmapFont itemFont;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean open = false;
    private int hoveredItem = -1; // item yang sedang di-hover mouse
    private float openTimer = 0f; // animasi buka menu

    // ── Textures ──────────────────────────────────────────────────────────────
    private final Texture optionTexture;
    private final Texture achievementTexture;
    private final Texture taskTexture;

    // ── Layout ───────────────────────────────────────────────────────────────
    private static final float SCREEN_W = 1253f;
    private static final float SCREEN_H = 832f;

    private static final float BOX_W  = 340f;
    private static final float BOX_H  = 320f;
    private static final float BOX_X  = (SCREEN_W - BOX_W) / 2f;
    private static final float BOX_Y  = (SCREEN_H - BOX_H) / 2f;

    private static final String[] LABELS = { "Option", "Achievement", "Task" };
    private static final float ITEM_H   = 56f;
    private static final float ITEM_GAP = 14f;
    private static final float ITEMS_START_Y = BOX_Y + 90f; // Y bawah baris pertama

    private final Rectangle[] itemRects = new Rectangle[3];

    // ── Warna premium ────────────────────────────────────────────────────────
    private static final Color COL_BG      = new Color(0.04f, 0.06f, 0.12f, 0.97f);
    private static final Color COL_BORDER  = new Color(1f, 0.843f, 0f, 1f);   // gold
    private static final Color COL_HOVER   = new Color(0.24f, 0.36f, 0.60f, 1f);
    private static final Color COL_NORMAL  = new Color(0.10f, 0.15f, 0.28f, 1f);
    private static final Color COL_EXIT    = new Color(0.55f, 0.10f, 0.10f, 1f);
    private static final Color COL_EXIT_H  = new Color(0.78f, 0.15f, 0.15f, 1f);

    public GameMenu(TheLastAncestorsGame game, BitmapFont titleFont, BitmapFont itemFont) {
        this.game      = game;
        this.titleFont = titleFont;
        this.itemFont  = itemFont;

        // Load textures
        optionTexture = new Texture(Gdx.files.internal("Eksplore/UI/Option_UI.png"));
        achievementTexture = new Texture(Gdx.files.internal("Eksplore/UI/Achievment_UI.png"));
        taskTexture = new Texture(Gdx.files.internal("Eksplore/UI/Task_UI.png"));

        // Hitung rectangle untuk ketiga item menu
        for (int i = 0; i < 3; i++) {
            float iy = ITEMS_START_Y + i * (ITEM_H + ITEM_GAP);
            itemRects[i] = new Rectangle(BOX_X + 20f, iy, BOX_W - 40f, ITEM_H);
        }
    }

    // ── API publik ────────────────────────────────────────────────────────────

    public boolean isOpen() { return open; }

    /** Buka / tutup menu (dipanggil saat ESC ditekan di screen). */
    public void toggle() {
        open = !open;
        if (open) openTimer = 0f;
    }

    public void open()  { open = true;  openTimer = 0f; }
    public void close() { open = false; }

    /**
     * Proses input. Harus dipanggil hanya saat isOpen() == true.
     * @return Action yang diminta pengguna (NONE jika tidak ada).
     */
    public Action handleInput() {
        if (!open) return Action.NONE;

        // Tutup dengan ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            close();
            return Action.CONTINUE;
        }

        if (!Gdx.input.justTouched()) return Action.NONE;

        // Konversi koordinat layar → UI (y dibalik oleh LibGDX)
        float mx = Gdx.input.getX();
        float my = SCREEN_H - Gdx.input.getY();

        for (int i = 0; i < itemRects.length; i++) {
            if (itemRects[i].contains(mx, my)) {
                switch (i) {
                    case 0:
                        return Action.OPTION;
                    case 1:
                        return Action.ACHIEVEMENT;
                    case 2:
                        return Action.TASK;
                    default:
                        return Action.NONE;
                }
            }
        }
        return Action.NONE;
    }

    /**
     * Render menu overlay. Harus dipanggil dengan batch/shape yang BELUM di-begin().
     * Projection matrix UI (1253×832) harus sudah diset sebelum memanggil ini.
     */
    public void render(SpriteBatch batch, ShapeRenderer shape, float delta) {
        if (!open) return;
        openTimer = Math.min(openTimer + delta * 6f, 1f);
        float scale = 0.85f + 0.15f * openTimer; // spring-open effect

        // ── Update hover ─────────────────────────────────────────────────────
        float mx = Gdx.input.getX();
        float my = SCREEN_H - Gdx.input.getY();
        hoveredItem = -1;
        for (int i = 0; i < itemRects.length; i++) {
            if (itemRects[i].contains(mx, my)) { hoveredItem = i; break; }
        }

        // Dimensi yang di-scale untuk animasi buka
        float bx = BOX_X + BOX_W / 2f * (1f - scale);
        float by = BOX_Y + BOX_H / 2f * (1f - scale);
        float bw = BOX_W * scale;
        float bh = BOX_H * scale;

        com.badlogic.gdx.math.Matrix4 uiProj = new com.badlogic.gdx.math.Matrix4();
        uiProj.setToOrtho2D(0, 0, SCREEN_W, SCREEN_H);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.setProjectionMatrix(uiProj);

        // ── Background overlay gelap ──────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.55f * openTimer);
        shape.rect(0, 0, SCREEN_W, SCREEN_H);
        shape.end();

        // ── Kotak menu utama ──────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_BG);
        shape.rect(bx, by, bw, bh);
        shape.end();

        // ── Border gold ───────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_BORDER);
        float bt = 3f; // border thickness
        shape.rect(bx,      by,      bw, bt);
        shape.rect(bx,      by+bh-bt, bw, bt);
        shape.rect(bx,      by,      bt, bh);
        shape.rect(bx+bw-bt,by,      bt, bh);
        shape.end();

        // ── Garis dekoratif di bawah judul ───────────────────────────────────
        float titleLineY = ITEMS_START_Y - 10f;
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COL_BORDER.r, COL_BORDER.g, COL_BORDER.b, 0.5f);
        shape.rect(bx + 20f, titleLineY, bw - 40f, 1.5f);
        shape.end();

        // ── Item menu (Gambar tekstur) ───────────────────────────────────────
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(uiProj);
        batch.begin();

        // Judul MENU
        titleFont.setColor(COL_BORDER);
        titleFont.draw(batch, "MENU", bx, by + bh - 22f, bw, Align.center, false);

        // Sub-judul "PAUSED"
        itemFont.setColor(0.6f, 0.6f, 0.7f, 1f);
        itemFont.draw(batch, "— GAME PAUSED —", bx, by + bh - 46f, bw, Align.center, false);

        // Render tombol menu menggunakan aset tekstur dengan efek hover premium
        for (int i = 0; i < 3; i++) {
            Rectangle r = itemRects[i];
            boolean hovered = (hoveredItem == i);
            Texture tex = (i == 0) ? optionTexture : ((i == 1) ? achievementTexture : taskTexture);

            if (hovered) {
                // Perbesaran skala 1.05x dari pusat tombol dan sedikit tint kuning
                float newW = r.width * 1.05f;
                float newH = r.height * 1.05f;
                float newX = r.x - (newW - r.width) / 2f;
                float newY = r.y - (newH - r.height) / 2f;

                batch.setColor(1f, 1f, 0.8f, 1f);
                batch.draw(tex, newX, newY, newW, newH);
                batch.setColor(Color.WHITE);

                itemFont.setColor(Color.YELLOW);
                itemFont.draw(batch, LABELS[i], newX, newY + newH - (newH - r.height)/2f - 16f, newW, Align.center, false);
            } else {
                batch.draw(tex, r.x, r.y, r.width, r.height);
                itemFont.setColor(Color.WHITE);
                itemFont.draw(batch, LABELS[i], r.x, r.y + r.height - 16f, r.width, Align.center, false);
            }
        }

        batch.end();
    }

    /** Dispose font yang dibuat oleh GameMenu ini sendiri (bukan yg diinjeksi dari luar). */
    public void dispose() {
        if (optionTexture != null) optionTexture.dispose();
        if (achievementTexture != null) achievementTexture.dispose();
        if (taskTexture != null) taskTexture.dispose();
    }
}
