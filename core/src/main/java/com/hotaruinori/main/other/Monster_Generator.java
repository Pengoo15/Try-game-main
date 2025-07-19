package com.hotaruinori.main.other;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.hotaruinori.Attack.MissileManager; // <--- 新增引入
import com.hotaruinori.Plays.Character;
import com.hotaruinori.monstars.BOSS.BossA;

public class Monster_Generator {
    private Array<BossA> monsters;
    private com.hotaruinori.Plays.Character character;    // 傳入主角方便給怪物AI用
    private Camera camera;       // 取得攝影機資訊，用來決定生成邊界
    private float spawnInterval; // 生成間隔秒數
    private float timeSinceLastSpawn; // 計時器
    private MissileManager missileManager;

    public Monster_Generator(Character character, Camera camera, float spawnIntervalSeconds, MissileManager missileManager) {
        this.monsters = new Array<>();
        this.character = character;
        this.camera = camera;
        this.spawnInterval = spawnIntervalSeconds;
        this.timeSinceLastSpawn = 0f;
        this.missileManager = missileManager;  // 接收管理器
    }
    public Array<BossA> getMonsters() {
        return monsters;
    }

    public void update(float deltaTime) {
        timeSinceLastSpawn += deltaTime;

        if (timeSinceLastSpawn >= spawnInterval) {
            spawnMonsterAtEdge();
            timeSinceLastSpawn = 0f;
        }

        // 更新怪物的 AI
        for (BossA boss : monsters) {
            if (boss.isAlive()) {
                boss.getMonsterAI().update(deltaTime);
            }
        }
    }

    // 在螢幕邊緣隨機位置生成怪物
    private void spawnMonsterAtEdge() {
        // 取得攝影機視窗邊界（世界座標）
        float camLeft = camera.position.x - camera.viewportWidth / 2f;
        float camRight = camera.position.x + camera.viewportWidth / 2f;
        float camBottom = camera.position.y - camera.viewportHeight / 2f;
        float camTop = camera.position.y + camera.viewportHeight / 2f;

        // 隨機選擇生成在哪一邊：0=左，1=右，2=上，3=下
        int edge = MathUtils.random(3);
        float x = 0f, y = 0f;

        switch (edge) {
            case 0: // 左邊
                x = camLeft;
                y = MathUtils.random(camBottom, camTop);
                break;
            case 1: // 右邊
                x = camRight;
                y = MathUtils.random(camBottom, camTop);
                break;
            case 2: // 上邊
                x = MathUtils.random(camLeft, camRight);
                y = camTop;
                break;
            case 3: // 下邊
                x = MathUtils.random(camLeft, camRight);
                y = camBottom;
                break;
        }

        BossA newBoss = new BossA();
        newBoss.setPlayer(character);
        newBoss.setX(x);
        newBoss.setY(y);
        missileManager.setPlayerCharacter(character); // <--- 將玩家角色傳給 MissileManager
        newBoss.setMissileManager(missileManager);

        monsters.add(newBoss);

        System.out.println("Monster_Generator: 在邊緣生成 BossA，位置 (" + x + ", " + y + ")");
    }

    public void render(SpriteBatch batch) {
        for (BossA boss : monsters) {
            if (boss.isAlive()) {
                boss.render(batch);
            }
        }
    }

    public void dispose() {
        for (BossA boss : monsters) {
            boss.dispose();
        }
    }

    // 可以加上清理死亡怪物的方法
    public void removeDeadMonsters() {
        for (int i = monsters.size - 1; i >= 0; i--) {
            if (!monsters.get(i).isAlive()) {
                monsters.removeIndex(i);
            }
        }
    }
}
