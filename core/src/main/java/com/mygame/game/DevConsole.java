package com.mygame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Developer Console — diaktifkan dengan menekan '/' (slash).
 *
 * Command yang tersedia:
 *   /tp <x> <y>        — Teleport pemain ke koordinat
 *   /speed <value>     — Atur multiplier kecepatan (1.0 = normal)
 *   /god               — Toggle god mode (kebal)
 *   /noclip            — Toggle no-clip (tembus tembok)
 *   /skipintro         — Skip intro cutscene
 *   /restart           — Mulai ulang cerita dari awal
 *   /rupture           — Trigger transisi RUPTURE secara instan
 *   /cinematic         — Trigger Cinematic Jaws letterbox overlay
 *   /hitbox            — Toggle hitbox/collision display
 *   /fps               — Toggle FPS & memory monitor
 *   /log               — Toggle action log panel
 *   /debug             — Toggle SEMUA debug overlay
 *   /help              — Tampilkan semua command
 *   /clear             — Bersihkan riwayat console
 *   /heal              — Pulihkan HP pemain ke penuh (BattleScreen)
 *   /killboss          — Bunuh boss seketika (BattleScreen)
 *   /setstage <n>      — Set boss stage ke 1/2/3 (BattleScreen)
 */
public class DevConsole {

    // ── Singleton Instance ───────────────────────────────────────────────
    private static DevConsole instance;
    public static DevConsole getInstance() {
        if (instance == null) {
            instance = new DevConsole();
        }
        return instance;
    }

    // ── Daftar command untuk menu saran ──────────────────────────────────
    public static final String[][] COMMANDS = {
        {"/tp <x> <y>",     "Teleport ke koordinat tertentu"},
        {"/battle",         "Masuk ke BattleScreen secara instan"},
        {"/speed <value>",  "Atur kecepatan (1.0 = normal)"},
        {"/god",            "Toggle god mode (kebal)"},
        {"/noclip",         "Toggle no-clip (tembus tembok)"},
        {"/skipintro",      "Skip intro cutscene"},
        {"/restart",        "Mulai ulang cerita dari awal"},
        {"/rupture",        "Trigger transisi RUPTURE secara instan"},
        {"/cinematic",      "Trigger Cinematic Jaws letterbox overlay"},
        {"/hitbox",         "Toggle hitbox display"},
        {"/collision",      "Toggle map collision display"},
        {"/reload",         "Reload map, assets, dan screen saat ini"},
        {"/fps",            "Toggle FPS & memory monitor"},
        {"/log",            "Toggle action log panel"},
        {"/debug",          "Toggle SEMUA debug overlay"},
        {"/heal",           "Pulihkan HP & EN penuh (Battle)"},
        {"/killboss",       "Bunuh boss seketika (Battle)"},
        {"/setstage <n>",   "Set boss stage 1/2/3 (Battle)"},
        {"/help",           "Tampilkan daftar command"},
        {"/clear",          "Bersihkan riwayat console"},
    };

    // ── State ─────────────────────────────────────────────────────────────
    private boolean open = false;
    private final StringBuilder inputBuffer = new StringBuilder();
    private final List<ConsoleEntry> history = new ArrayList<>();
    private int selectedSuggestion = -1;
    private final List<String[]> suggestions = new ArrayList<>();

    // ── Cheat flags (dibaca oleh Screen yang aktif) ───────────────────────
    private boolean godMode       = false;
    private boolean noClip        = false;
    private float   speedMulti    = 1.0f;
    private boolean showHitbox    = false;
    private boolean showCollision = false;
    private boolean showFps       = false;
    private boolean showActionLog = false;

    private boolean requestSkipIntro  = false;
    private boolean requestTeleport   = false;
    private boolean requestRestart    = false;
    private boolean requestRupture    = false;
    private boolean requestCinematic  = false;
    private boolean requestHeal       = false;
    private boolean requestKillBoss   = false;
    private boolean requestSetStage   = false;
    private boolean requestBattle     = false;
    private boolean requestReload     = false;
    private int     requestedStage    = 1;

    private float teleportX = 0f, teleportY = 0f;

    // ── Konstanta visual ─────────────────────────────────────────────────
    private static final float CONSOLE_HEIGHT    = 42f;
    private static final float HISTORY_LINE_H    = 22f;
    private static final float SUGGESTION_LINE_H = 28f;
    private static final float CONSOLE_PAD       = 12f;
    private static final int   MAX_HISTORY       = 50;
    private static final float CURSOR_BLINK_SPEED = 0.5f;

    private float cursorTimer = 0f;
    private float slideAnim   = 0f; // 0 = tertutup, 1 = terbuka penuh

