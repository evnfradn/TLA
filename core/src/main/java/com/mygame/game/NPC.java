package com.mygame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class NPC {
    private final String name;
    private final float x;
    private final float y;
    private final String assetFolder;
    private final Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> lookLeftAnimation = null;
    private boolean lookingLeft = false;
    private float stateTime;

    public NPC(String name, float x, float y, String assetFolder, String direction, Array<Texture> allTextures) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.assetFolder = assetFolder;
        this.stateTime = (float) (Math.random() * 2f);

        TextureRegion[] frames = new TextureRegion[2];
        for (int i = 1; i <= 2; i++) {
            String path = "Eksplore/NPC School/" + assetFolder + "/standard/idle/" + direction + "/" + i + ".png";
            Texture tex = new Texture(Gdx.files.internal(path));
            allTextures.add(tex);
            frames[i - 1] = new TextureRegion(tex);
        }
        this.idleAnimation = new Animation<>(0.25f + (float) Math.random() * 0.05f, frames);
        this.idleAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }

    /** Preload "look left" (toward window) animation untuk event cutscene. */
    public void loadLookLeftAnimation(Array<Texture> allTextures) {
        if (lookLeftAnimation != null) return; // already loaded
        try {
            String p1 = "Eksplore/NPC School/" + assetFolder + "/standard/idle/left/1.png";
            String p2 = "Eksplore/NPC School/" + assetFolder + "/standard/idle/left/2.png";
            if (!Gdx.files.internal(p1).exists()) return;
            TextureRegion[] frames = new TextureRegion[2];
            for (int i = 1; i <= 2; i++) {
                String path = "Eksplore/NPC School/" + assetFolder + "/standard/idle/left/" + i + ".png";
                Texture tex = new Texture(Gdx.files.internal(path));
                allTextures.add(tex);
                frames[i - 1] = new TextureRegion(tex);
            }
            lookLeftAnimation = new Animation<>(0.3f, frames);
            lookLeftAnimation.setPlayMode(Animation.PlayMode.LOOP);
        } catch (Exception e) {
            Gdx.app.log("NPC", "Could not load left anim for " + assetFolder);
        }
    }

    public void setLookingLeft(boolean looking) { this.lookingLeft = looking; }

    public void update(float delta) { stateTime += delta; }

    public TextureRegion getCurrentFrame() {
        if (lookingLeft && lookLeftAnimation != null) return lookLeftAnimation.getKeyFrame(stateTime);
        return idleAnimation.getKeyFrame(stateTime);
    }

    public String getName()  { return name; }
    public float  getX()     { return x; }
    public float  getY()     { return y; }
}
