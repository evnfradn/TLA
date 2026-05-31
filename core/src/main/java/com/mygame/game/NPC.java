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
    private final Animation<TextureRegion> idleAnimation;
    private float stateTime;

    public NPC(String name, float x, float y, String assetFolder, String direction, Array<Texture> allTextures) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.stateTime = (float) (Math.random() * 2f); // Random start offset so they aren't in sync

        TextureRegion[] frames = new TextureRegion[2];
        for (int i = 1; i <= 2; i++) {
            String path = "Eksplore/NPC School/" + assetFolder + "/standard/idle/" + direction + "/" + i + ".png";
            Texture tex = new Texture(Gdx.files.internal(path));
            allTextures.add(tex);
            frames[i - 1] = new TextureRegion(tex);
        }
        this.idleAnimation = new Animation<>(0.25f + (float) Math.random() * 0.05f, frames); // sedikit diacak agar ritme bernapas berbeda
        this.idleAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public TextureRegion getCurrentFrame() {
        return idleAnimation.getKeyFrame(stateTime);
    }

    public String getName() {
        return name;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