    // ── Entry console dengan warna ────────────────────────────────────────
    public static class ConsoleEntry {
        public final String text;
        public final Color  color;
        public ConsoleEntry(String text, Color color) {
            this.text  = text;
            this.color = color;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INPUT HANDLING
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Panggil setiap frame dari Screen. Mengembalikan true jika console
     * mengkonsumsi input (screen tidak perlu memproses input lagi).
     */
    public boolean handleInput() {
        // Toggle console dengan tombol '/'
        if (Gdx.input.isKeyJustPressed(Input.Keys.SLASH)) {
            if (!open) {
                open = true;
                inputBuffer.setLength(0);
                inputBuffer.append('/');
                selectedSuggestion = -1;
                updateSuggestions();
                return true;
            }
        }

        if (!open) return false;

        // Escape menutup console
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            open = false;
            return true;
        }

        // Enter menjalankan command atau memilih saran
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                String cmd = suggestions.get(selectedSuggestion)[0];
                String base = cmd.split(" <")[0];
                inputBuffer.setLength(0);
                inputBuffer.append(base).append(' ');
                selectedSuggestion = -1;
                updateSuggestions();
            } else {
                executeCommand(inputBuffer.toString().trim());
                inputBuffer.setLength(0);
                open = false;
            }
            return true;
        }

