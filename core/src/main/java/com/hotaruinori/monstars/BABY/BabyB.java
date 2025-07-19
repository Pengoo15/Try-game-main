package com.hotaruinori.monstars.BABY;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;
import com.hotaruinori.Plays.Character;

public class BabyB {

    private Sprite bossDisplaySprite;
    private Character character;

    private float monsterWidth = 1f;    // 怪物寬度
    private float monsterHeight = 1f;   // 怪物高度

    private float posX = 0.8f;          // 怪物起始X位置
    private float posY = 0.8f;          // 怪物起始Y位置

    private float moveSpeed = 2f;       // 怪物移動速度
    private float collisionDamage = 5f; // 碰撞傷害值 (可以調整)

    // --- 碰撞傷害冷卻相關 ---
    private float collisionCooldownDuration = 1f; // 碰撞傷害的冷卻時間 (秒)
    private float timeSinceLastCollisionDamage = 0f; // 距離上次造成碰撞傷害的時間


    private float accelerationTimer = 0f;  //從0開始計時
    private float accelerationInterval = 1.0f;  //每X秒加速一次
    private float accelerationAmount = 0.2f;  //每次加多少速度
    private float maxSpeed = 11.0f;  //最終速度



    private float maxHealth;
    private float currentHealth;
    private boolean isAlive;

    private TextureAtlas monsterAtlas;
    private Animation<TextureRegion> monsterWalkAnimation;
    private Animation<TextureRegion> monsterIdleAnimation;

    private float stateTime = 0f;
    private CurrentBossState currentBossState;

    public enum CurrentBossState {
        IDLE,
        WALK
    }

    public BabyB() {
        this.currentBossState = CurrentBossState.IDLE;

        monsterAtlas = new TextureAtlas(Gdx.files.internal("monsters/monsters/babyB.atlas"));

        // 創建走路動畫
        // 假設您的圖集裡有命名為 "run_frame_1", "run_frame_2", ... 的幀
        Array<TextureAtlas.AtlasRegion> walkFrames = monsterAtlas.findRegions("run_frame"); // <--- 修改這裡！使用正確的前綴
        if (walkFrames.size > 0) {
            monsterWalkAnimation = new Animation<TextureRegion>(0.15f, walkFrames, Animation.PlayMode.LOOP); // 調整動畫速度
        } else {
            Gdx.app.error("BabyB", "錯誤：找不到 'run_frame' 動畫幀。請檢查 babyB.atlas 文件和圖片命名。");
            // 如果找不到走路動畫，嘗試使用單個靜態圖片作為替代，並發出警告
            TextureRegion fallbackRegion = monsterAtlas.findRegion("mo1"); // <--- 假設 mo1.png 在 atlas 中被命名為 "mo1"
            if (fallbackRegion == null) {
                fallbackRegion = new TextureRegion(new Texture(Gdx.files.internal("monsters/run1.png")));
                Gdx.app.error("BabyB", "錯誤：找不到 'mo1' 區域，已使用硬編碼的 fallback 圖片。");
            }
            monsterWalkAnimation = new Animation<TextureRegion>(0.15f, fallbackRegion);
        }


        // 創建待機動畫
        // 假設您的圖集裡有命名為 "idle_frame_1", "idle_frame_2", ... 的幀
        Array<TextureAtlas.AtlasRegion> idleFrames = monsterAtlas.findRegions("idle_frame"); // <--- 修改這裡！使用正確的前綴
        if (idleFrames.size > 0) {
            monsterIdleAnimation = new Animation<TextureRegion>(0.2f, idleFrames, Animation.PlayMode.LOOP);
        } else {
            Gdx.app.error("BabyB", "錯誤：找不到 'idle_frame' 動畫幀。將使用走路動畫的第一幀作為待機。");
            // 如果沒有專屬待機動畫，使用走路動畫的第一幀 (或第一個可用的幀)
            monsterIdleAnimation = new Animation<TextureRegion>(0.2f, monsterWalkAnimation.getKeyFrame(0));
        }


        bossDisplaySprite = new Sprite(monsterIdleAnimation.getKeyFrame(0));
        bossDisplaySprite.setSize(monsterWidth, monsterHeight);
        bossDisplaySprite.setPosition(posX, posY);

        this.maxHealth = 100f;
        this.currentHealth = maxHealth;
        this.isAlive = true;
    }

