package com.hotaruinori.Plays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * CharacterMovement 是一個專責類別，負責處理角色的鍵盤與觸控移動邏輯。
 * 提供一個 public 方法 handle()，讓 Main.input() 呼叫。
 */
public class CharacterMovement {
    private final com.hotaruinori.Plays.Character character;
    private final Viewport viewport;
    private final Vector2 touchPos = new Vector2();

    public CharacterMovement(Character character, Viewport viewport) {
        this.character = character;
        this.viewport = viewport;
    }

    /**
     * 處理角色的移動控制：鍵盤或觸控輸入。
     * @param delta 畫面更新時間間隔（秒）
     */
    public void handle(float delta) {
        float speed = 4f;
        boolean moving = false;
        Vector2 direction = new Vector2();

        // 處理鍵盤輸入（現在可以同時檢測多個按鍵）
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) direction.x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) direction.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) direction.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) direction.y -= 1;

        if (!direction.isZero()) {
            character.moveWithDirection(delta, direction, speed);
            moving = true;
        }

        // 觸控控制，目前確認滑鼠移動OK
        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);
            character.moveTo(touchPos.x, touchPos.y);
            moving = true;
        }

        // 更新角色動畫狀態
        character.update(delta, moving);
        character.updateMovement(delta);
    }
}
