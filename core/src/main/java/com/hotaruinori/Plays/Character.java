package com.hotaruinori.Plays;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.Pixmap; // 繪製血條用

public class Character {
    // 可調整參數
    float MAX_HEALTH = 100.0f;      //最大血量
    float CURRENT_HEALTH = 100.0f;  //當前血量
    float AUTO_HEAL_POINT = 1.0f;     // 秒回
    float MOVE_SPEED = 4.0f;        //移動速度
    float CHARACTER_HEIGHT = 0.5f;        //角色寬度
    float CHARACTER_WIDTH = 0.5f;        //角色高度
    // 角色狀態枚舉
    public enum State {
        STANDING, WALKING
    }
    // 角色面對方向枚舉
    public enum FacingDirection {
        UP, DOWN, LEFT, RIGHT
    }

    // 角色屬性
    private Sprite sprite;
    private State state = State.STANDING;
    private FacingDirection facing = FacingDirection.DOWN;
    private float stateTime = 0;
    private int currentExp = 0; //初始經驗值
    private int totalExp = 0; //總經驗值，結算面板使用
    private int level = 1;             //初始等級
    private int expToNextLevel = 100; // 初始升級所需經驗
    // 血量相關初始
    private float maxHealth = MAX_HEALTH;   // 最大血量
    private float currentHealth = CURRENT_HEALTH;  // 當前血量
    private float autoHealPoint = AUTO_HEAL_POINT; // 每秒回血量
    private float healTimer = 0f;
    // 單像素白色貼圖用於繪製血條
    private static Texture whiteTexture;

    // 阻擋物件陣列（用於碰撞檢查）
    private Rectangle[] blockingObjects = new Rectangle[0];  // 預設為空陣列

