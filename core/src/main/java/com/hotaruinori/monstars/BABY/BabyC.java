package com.hotaruinori.monstars.BABY;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.hotaruinori.Plays.Character;


public class BabyC {

    private Sprite bossDisplaySprite;
    private Character character; // 玩家角色引用

    private float monsterWidth = 2f;    // 怪物寬度
    private float monsterHeight = 2f;   // 怪物高度

    private float posX = 1f;          // 怪物起始X位置
    private float posY = 1.5f;          // 怪物起始Y位置

    private float moveSpeed = 0.2f;       // 怪物移動速度
    // private float collisionDamage = 5f; // 碰撞傷害值 (現在改為自爆傷害)

    // --- 碰撞傷害冷卻相關 (現在主要用於自爆後的消失) ---
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
    private Animation<TextureRegion> explosionAnimation; // <--- 新增：爆炸動畫

    private float stateTime = 0f; // 用於追蹤動畫播放時間
    private CurrentBossState currentBossState; // 用於管理 Boss 當前的動畫狀態

    // --- 自爆相關變數 ---
    private float selfDestructDamage = 50f; // <--- 新增：自爆造成的傷害 (可調整)
    private float explosionDuration = 1f; // <--- 新增：爆炸動畫持續時間 (秒)
    private float explosionTimer = 0f;      // <--- 新增：自爆計時器
    private boolean hasExplodedDamageDealt = false; // <--- 新增：確保自爆傷害只造成一次

    // 精簡後的 Boss 狀態，加入 EXPLODING
    public enum CurrentBossState {
        IDLE,
        WALK,
        EXPLODING // <--- 新增：自爆狀態
    }

