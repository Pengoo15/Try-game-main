package com.hotaruinori.monstars.BABY; // 假設這個是 Boss 的新路徑

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;
import com.hotaruinori.Plays.Character;


public class BabyA { // 類別名稱改為 Boss

    private Sprite bossDisplaySprite;
    private Character character; // 玩家角色引用

    private float monsterWidth = 0.5f;    // 怪物寬度
    private float monsterHeight = 0.5f;   // 怪物高度

    private float posX = 0.5f;          // 怪物起始X位置
    private float posY = 0.5f;          // 怪物起始Y位置

    private float moveSpeed = 2f;       // 怪物移動速度
    private float collisionDamage = 5f; // 碰撞傷害值 (可以調整)

    // --- 碰撞傷害冷卻相關 ---
    private float collisionCooldownDuration = 1f; // 碰撞傷害的冷卻時間 (秒)
    private float timeSinceLastCollisionDamage = 0f; // 距離上次造成碰撞傷害的時間


    // --- HP 相關變數 ---
    private float maxHealth;
    private float currentHealth;
    private boolean isAlive; // 怪物是否存活

    // 圖片動畫相關
    private TextureAtlas monsterAtlas;
    private Animation<TextureRegion> monsterWalkAnimation;
    private Animation<TextureRegion> monsterIdleAnimation;

    private float stateTime = 0f; // 用於追蹤動畫播放時間
    private CurrentBossState currentBossState; // 用於管理 Boss 當前的動畫狀態

    // 精簡後的 Boss 狀態，只包含追蹤和待機
    public enum CurrentBossState {
        IDLE,
        WALK
    }

    public BabyA() {
        this.currentBossState = CurrentBossState.IDLE; // 初始狀態為待機

        // 載入紋理圖集
        // 請確保你的 monster.atlas 和 monster.png (或其他圖片) 存在於正確路徑
        monsterAtlas = new TextureAtlas(Gdx.files.internal("monsters/monsters/baby.atlas"));

        // 創建行走動畫
        Array<TextureAtlas.AtlasRegion> walkFrames = monsterAtlas.findRegions("baby");
        if (walkFrames.size > 0) {
            monsterWalkAnimation = new Animation<TextureRegion>(0.1f, walkFrames, Animation.PlayMode.LOOP);
        } else {
            Gdx.app.error("Boss", "找不到 'bossWalk2' 動畫幀。請檢查 monster.atlas 文件。");
            // 提供一個預設紋理，以防萬一
            monsterWalkAnimation = new Animation<TextureRegion>(0.1f, new TextureRegion(new Texture(Gdx.files.internal("monsters/baby.png"))));
        }

        // 創建待機動畫 (如果沒有專屬待機，可以沿用行走動畫的第一幀)
        Array<TextureAtlas.AtlasRegion> idleFrames = monsterAtlas.findRegions("baby"); // 假設用同樣的幀作為待機
        if (idleFrames.size > 0) {
            monsterIdleAnimation = new Animation<TextureRegion>(0.15f, idleFrames, Animation.PlayMode.LOOP);
        } else {
            monsterIdleAnimation = new Animation<TextureRegion>(0.15f, monsterWalkAnimation.getKeyFrame(0));
            Gdx.app.error("Boss", "找不到 'bossWalk2' 動畫幀作為待機。");
        }

        // 初始化用於繪製的 Sprite
        bossDisplaySprite = new Sprite(monsterIdleAnimation.getKeyFrame(0)); // 初始使用待機動畫的第一幀
        bossDisplaySprite.setSize(monsterWidth, monsterHeight); // 設定 Boss 在世界單位中的大小
        bossDisplaySprite.setPosition(posX, posY); // 設定初始位置

        this.maxHealth = 100f; // 設定 Boss 的最大生命值 (可以調整)
        this.currentHealth = maxHealth; // 初始生命值等於最大生命值
        this.isAlive = true; // 初始為存活狀態
    }

    // 設定玩家角色的方法
    public void setPlayer(Character character) {
        this.character = character;
    }

