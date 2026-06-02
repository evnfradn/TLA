package com.mygame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Sistem simpan/muat progres menggunakan LibGDX Preferences (disimpan secara lokal).
 *
 * Data yang disimpan:
 *  - Nama map terakhir (mapId)
 *  - Posisi X & Y player terakhir
 *  - Nilai gold & gems
 */
public class GameSave {

    private static final String PREF_NAME  = "tla_save";
    private static final String KEY_MAP    = "map";
    private static final String KEY_X      = "px";
    private static final String KEY_Y      = "py";
    private static final String KEY_GOLD   = "gold";
    private static final String KEY_GEMS   = "gems";
    private static final String KEY_EXISTS = "exists";

    // ── Simpan ────────────────────────────────────────────────────────────────
    public static void save(String mapId, float px, float py, int gold, int gems) {
        Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
        prefs.putBoolean(KEY_EXISTS, true);
        prefs.putString(KEY_MAP,  mapId);
        prefs.putFloat(KEY_X,     px);
        prefs.putFloat(KEY_Y,     py);
        prefs.putInteger(KEY_GOLD, gold);
        prefs.putInteger(KEY_GEMS, gems);
        prefs.flush();
        Gdx.app.log("GameSave", "Saved: map=" + mapId + " x=" + px + " y=" + py);
    }

    // ── Muat ─────────────────────────────────────────────────────────────────
    public static boolean hasSave() {
        return Gdx.app.getPreferences(PREF_NAME).getBoolean(KEY_EXISTS, false);
    }

    public static String loadMap()  { return Gdx.app.getPreferences(PREF_NAME).getString(KEY_MAP,  "classroom"); }
    public static float  loadX()    { return Gdx.app.getPreferences(PREF_NAME).getFloat(KEY_X,    877f); }
    public static float  loadY()    { return Gdx.app.getPreferences(PREF_NAME).getFloat(KEY_Y,    447f); }
    public static int    loadGold() { return Gdx.app.getPreferences(PREF_NAME).getInteger(KEY_GOLD, 100); }
    public static int    loadGems() { return Gdx.app.getPreferences(PREF_NAME).getInteger(KEY_GEMS, 0);  }

    public static void clear() {
        Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
        prefs.clear();
        prefs.flush();
        Gdx.app.log("GameSave", "Save cleared.");
    }
}