        // Tab untuk autocomplete
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            if (!suggestions.isEmpty()) {
                int idx = selectedSuggestion >= 0 ? selectedSuggestion : 0;
                String cmd = suggestions.get(idx)[0];
                String base = cmd.split(" <")[0];
                inputBuffer.setLength(0);
                inputBuffer.append(base).append(' ');
                selectedSuggestion = -1;
                updateSuggestions();
            }
            return true;
        }

        // Navigasi saran dengan tombol panah
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            if (!suggestions.isEmpty())
                selectedSuggestion = Math.max(0, selectedSuggestion - 1);
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            if (!suggestions.isEmpty())
                selectedSuggestion = Math.min(suggestions.size() - 1, selectedSuggestion + 1);
            return true;
        }

        // Backspace
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) || Gdx.input.isKeyJustPressed(Input.Keys.DEL)) {
            if (inputBuffer.length() > 0) {
                inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                selectedSuggestion = -1;
                updateSuggestions();
            }
            if (inputBuffer.length() == 0) {
                open = false;
            }
            return true;
        }

        return true; // konsumsi semua input selama console terbuka
    }

    /** Panggil dari keyTyped di InputProcessor. */
    public boolean keyTyped(char character) {
        if (!open) return false;
        if (character == '/' && inputBuffer.length() == 0) {
            inputBuffer.append('/');
            updateSuggestions();
            return true;
        }
        // Hanya terima karakter yang bisa dicetak
        if (character >= 32 && character != 127 && character != '/') {
            inputBuffer.append(character);
            selectedSuggestion = -1;
            updateSuggestions();
            return true;
        }
        return true;
    }

    private void updateSuggestions() {
        suggestions.clear();
        String input = inputBuffer.toString().toLowerCase().trim();
        if (input.isEmpty()) return;
        for (String[] cmd : COMMANDS) {
            if (cmd[0].toLowerCase().startsWith(input) || input.equals("/")) {
                suggestions.add(cmd);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  EKSEKUSI COMMAND
    // ══════════════════════════════════════════════════════════════════════

    public void executeCommand(String raw) {
        if (raw.isEmpty()) return;

        addHistory("> " + raw, new Color(0.6f, 0.8f, 1.0f, 1f));

        String[] parts = raw.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/tp":
                if (parts.length >= 3) {
                    try {
                        teleportX = Float.parseFloat(parts[1]);
                        teleportY = Float.parseFloat(parts[2]);
                        requestTeleport = true;
                        addHistory("  Teleported to (" + (int) teleportX + ", " + (int) teleportY + ")",
                            new Color(0.4f, 1f, 0.5f, 1f));
                    } catch (NumberFormatException e) {
                        addHistory("  ERROR: Koordinat harus angka! Contoh: /tp 400 300",
                            new Color(1f, 0.4f, 0.4f, 1f));
                    }
                } else {
                    addHistory("  Penggunaan: /tp <x> <y>", new Color(1f, 0.85f, 0.3f, 1f));
                }
                break;

            case "/speed":
                if (parts.length >= 2) {
                    try {
                        speedMulti = Float.parseFloat(parts[1]);
                        speedMulti = Math.max(0.1f, Math.min(10f, speedMulti));
                        addHistory("  Speed set to " + speedMulti + "x",
                            new Color(0.4f, 1f, 0.5f, 1f));
                    } catch (NumberFormatException e) {
                        addHistory("  ERROR: Harus angka! Contoh: /speed 2.5",
                            new Color(1f, 0.4f, 0.4f, 1f));
                    }
                } else {
                    addHistory("  Penggunaan: /speed <value>  (sekarang: " + speedMulti + "x)",
                        new Color(1f, 0.85f, 0.3f, 1f));
                }
                break;

            case "/god":
                godMode = !godMode;
                addHistory("  God Mode: " + (godMode ? "ON" : "OFF"),
                    godMode ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.5f, 0.3f, 1f));
                break;

            case "/noclip":
                noClip = !noClip;
                addHistory("  No-Clip: " + (noClip ? "ON" : "OFF"),
                    noClip ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.5f, 0.3f, 1f));
                break;

            case "/skipintro":
                requestSkipIntro = true;
                addHistory("  Intro cutscene skipped!", new Color(0.4f, 1f, 0.5f, 1f));
                break;

            case "/restart":
                requestRestart = true;
                addHistory("  Restarting story from the beginning...", new Color(0.4f, 1f, 0.5f, 1f));
                break;

            case "/rupture":
                requestRupture = true;
                addHistory("  Triggering Rupture cutscene instantly...", new Color(0.4f, 1f, 0.5f, 1f));
                break;

            case "/cinematic":
                requestCinematic = true;
                addHistory("  Triggering Cinematic Jaws letterbox overlay...", new Color(0.4f, 1f, 0.5f, 1f));
                break;

            case "/hitbox":
                showHitbox = !showHitbox;
                addHistory("  Hitbox Display: " + (showHitbox ? "ON" : "OFF"),
                    showHitbox ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.5f, 0.3f, 1f));
                break;

            case "/collision":
                showCollision = !showCollision;
                addHistory("  Collision Layer Display: " + (showCollision ? "ON" : "OFF"),
                    showCollision ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.5f, 0.3f, 1f));
                break;

            case "/reload":
                requestReload = true;
                addHistory("  Reloading current screen and assets...", new Color(0.4f, 1f, 0.5f, 1f));
                break;

            case "/fps":
                showFps = !showFps;
                addHistory("  FPS Monitor: " + (showFps ? "ON" : "OFF"),
                    showFps ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.5f, 0.3f, 1f));
                break;

            case "/log":
                showActionLog = !showActionLog;
                addHistory("  Action Log: " + (showActionLog ? "ON" : "OFF"),
                    showActionLog ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.5f, 0.3f, 1f));
                break;

            case "/debug":
                boolean allOn = showHitbox && showFps && showActionLog;
                showHitbox    = !allOn;
                showFps       = !allOn;
                showActionLog = !allOn;
                addHistory("  All Debug Overlays: " + (!allOn ? "ON" : "OFF"),
                    !allOn ? new Color(0.4f, 1f, 0.5f, 1f) : new Color(1f, 0.5f, 0.3f, 1f));
                break;

            case "/heal":
                requestHeal = true;
                addHistory("  Player HP & EN dipulihkan penuh!", new Color(0.4f, 1f, 0.5f, 1f));
                break;

            case "/killboss":
                requestKillBoss = true;
                addHistory("  Boss dihancurkan seketika!", new Color(0.4f, 1f, 0.5f, 1f));
                break;

            case "/setstage":
                if (parts.length >= 2) {
                    try {
                        int stage = Integer.parseInt(parts[1]);
                        if (stage >= 1 && stage <= 3) {
                            requestedStage = stage;
                            requestSetStage = true;
                            addHistory("  Boss stage diset ke " + stage, new Color(0.4f, 1f, 0.5f, 1f));
                        } else {
                            addHistory("  ERROR: Stage harus 1, 2, atau 3", new Color(1f, 0.4f, 0.4f, 1f));
                        }
                    } catch (NumberFormatException e) {
                        addHistory("  ERROR: Harus angka! Contoh: /setstage 2", new Color(1f, 0.4f, 0.4f, 1f));
                    }
                } else {
                    addHistory("  Penggunaan: /setstage <1|2|3>", new Color(1f, 0.85f, 0.3f, 1f));
                }
                break;

            case "/battle":
                requestBattle = true;
                addHistory("  Memasuki layar pertempuran...", new Color(0.4f, 1f, 0.5f, 1f));
                break;

            case "/help":
                addHistory("  ═══ DEVELOPER COMMANDS ═══", new Color(0.85f, 0.72f, 0.22f, 1f));
                for (String[] c : COMMANDS) {
                    addHistory("  " + c[0] + "  — " + c[1], new Color(0.8f, 0.8f, 0.9f, 1f));
                }
                break;

            case "/clear":
                history.clear();
                addHistory("  Console cleared.", new Color(0.6f, 0.6f, 0.7f, 1f));
                break;

            default:
                addHistory("  Unknown command: " + cmd + "  (ketik /help)",
                    new Color(1f, 0.4f, 0.4f, 1f));
                break;
        }
    }

    private void addHistory(String text, Color color) {
        history.add(new ConsoleEntry(text, color));
        while (history.size() > MAX_HISTORY) history.remove(0);
    }

    /** Tambah event eksternal ke action log (dipanggil dari Screen). */
    public void logAction(String message) {
        addHistory("[EVENT] " + message, new Color(0.5f, 0.9f, 1f, 1f));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UPDATE & RENDER
    // ══════════════════════════════════════════════════════════════════════

    public void update(float delta) {
        cursorTimer += delta;

        float target = open ? 1f : 0f;
        float speed  = 8f;
        slideAnim += (target - slideAnim) * speed * delta;
        if (Math.abs(slideAnim - target) < 0.01f) slideAnim = target;
    }

    /**
     * Render UI console. Harus dipanggil dalam screen-space (setelah camera
     * projection di-reset ke koordinat layar 0,0).
     *
     * @param batch    SpriteBatch untuk render teks
     * @param shapes   ShapeRenderer untuk render kotak latar belakang
     * @param font     BitmapFont yang akan digunakan
     * @param screenW  Lebar layar virtual
     * @param screenH  Tinggi layar virtual
     */
    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                       float screenW, float screenH) {
        if (slideAnim <= 0.01f && history.isEmpty()) return;

        float alpha = Math.min(1f, slideAnim * 2f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // ── Panel riwayat (di atas input bar) ────────────────────────────
        if (!history.isEmpty() && slideAnim > 0.1f) {
            int showLines = Math.min(history.size(), 8);
            float histH = showLines * HISTORY_LINE_H + 12f;

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.04f, 0.04f, 0.08f, 0.85f * alpha);
            shapes.rect(0, screenH - CONSOLE_HEIGHT * slideAnim - histH, screenW, histH);
            // Aksen bar kiri emas
            shapes.setColor(0.85f, 0.72f, 0.22f, 0.6f * alpha);
            shapes.rect(0, screenH - CONSOLE_HEIGHT * slideAnim - histH, 3f, histH);
            shapes.end();

            batch.begin();
            float origSx = font.getData().scaleX;
            float origSy = font.getData().scaleY;
            font.getData().setScale(0.85f);

            for (int i = 0; i < showLines; i++) {
                ConsoleEntry entry = history.get(history.size() - showLines + i);
                float lineY = screenH - CONSOLE_HEIGHT * slideAnim - histH
                    + histH - 10f - i * HISTORY_LINE_H;
                font.setColor(entry.color.r, entry.color.g, entry.color.b, alpha);
                font.draw(batch, entry.text, CONSOLE_PAD + 6f, lineY);
            }

            font.getData().setScale(origSx, origSy);
            batch.end();
        }

        // ── Popup saran command ──────────────────────────────────────────
        if (open && !suggestions.isEmpty()) {
            float sugH = suggestions.size() * SUGGESTION_LINE_H + 8f;
            float sugY  = screenH - CONSOLE_HEIGHT * slideAnim
                - (history.isEmpty() ? 0 : Math.min(history.size(), 8) * HISTORY_LINE_H + 12f)
                - sugH;

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.06f, 0.08f, 0.14f, 0.92f * alpha);
            shapes.rect(CONSOLE_PAD, sugY, screenW * 0.55f, sugH);
            shapes.setColor(0.85f, 0.72f, 0.22f, 0.3f * alpha);
            shapes.rect(CONSOLE_PAD, sugY, screenW * 0.55f, 2f);
            shapes.rect(CONSOLE_PAD, sugY + sugH - 2f, screenW * 0.55f, 2f);
            shapes.end();

            batch.begin();
            float origSx = font.getData().scaleX;
            float origSy = font.getData().scaleY;
            font.getData().setScale(0.85f);

            for (int i = 0; i < suggestions.size(); i++) {
                float lineY = sugY + sugH - 6f - i * SUGGESTION_LINE_H;
                boolean selected = (i == selectedSuggestion);

                if (selected) {
                    batch.end();
                    shapes.begin(ShapeRenderer.ShapeType.Filled);
                    shapes.setColor(0.85f, 0.72f, 0.22f, 0.15f * alpha);
                    shapes.rect(CONSOLE_PAD, lineY - SUGGESTION_LINE_H + 6f,
                        screenW * 0.55f, SUGGESTION_LINE_H);
                    shapes.end();
                    batch.begin();
                }

                // Nama command (emas)
                font.setColor(0.85f, 0.72f, 0.22f, alpha);
                font.draw(batch, suggestions.get(i)[0], CONSOLE_PAD + 10f, lineY);
                // Deskripsi (abu-abu)
                font.setColor(0.6f, 0.6f, 0.7f, alpha * 0.8f);
                font.draw(batch, suggestions.get(i)[1], CONSOLE_PAD + 200f, lineY);
            }

            font.getData().setScale(origSx, origSy);
            batch.end();
        }

        // ── Input bar ────────────────────────────────────────────────────
        if (open) {
            float barY = screenH - CONSOLE_HEIGHT * slideAnim;

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.07f, 0.09f, 0.17f, 0.95f * alpha);
            shapes.rect(0, barY, screenW, CONSOLE_HEIGHT);
            // Border atas (emas)
            shapes.setColor(0.85f, 0.72f, 0.22f, alpha);
            shapes.rect(0, barY, screenW, 2f);
            // Border bawah (emas tipis)
            shapes.setColor(0.85f, 0.72f, 0.22f, 0.3f * alpha);
            shapes.rect(0, barY + CONSOLE_HEIGHT - 1f, screenW, 1f);
            shapes.end();

            batch.begin();
            float origSx = font.getData().scaleX;
            float origSy = font.getData().scaleY;
            font.getData().setScale(1.1f);

            // Prompt ">"
            font.setColor(0.85f, 0.72f, 0.22f, alpha);
            font.draw(batch, ">", CONSOLE_PAD, barY + CONSOLE_HEIGHT - 12f);

            // Teks input
            String input = inputBuffer.toString();
            font.setColor(1f, 1f, 1f, alpha);
            font.draw(batch, input, CONSOLE_PAD + 18f, barY + CONSOLE_HEIGHT - 12f);

            // Kursor berkedip
            boolean cursorVisible = ((int) (cursorTimer / CURSOR_BLINK_SPEED)) % 2 == 0;
            if (cursorVisible) {
                GlyphLayout gl = new GlyphLayout(font, input);
                font.setColor(0.85f, 0.72f, 0.22f, alpha);
                font.draw(batch, "_", CONSOLE_PAD + 18f + gl.width + 2f,
                    barY + CONSOLE_HEIGHT - 12f);
            }

            font.getData().setScale(origSx, origSy);
            batch.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GETTERS — dibaca oleh ExplorationScreen & BattleScreen
    // ══════════════════════════════════════════════════════════════════════

    public boolean isOpen()          { return open; }
    public boolean isGodMode()       { return godMode; }
    public boolean isNoClip()        { return noClip; }
    public float   getSpeedMulti()   { return speedMulti; }
    public boolean isShowHitbox()    { return showHitbox; }
    public boolean isShowCollision() { return showCollision; }
    public boolean isShowFps()       { return showFps; }
    public boolean isShowActionLog() { return showActionLog; }

    /** Konsumsi flag sekali-pakai: reload. */
    public boolean consumeReload() {
        boolean v = requestReload;
        requestReload = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: skip intro. */
    public boolean consumeSkipIntro() {
        boolean v = requestSkipIntro;
        requestSkipIntro = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: teleport. */
    public boolean consumeTeleport() {
        boolean v = requestTeleport;
        requestTeleport = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: restart. */
    public boolean consumeRestart() {
        boolean v = requestRestart;
        requestRestart = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: rupture. */
    public boolean consumeRupture() {
        boolean v = requestRupture;
        requestRupture = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: cinematic. */
    public boolean consumeCinematic() {
        boolean v = requestCinematic;
        requestCinematic = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: heal penuh (BattleScreen). */
    public boolean consumeHeal() {
        boolean v = requestHeal;
        requestHeal = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: kill boss (BattleScreen). */
    public boolean consumeKillBoss() {
        boolean v = requestKillBoss;
        requestKillBoss = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: set boss stage (BattleScreen). */
    public boolean consumeSetStage() {
        boolean v = requestSetStage;
        requestSetStage = false;
        return v;
    }

    /** Konsumsi flag sekali-pakai: masuk battle. */
    public boolean consumeBattle() {
        boolean v = requestBattle;
        requestBattle = false;
        return v;
    }

    /** Stage yang diminta oleh /setstage command. */
    public int getRequestedStage() { return requestedStage; }

    public float getTeleportX() { return teleportX; }
    public float getTeleportY() { return teleportY; }
}