    static {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    // 鍵盤移動動畫資源
    private Animation<TextureRegion> walkUpAnimation;
    private Animation<TextureRegion> walkDownAnimation;
    private Animation<TextureRegion> walkLeftAnimation;
    private Animation<TextureRegion> walkRightAnimation;
    private TextureRegion[] standingFrames;

    // 角色中心點
    public Vector2 getCenterPosition() {
        return new Vector2(
            sprite.getX() + sprite.getWidth() / 2f,
            sprite.getY() + sprite.getHeight() / 2f
        );
    }

    // 觸碰 or 滑鼠移動用
    private Vector2 targetPosition = null;
    private float moveSpeed = MOVE_SPEED;

    // 設定阻擋物件陣列（由外部設定）
    public void setBlockingObjects(Rectangle[] blockingObjects) {
        this.blockingObjects = blockingObjects;
    }

    // 斜方向移動用，2025430新增
    public void moveWithDirection(float delta, Vector2 direction, float speed) {
        // 如果正在進行滑鼠移動，則優先處理滑鼠移動
        if (targetPosition != null) return;

        if (direction.len() > 0) {
            direction.nor();
        }
        move(direction.x * speed * delta, direction.y * speed * delta);

        // 修改方向判斷邏輯：斜向移動時優先顯示側面動畫
        if (Math.abs(direction.x) > 0.5f) {  // 當水平分量較大時
            setFacing(direction.x > 0 ? FacingDirection.RIGHT : FacingDirection.LEFT);
        } else if (direction.y != 0) {  // 否則才考慮垂直方向
            setFacing(direction.y > 0 ? FacingDirection.UP : FacingDirection.DOWN);
        }
    }

    // 添加移動到目標位置的方法
    public void moveTo(float x, float y) {
        if (targetPosition == null) {
            targetPosition = new Vector2();
        }
        targetPosition.set(x, y);
    }

    // 更新移動邏輯
    public void updateMovement(float delta) {
        if (targetPosition != null) {
            // 計算移動方向
            Vector2 direction = new Vector2(
                targetPosition.x - sprite.getX() - sprite.getWidth()/2,
                targetPosition.y - sprite.getY() - sprite.getHeight()/2
            );

            // 如果已經到達目標位置
            if (direction.len() < 0.1f) {
                targetPosition = null;
                return;
            }

            // 標準化方向向量並計算移動量
            direction.nor();
            float moveX = direction.x * moveSpeed * delta;
            float moveY = direction.y * moveSpeed * delta;

            // 使用內部阻擋物件檢查碰撞後移動
            move(moveX, moveY);

            // 根據移動方向設置面向方向
            if (Math.abs(direction.x) > Math.abs(direction.y)) {
                setFacing(direction.x > 0 ? FacingDirection.RIGHT : FacingDirection.LEFT);
            } else {
                setFacing(direction.y > 0 ? FacingDirection.UP : FacingDirection.DOWN);
            }
        }
    }

    public Character() {
        initAnimations();
        // 使用站立動畫的第一幀初始化精靈
        sprite = new Sprite(standingFrames[FacingDirection.DOWN.ordinal()]);
        sprite.setSize(CHARACTER_WIDTH, CHARACTER_HEIGHT);
    }

    private void initAnimations() {
        // 加載站立幀紋理
        standingFrames = new TextureRegion[4];
        standingFrames[FacingDirection.UP.ordinal()] = new TextureRegion(new Texture("character_img/dora_walk_back1.png"));
        standingFrames[FacingDirection.DOWN.ordinal()] = new TextureRegion(new Texture("character_img/dora_walk1.png"));
        standingFrames[FacingDirection.LEFT.ordinal()] = new TextureRegion(new Texture("character_img/dora_walk_left1.png"));
        standingFrames[FacingDirection.RIGHT.ordinal()] = new TextureRegion(new Texture("character_img/dora_walk_right1.png"));

        // 初始化走路動畫
        walkUpAnimation = new Animation<>(0.15f,
            new TextureRegion(new Texture("character_img/dora_walk_back1.png")),
            new TextureRegion(new Texture("character_img/dora_walk_back2.png")),
            new TextureRegion(new Texture("character_img/dora_walk_back3.png")),
            new TextureRegion(new Texture("character_img/dora_walk_back4.png"))
        );
        walkDownAnimation = new Animation<>(0.15f,
            new TextureRegion(new Texture("character_img/dora_walk1.png")),
            new TextureRegion(new Texture("character_img/dora_walk2.png")),
            new TextureRegion(new Texture("character_img/dora_walk3.png")),
            new TextureRegion(new Texture("character_img/dora_walk4.png"))
        );
        walkLeftAnimation = new Animation<>(0.15f,
            new TextureRegion(new Texture("character_img/dora_walk_left1.png")),
            new TextureRegion(new Texture("character_img/dora_walk_left2.png")),
            new TextureRegion(new Texture("character_img/dora_walk_left3.png")),
            new TextureRegion(new Texture("character_img/dora_walk_left4.png"))
        );
        walkRightAnimation = new Animation<>(0.15f,
            new TextureRegion(new Texture("character_img/dora_walk_right1.png")),
            new TextureRegion(new Texture("character_img/dora_walk_right2.png")),
            new TextureRegion(new Texture("character_img/dora_walk_right3.png")),
            new TextureRegion(new Texture("character_img/dora_walk_right4.png"))
        );
    }

    // 修改現有的 update 方法
    public void update(float delta, boolean isMoving) {
        if (isMoving) {
            state = State.WALKING;
            stateTime += delta;
            // 立即更新動畫幀（移除緩衝）
            sprite.setRegion(getCurrentAnimationFrame());
        } else {
            state = State.STANDING;
            stateTime = 0;
            sprite.setRegion(standingFrames[facing.ordinal()]);
        }
        //每秒回血
        updateHealing(delta);
    }

    private TextureRegion getCurrentAnimationFrame() {
        switch (facing) {
            case UP: return walkUpAnimation.getKeyFrame(stateTime, true);
            case DOWN: return walkDownAnimation.getKeyFrame(stateTime, true);
            case LEFT: return walkLeftAnimation.getKeyFrame(stateTime, true);
            case RIGHT: return walkRightAnimation.getKeyFrame(stateTime, true);
            default: return standingFrames[facing.ordinal()];
        }
    }

    // 修改：移動方法，加入碰撞檢查（使用 blockingObjects 陣列）
    public void move(float deltaX, float deltaY) {
        float newX = sprite.getX() + deltaX;
        float newY = sprite.getY() + deltaY;

        // 預測移動後角色的新位置矩形
        Rectangle futureBounds = new Rectangle(newX, newY, sprite.getWidth(), sprite.getHeight());

        for (Rectangle objectBounds : blockingObjects) {
            if (futureBounds.overlaps(objectBounds)) {
                return; // 有碰撞就不移動
            }
        }

        sprite.translateX(deltaX);
        sprite.translateY(deltaY);
    }

    // 設置面向方向
    public void setFacing(FacingDirection direction) {
        this.facing = direction;
    }

    // 獲取精靈用於繪製
    public Sprite getSprite() {
        return sprite;
    }

    // 獲取位置和大小用於碰撞檢測
    public float getX() {
        return sprite.getX();
    }

    public float getY() {
        return sprite.getY();
    }

    public float getWidth() {
        return sprite.getWidth();
    }

    public float getHeight() {
        return sprite.getHeight();
    }
    //圓形碰撞區域，目前是給經驗值球用
    public Circle getBounds() {
        return new Circle(getX(), getY(), 1f); // 根據需要調整
    }
    //經驗值計算
    public void addExp(int value) {
        this.currentExp += value;
        this.totalExp += value;
        System.out.println("Gained " + value + " EXP. Total: " + currentExp);
        checkLevelUp();
    }
    public int getCurrentExp() {
        return currentExp;
    }
    public int getTotalExp() {
        return totalExp;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public int getNextLevelExp() {
        return expToNextLevel;
    }
    public int getCurrentLevel() {
        return level;
    }
    private void checkLevelUp() {
        while (currentExp >= expToNextLevel) {
            currentExp -= expToNextLevel;
            level++;
            expToNextLevel = calculateNextLevelExp();
            // 這裡可以加上升級特效或音效
            System.out.println("Level up! 現在等級：" + level);
        }
    }

    private int calculateNextLevelExp() {
        // 升級所需經驗成長曲線（可以自定義）
        return 100 + level * 20;
    }

    public int getLevel() {
        return level;
    }

    public void takeDamage(float damage) {
        currentHealth -= damage;
        if (currentHealth < 0) currentHealth = 0;
    }
    // 回血處理邏輯
    private void updateHealing(float delta) {
        healTimer += delta;
        if (healTimer >= 1.0f) {
            healTimer -= 1.0f;
            heal(autoHealPoint); // 每秒呼叫一次 heal()
        }
    }

    public void heal(float amount) {
        currentHealth += amount;
        if (currentHealth > maxHealth) currentHealth = maxHealth;
    }

    //加入死亡判斷
    public boolean isDead() {
        return currentHealth <= 0;
    }

    public void render(SpriteBatch batch) {
        sprite.draw(batch);
        drawHealthBar(batch);
    }

    private void drawHealthBar(SpriteBatch batch) {
        float barWidth = sprite.getWidth();                // 血條寬度 = 角色寬度
        float barHeight = 0.05f;                           // 血條高度 = 一個小值，適合顯示為細長條
        float x = sprite.getX();                           // 血條的起始 X 位置（跟角色左對齊）
        float y = sprite.getY() - barHeight - 0.02f;       // 血條 Y 位置（在角色下方一點點）

        float healthPercent = (float) currentHealth / maxHealth;   // 生命百分比（0~1）

        // 背景條：灰色血條背景（表示滿血範圍）
        batch.setColor(0.3f, 0.3f, 0.3f, 1f);    // 設成灰色
        batch.draw(whiteTexture, x, y, barWidth, barHeight);

        // 前景條：根據血量百分比決定顏色
        if (healthPercent <= 0.3f) {
            batch.setColor(1f, 0f, 0f, 1f);  // 紅色（低血）
        } else if (healthPercent <= 0.7f) {
            batch.setColor(1f, 1f, 0f, 1f);  // 黃色（中血）
        } else {
            batch.setColor(0f, 1f, 0f, 1f);  // 綠色（高血）
        }

        // 畫前景條（血量）
        batch.draw(whiteTexture, x, y, barWidth * healthPercent, barHeight);

        // 還原顏色（避免影響後續繪圖）
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        // 釋放所有靜態圖片資源
        for (TextureRegion frame : standingFrames) {
            if (frame != null && frame.getTexture() != null) {
                frame.getTexture().dispose(); // 釋放紋理
            }
        }
        // 釋放動畫資源
        disposeAnimation(walkUpAnimation);
        disposeAnimation(walkDownAnimation);
        disposeAnimation(walkLeftAnimation);
        disposeAnimation(walkRightAnimation);
    }

    private void disposeAnimation(Animation<TextureRegion> animation) {
        if (animation != null) {
            for (TextureRegion frame : animation.getKeyFrames()) {
                if (frame != null && frame.getTexture() != null) {
                    frame.getTexture().dispose();
                }
            }
        }
    }
}
