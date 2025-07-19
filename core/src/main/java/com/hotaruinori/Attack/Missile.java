package com.hotaruinori.Attack;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.hotaruinori.Plays.Character;


public class Missile {
    private Sprite missileSprite;
    private Vector2 position;
    private Vector2 velocity;
    private float speed;
    private float damage;
    private boolean active;
    private float lifeTime = 3.0f;  //飛彈存活時間
    private float timeElapsed = 0f;
    private float MissileWidth = 0.4f;  //飛彈圖片大小
    private float MissileHeight = 0.4f; //飛彈圖片大小

    private Character targetCharacter;
    private boolean isTrackingMissile; // 標誌來判斷是否是追蹤型飛彈
    private String M_image = "logo.png";  //飛彈圖片

    // 建構子 for 直線飛彈或預設追蹤飛彈 (統一圖片)
    public Missile(float startX, float startY, Character target, float speed, float damage) {
        this.missileSprite = new Sprite(new Texture(M_image)); //
        this.position = new Vector2(startX, startY);
        this.targetCharacter = target;
        this.speed = speed;
        this.damage = damage;
        this.active = true;
        this.isTrackingMissile = true; // 預設為追蹤型，如果希望是直射型，則設為 false

        if (target != null) {
            this.velocity = new Vector2(target.getCenterPosition()).sub(position).nor().scl(speed);
        } else {
            this.velocity = new Vector2(0, 0);
        }

        missileSprite.setSize( MissileWidth, MissileHeight);
        missileSprite.setOriginCenter();
        missileSprite.setRotation(velocity.angleDeg() - 90);
    }

    // 建構子 for 角度發射的飛彈 (扇形發射後追蹤)
    public Missile(float startX, float startY, Character target, float speed, float damage, float initialAngle) {
        this.missileSprite = new Sprite(new Texture(M_image)); // 統一圖片路徑
        this.position = new Vector2(startX, startY);
        this.targetCharacter = target;
        this.speed = speed;
        this.damage = damage;
        this.active = true;
        this.isTrackingMissile = true; // **關鍵修改：將扇形飛彈也設為追蹤型**

        // 根據角度計算初始速度向量
        this.velocity = new Vector2().setLength(speed).setAngleDeg(initialAngle);

        missileSprite.setSize( MissileWidth, MissileHeight);
        missileSprite.setOriginCenter();
        missileSprite.setRotation(velocity.angleDeg() - 90);
    }

    public void update(float deltaTime, Character character) {
        if (!active) {
            return;
        }

        timeElapsed += deltaTime;
        if (timeElapsed >= lifeTime) {
            active = false;
            return;
        }

        // 根據 isTrackingMissile 決定是否追蹤玩家
        if (isTrackingMissile && targetCharacter != null) {
            Vector2 currentTargetPos = targetCharacter.getCenterPosition();
            Vector2 directionToTarget = new Vector2(currentTargetPos).sub(position).nor();


            velocity.set(directionToTarget).scl(speed);


            // 或者，如果你想要發射後有一小段直線，然後再開始追蹤，可以這樣做：
            float trackingDelay = 0.5f; // 飛彈飛行 0.5 秒後才開始追蹤
            float turnSpeed = 0.2f; // 轉向速度，0-1 之間，越小轉向越慢
            if (timeElapsed > trackingDelay) {
                Vector2 desiredVelocity = directionToTarget.scl(speed);
                velocity.lerp(desiredVelocity, turnSpeed); // 平滑轉向
            }
            // 否則，保持初始速度（不需要額外寫 else 塊，因為 velocity 已經在建構子中設定了）
        }

        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;

        missileSprite.setPosition(position.x - missileSprite.getWidth() / 2, position.y - missileSprite.getHeight() / 2);
        missileSprite.setRotation(velocity.angleDeg() - 90);

        // 判斷飛彈是否碰撞到角色
        float missileRadius = missileSprite.getWidth() / 2f; // 圖片是正方形，寬=高
        float characterRadius = character.getSprite().getWidth() / 2f; // 假設角色也用 Sprite

        float distance = position.dst(character.getCenterPosition());

        if (distance < missileRadius + characterRadius) {
            character.takeDamage(damage);
            System.out.println("飛彈擊中玩家，造成 " + damage + " 點傷害！");
            active = false;
        }

    }

    public void render(SpriteBatch batch) {
        if (active) {
            missileSprite.draw(batch);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void dispose() {
        missileSprite.getTexture().dispose();
    }
}