    public void setPlayer(Character character) {
        this.character = character;
    }

    public void update(float deltaTime) {
        if (!isAlive || character == null) {
            return;
        }

        stateTime += deltaTime;
        timeSinceLastCollisionDamage += deltaTime;
        accelerationTimer += deltaTime;

        if (accelerationTimer >= accelerationInterval) {
            moveSpeed += accelerationAmount;
            if (moveSpeed > maxSpeed) {
                moveSpeed = maxSpeed;
            }
            accelerationTimer = 0f;
            System.out.println("小怪加速了！目前速度: " + moveSpeed);
        }

        Vector2 bossCenter = getCenterPosition();
        Vector2 playerCenter = character.getCenterPosition();

        float distance = bossCenter.dst(playerCenter);

        if (distance > (monsterWidth / 2 + character.getWidth() / 2) * 0.8f && distance > 0.05f) {
            Vector2 direction = new Vector2(playerCenter).sub(bossCenter).nor();
            float velocityX = direction.x * moveSpeed * deltaTime;
            float velocityY = direction.y * moveSpeed * deltaTime;

            if (velocityX < 0) {
                if (!bossDisplaySprite.isFlipX()) {
                    bossDisplaySprite.flip(true, false);
                }
            } else if (velocityX > 0) {
                if (bossDisplaySprite.isFlipX()) {
                    bossDisplaySprite.flip(true, false);
                }
            }
            setX(getX() + velocityX);
            setY(getY() + velocityY);

            setBossState(CurrentBossState.WALK);
        } else {
            setBossState(CurrentBossState.IDLE);
        }

        if (Boss_Rectangle().overlaps(character.getCharacterRectangle())) {
            if (timeSinceLastCollisionDamage >= collisionCooldownDuration) {
                character.takeDamage(collisionDamage);
                System.out.println("小怪碰撞到玩家，造成 " + collisionDamage + " 點傷害！玩家目前 HP: " + character.getCurrentHealth());
                timeSinceLastCollisionDamage = 0;
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (!isAlive) {
            return;
        }

        TextureRegion currentFrame = null;
        switch (currentBossState) {
            case WALK:
                currentFrame = monsterWalkAnimation.getKeyFrame(stateTime, true);
                break;
            case IDLE:
            default:
                currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                break;
        }

        if (currentFrame == null) {
            currentFrame = monsterIdleAnimation.getKeyFrame(0, true);
        }

        bossDisplaySprite.setRegion(currentFrame);
        bossDisplaySprite.draw(batch);
    }

    private void setBossState(CurrentBossState newState) {
        if (this.currentBossState != newState) {
            this.currentBossState = newState;
            stateTime = 0f;
        }
    }

    public Rectangle Boss_Rectangle() {
        return bossDisplaySprite.getBoundingRectangle();
    }

    public void takeDamage(float damageAmount) {
        if (!isAlive) {
            return;
        }
        currentHealth -= damageAmount;
        System.out.println("小怪受到 " + damageAmount + " 點傷害！目前 HP: " + currentHealth);

        if (currentHealth <= 0) {
            currentHealth = 0;
            isAlive = false;
            System.out.println("小怪已被擊敗！");
        }
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void dispose() {
        if (monsterAtlas != null) {
            monsterAtlas.dispose();
        }
    }

    public float getX() {
        return bossDisplaySprite.getX();
    }

    public float getY() {
        return bossDisplaySprite.getY();
    }

    public Vector2 getCenterPosition() {
        return new Vector2(bossDisplaySprite.getX() + bossDisplaySprite.getWidth() / 2,
            bossDisplaySprite.getY() + bossDisplaySprite.getHeight() / 2);
    }

    public void setX(float x) {
        this.posX = x;
        bossDisplaySprite.setX(x);
    }

    public void setY(float y) {
        this.posY = y;
        bossDisplaySprite.setY(y);
    }

    public float getMonsterWidth() {
        return monsterWidth;
    }

    public float getMonsterHeight() {
        return monsterHeight;
    }
}