    // 更新 Boss 行為和狀態
    public void update(float deltaTime) {
        if (!isAlive || character == null) {
            return; // 如果 Boss 死亡或玩家不存在，則不更新
        }

        stateTime += deltaTime; // 更新動畫時間
        timeSinceLastCollisionDamage += deltaTime; // 更新碰撞傷害冷卻計時

        Vector2 bossCenter = getCenterPosition();
        Vector2 playerCenter = character.getCenterPosition();

        float distance = bossCenter.dst(playerCenter); // 計算 Boss 和玩家之間的距離

        // 追蹤邏輯
        if (distance > (monsterWidth / 2 + character.getWidth() / 2) * 0.8f) { // 當距離大於 Boss 和玩家半徑和的0.8倍時才追蹤，避免過於靠近
            Vector2 direction = new Vector2(playerCenter).sub(bossCenter).nor(); // 計算方向向量並正規化
            float velocityX = direction.x * moveSpeed * deltaTime;
            float velocityY = direction.y * moveSpeed * deltaTime;

            setX(getX() + velocityX);
            setY(getY() + velocityY);

            // 根據移動方向設定 Sprite 的翻轉
            if (velocityX < 0) { // 向左移動
                if (!bossDisplaySprite.isFlipX()) {
                    bossDisplaySprite.flip(true, false);
                }
            } else if (velocityX > 0) { // 向右移動
                if (bossDisplaySprite.isFlipX()) {
                    bossDisplaySprite.flip(true, false);
                }
            }
            setBossState(CurrentBossState.WALK); // 正在移動，切換到行走動畫
        } else {
            // 停止追蹤，可以切換到待機動畫
            setBossState(CurrentBossState.IDLE);
        }

        // 碰撞傷害邏輯
        // 檢查 Boss 的碰撞矩形是否與玩家的碰撞矩形重疊
        if (Boss_Rectangle().overlaps(character.getCharacterRectangle())) {
            if (timeSinceLastCollisionDamage >= collisionCooldownDuration) {
                character.takeDamage(collisionDamage); // 對玩家造成傷害
                System.out.println("Boss 碰撞到玩家，造成 " + collisionDamage + " 點傷害！玩家當前 HP: " + character.getCurrentHealth());
                timeSinceLastCollisionDamage = 0; // 重置碰撞傷害冷卻計時
            }
        }
    }

    // 繪製 Boss
    public void render(SpriteBatch batch) {
        if (!isAlive) {
            return; // 如果 Boss 死亡，不繪製
        }

        TextureRegion currentFrame = null;
        switch (currentBossState) {
            case WALK:
                currentFrame = monsterWalkAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE:
            default: // 預設為待機狀態
                currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                break;
        }

        // 確保 currentFrame 不為 null
        if (currentFrame == null) {
            currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
        }

        bossDisplaySprite.setRegion(currentFrame);
        bossDisplaySprite.draw(batch);
    }

    // 設定 Boss 狀態 (用於動畫切換)
    private void setBossState(CurrentBossState newState) {
        if (this.currentBossState != newState) {
            this.currentBossState = newState;
            stateTime = 0f; // 切換狀態時，動畫時間歸零
        }
    }

    // Boss 的碰撞矩形
    public Rectangle Boss_Rectangle() {
        return bossDisplaySprite.getBoundingRectangle();
    }

    // Boss 受傷方法
    public void takeDamage(float damageAmount) {
        if (!isAlive) {
            return;
        }
        currentHealth -= damageAmount;
        System.out.println("Boss 受到 " + damageAmount + " 點傷害！當前 HP: " + currentHealth);

        if (currentHealth <= 0) {
            currentHealth = 0;
            isAlive = false;
            System.out.println("Boss 已被擊敗！");
            // TODO: 在這裡處理 Boss 死亡的邏輯，例如播放死亡動畫、掉落物品、遊戲勝利等
        }
    }

    // --- HP 和存活狀態獲取方法 ---
    public float getCurrentHealth() {
        return currentHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public boolean isAlive() {
        return isAlive;
    }

    // 釋放資源
    public void dispose() {
        monsterAtlas.dispose();
        // 如果你的 monster.atlas 裡有圖片，monster.atlas.dispose() 會處理它們
        // 如果你像之前那樣額外載入了 laserAttackTexture，記得也要 dispose
        // laserAttackTexture.dispose(); // 如果有
    }

    // --- 位置獲取和設定方法 ---
    public float getX() {
        return bossDisplaySprite.getX(); // 直接從 sprite 獲取位置
    }

    public float getY() {
        return bossDisplaySprite.getY(); // 直接從 sprite 獲取位置
    }

    public Vector2 getCenterPosition() {
        return new Vector2(bossDisplaySprite.getX() + bossDisplaySprite.getWidth() / 2,
            bossDisplaySprite.getY() + bossDisplaySprite.getHeight() / 2);
    }

    public void setX(float x) {
        this.posX = x; // 更新內部坐標 (可選，但保持一致性較好)
        bossDisplaySprite.setX(x);
    }

    public void setY(float y) {
        this.posY = y; // 更新內部坐標
        bossDisplaySprite.setY(y);
    }

    public float getMonsterWidth() {
        return monsterWidth;
    }

    public float getMonsterHeight() {
        return monsterHeight;
    }
}
