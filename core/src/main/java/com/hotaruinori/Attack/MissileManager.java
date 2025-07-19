package com.hotaruinori.Attack;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.hotaruinori.Plays.Character; // 需要引入 Character 類別來進行碰撞檢測

public class MissileManager {
    private Array<Missile> activeMissiles;
    private Character playerCharacter; // MissileManager 需要知道玩家在哪裡以便傳給飛彈更新

    public MissileManager() {
        activeMissiles = new Array<>();
    }

    // 設置玩家角色，供飛彈追蹤和碰撞檢測使用
    public void setPlayerCharacter(Character player) {
        this.playerCharacter = player;
    }

    // 由發射者（例如 BossA）呼叫來添加單發飛彈
    public void addMissile(float startX, float startY, float speed, float damage) {
        // 直接使用 Missile 的第一種建構子
        Missile newMissile = new Missile(startX, startY, playerCharacter, speed, damage);
        activeMissiles.add(newMissile);
    }

    // 由發射者（例如 BossA）呼叫來添加扇形飛彈
    public void addSpreadMissiles(float startX, float startY, int numberOfMissiles, float spreadAngle, float missileSpeed, float missileDamage) {
        if (playerCharacter == null) return;

        Vector2 origin = new Vector2(startX, startY);
        Vector2 target = playerCharacter.getCenterPosition();

        Vector2 directionToPlayer = new Vector2(target).sub(origin);
        float baseAngle = directionToPlayer.angleDeg();

        float angleStep = 0;
        if (numberOfMissiles > 1) {
            angleStep = spreadAngle / (numberOfMissiles - 1);
        }
        float startAngle = baseAngle - spreadAngle / 2;

        for (int i = 0; i < numberOfMissiles; i++) {
            float currentAngle = startAngle + i * angleStep;
            // 使用 Missile 的第二種建構子
            Missile newMissile = new Missile(origin.x, origin.y, playerCharacter, missileSpeed, missileDamage, currentAngle);
            activeMissiles.add(newMissile);
        }
    }


    // 在遊戲迴圈中更新所有飛彈
    public void update(float deltaTime) {
        for (int i = activeMissiles.size - 1; i >= 0; i--) {
            Missile missile = activeMissiles.get(i);
            missile.update(deltaTime, playerCharacter); // 將 playerCharacter 傳給飛彈進行更新和碰撞檢測
            if (!missile.isActive()) {
                missile.dispose();
                activeMissiles.removeIndex(i);
            }
        }
    }

    // 在遊戲迴圈中渲染所有飛彈
    public void render(SpriteBatch batch) {
        for (Missile missile : activeMissiles) {
            missile.render(batch);
        }
    }

    // 釋放所有飛彈資源
    public void dispose() {
        for (Missile missile : activeMissiles) {
            missile.dispose();
        }
        activeMissiles.clear();
    }
}