    public BabyC() {
        this.currentBossState = CurrentBossState.IDLE; // 初始狀態為待機

        // 載入紋理圖集
        // 請確保你的 baby.atlas 文件存在於正確路徑
        monsterAtlas = new TextureAtlas(Gdx.files.internal("monsters/monsters/babyC.atlas"));

        // 創建行走動畫
        // 假設你的圖集裡有命名為 "baby_walk_1", "baby_walk_2", ... 的幀
        Array<TextureAtlas.AtlasRegion> walkFrames = monsterAtlas.findRegions("big"); // 假設 'baby' 是走路動畫的前綴
        if (walkFrames.size > 0) {
            monsterWalkAnimation = new Animation<TextureRegion>(0.1f, walkFrames, Animation.PlayMode.LOOP);
        } else {
            Gdx.app.error("BabyC", "錯誤：找不到 'baby' 走路動畫幀。請檢查 baby.atlas 文件。");
            // 提供一個預設紋理，以防萬一
            TextureRegion defaultRegion = monsterAtlas.findRegion("big"); // 嘗試找一個預設單幀圖片
            if (defaultRegion == null) {
                defaultRegion = new TextureRegion(new Texture(Gdx.files.internal("monsters/big.png"))); // 最後手段
            }
            monsterWalkAnimation = new Animation<TextureRegion>(0.1f, defaultRegion);
        }

        // 創建待機動畫 (如果沒有專屬待機，可以沿用行走動畫的第一幀)
        // 假設你的圖集裡有命名為 "baby_idle_1", "baby_idle_2", ... 的幀
        Array<TextureAtlas.AtlasRegion> idleFrames = monsterAtlas.findRegions("big"); // 假設 'baby' 也作為待機動畫的前綴
        if (idleFrames.size > 0) {
            monsterIdleAnimation = new Animation<TextureRegion>(0.15f, idleFrames, Animation.PlayMode.LOOP);
        } else {
            Gdx.app.error("BabyC", "錯誤：找不到 'baby' 待機動畫幀。將使用走路動畫的第一幀作為待機。");
            monsterIdleAnimation = new Animation<TextureRegion>(0.15f, monsterWalkAnimation.getKeyFrame(0));
        }

        // <--- 載入爆炸動畫 ---
        // 假設你的圖集裡有命名為 "explosion_1", "explosion_2", ... 的幀
        Array<TextureAtlas.AtlasRegion> explodeFrames = monsterAtlas.findRegions("Bang"); // <--- 請根據你的實際圖片命名修改這裡
        if (explodeFrames.size > 0) {
            explosionAnimation = new Animation<TextureRegion>(0.05f, explodeFrames, Animation.PlayMode.NORMAL); // 爆炸動畫通常播放一次
        } else {
            Gdx.app.error("BabyC", "警告：找不到 'explosion' 動畫幀。請檢查 baby.atlas 文件。自爆時將顯示預設圖片。");
            // 如果沒有爆炸動畫，可以顯示一個預設的圖片或讓它直接消失
            TextureRegion defaultExplosionRegion = new TextureRegion(new Texture(Gdx.files.internal("monsters/Bang.png"))); // <--- 提供一個爆炸佔位符圖片
            explosionAnimation = new Animation<TextureRegion>(0.1f, defaultExplosionRegion);
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

        // 如果正在自爆，則只處理自爆計時和動畫
        if (currentBossState == CurrentBossState.EXPLODING) {
            explosionTimer += deltaTime;
            if (explosionTimer >= explosionDuration) {
                isAlive = false; // 爆炸結束，怪物死亡
                System.out.println("小怪自爆結束，已消失！");
            }
            return; // 自爆中不執行其他行為
        }

        // 正常追蹤和碰撞邏輯
        timeSinceLastCollisionDamage += deltaTime;

        Vector2 bossCenter = getCenterPosition();
        Vector2 playerCenter = character.getCenterPosition();

        float distance = bossCenter.dst(playerCenter); // 計算 Boss 和玩家之間的距離

        // 追蹤邏輯
        // 當距離大於 Boss 和玩家半徑和的0.8倍時才追蹤，避免過於靠近
        // 並且只有在非自爆狀態下才追蹤
        if (distance > (monsterWidth / 2 + character.getWidth() / 2) * 0.8f) {
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

        // <--- 碰撞觸發自爆邏輯 ---
        // 檢查 Boss 的碰撞矩形是否與玩家的碰撞矩形重疊
        // 並且只有在非自爆狀態下才觸發自爆
        if (Boss_Rectangle().overlaps(character.getCharacterRectangle()) && !hasExplodedDamageDealt) {
            // 觸發自爆
            setBossState(CurrentBossState.EXPLODING); // 切換到自爆狀態
            explosionTimer = 0f; // 重置自爆計時器

            // 對玩家造成自爆傷害
            character.takeDamage(selfDestructDamage);
            System.out.println("小怪自爆！對玩家造成 " + selfDestructDamage + " 點傷害！玩家當前 HP: " + character.getCurrentHealth());
            hasExplodedDamageDealt = true; // 標記為已造成傷害，防止重複傷害
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
                currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                break;
            case EXPLODING: // <--- 繪製爆炸動畫
                currentFrame = explosionAnimation.getKeyFrame(explosionTimer, false); // 爆炸動畫通常不循環
                break;
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

    // Boss 受傷方法 (如果玩家可以攻擊小怪的話)
    public void takeDamage(float damageAmount) {
        if (!isAlive || currentBossState == CurrentBossState.EXPLODING) { // 自爆中或已死亡不重複受傷
            return;
        }
        currentHealth -= damageAmount;
        System.out.println("小怪受到 " + damageAmount + " 點傷害！當前 HP: " + currentHealth);

        if (currentHealth <= 0) {
            currentHealth = 0;
            // 小怪被擊敗時也觸發自爆 (或者直接消失，看遊戲設計)
            // 這裡選擇直接觸發自爆，如果不需要自爆動畫，可以直接 isAlive = false;
            if (currentBossState != CurrentBossState.EXPLODING) {
                setBossState(CurrentBossState.EXPLODING);
                explosionTimer = 0f;
                // 注意：這裡如果被擊敗就自爆，可能需要決定是否再對玩家造成一次傷害
                // 目前的邏輯是只有碰撞才會造成自爆傷害，被擊敗只是觸發自爆動畫
            }
            System.out.println("小怪已被擊敗！");
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
        if (monsterAtlas != null) {
            monsterAtlas.dispose();
        }
        // 如果 explosion_placeholder.png 是單獨載入的，也需要 dispose
        // if (explosionPlaceholderTexture != null) {
        //     explosionPlaceholderTexture.dispose();
        // }
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
