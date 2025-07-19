package com.hotaruinori.Plays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.hotaruinori.main.other.Monster_Generator;
import com.hotaruinori.monstars.BOSS.BossA;

public class Projectiles {
    // 可調整參數，圖片與聲音要到 main 去調整
    float PROJECTILE_WIDTH = 0.5f;  // 投射物寬度
    float PROJECTILE_HEIGHT = 0.5f; // 投射物高度
    float SPAWN_INTERVAL = 0.5f;    // 發射間隔
    float PROJECTILE_SPEED = 4.0f;  // 投射物飛行速度
    int PROJECTILE_COUNT = 1;        // 每次發射的投射物數量（可自由調整）
    float PROJECTILE_Damage = 1;    // 投射物傷害
    //宣告物件
    private Texture projectileTexture;  //儲存投射物使用的圖片材質（Texture 是圖片素材的基本單位）
    private Sound hitSound;             //發射播放的音效
    private Array<ProjectileInstance> projectiles; // 用來儲存目前場上所有的投射物實體，每個包含 Sprite 與速度向量
    private float spawnTimer;           //記錄時間累加，用來決定何時產生下一個投射物。
    private Rectangle projectileRectangle; //暫存矩形，用於未來可能進行的碰撞偵測
    private float spawnInterval;           // 發射間隔
    private float projectileSpeed;        // 投射物飛行速度
    private int projectileCount;          // 每次發射的投射物數量
    private float projectiledamage;

    // 用來記錄每個投射物的 Sprite 和移動速度向量
    private static class ProjectileInstance {
        Sprite sprite;       // 投射物本體(Sprite)
        Vector2 velocity;    // 速度向量（單位：距離/秒）（Vector2）

        ProjectileInstance(Sprite sprite, Vector2 velocity) {
            this.sprite = sprite;
            this.velocity = velocity;
        }
    }
    // 提供給main.java呼叫的投射物create的方法，呼叫要有圖片與聲音路徑，功能請參照上方宣告物件。
    public Projectiles(String texturePath, String soundPath) {
        this.projectileTexture = new Texture(texturePath);
        this.hitSound = Gdx.audio.newSound(Gdx.files.internal(soundPath));
        this.projectiles = new Array<>();
        this.projectileRectangle = new Rectangle();
        this.spawnInterval = SPAWN_INTERVAL;
        this.projectileSpeed = PROJECTILE_SPEED;
        this.spawnTimer = 0;
        this.projectileCount = PROJECTILE_COUNT;
    }

    public void update(float delta, Rectangle characterRect, Viewport viewport, Character character, BossA boss1, Monster_Generator monsterGenerator) {
        // 更新每一個投射物的位置與判斷是否離開畫面
        for (int i = projectiles.size - 1; i >= 0; i--) {
            ProjectileInstance instance = projectiles.get(i);
            Sprite projectile = instance.sprite;
            Vector2 velocity = instance.velocity;

            // 以 velocity 代表速度方向向量，乘以 delta 更新位置（速度是距離/秒，所以要乘上 delta 才是每幀位移）
            // delta：每幀的時間差（秒），用來確保移動速度不受 FPS 影響
            projectile.translate(velocity.x * delta, velocity.y * delta);

            // 更新暫存矩形用於碰撞判定或其他用途（這邊雖然沒用到）
            projectileRectangle.set(projectile.getX(), projectile.getY(), projectile.getWidth(), projectile.getHeight());
            // 檢查是否打中任何怪物
            boolean hit = false;
            // 1. 判斷是否打到 Boss
            if (projectileRectangle.overlaps(boss1.BossA_Rectangle())) {
                boss1.takeDamage(projectiledamage); // 造成傷害
                hit = true;
            }
            // 2. 判斷是否打到怪物陣列中的任一隻
            Array<BossA> monsters = monsterGenerator.getMonsters();
            if (!hit) {
                for (BossA boss : monsters) {
                    if (boss != null && boss.isAlive() && projectileRectangle.overlaps(boss.BossA_Rectangle())) {
                        boss.takeDamage(projectiledamage);
                        hit = true;
                        break; // 只打中一隻怪物就停止
                    }
                }
            }
            // 若投射物擊中或超出視野範圍，則移除
            if (hit||isOutOfView(projectile, viewport)) {
                projectiles.removeIndex(i);
            }
        }

        // 自動產生投射物，每隔一定時間發射
        spawnTimer += delta;
        if (spawnTimer > spawnInterval) {
            spawnTimer = 0;

            // 取得角色中心座標
            Vector2 characterCenter = character.getCenterPosition();

            // 取得滑鼠位置並轉為世界座標
            Vector2 screenMousePos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
            Vector2 worldMousePos = viewport.unproject(screenMousePos);

            // 發射新的投射物
            spawnProjectile(characterCenter, worldMousePos);
        }
    }

    public void render(SpriteBatch batch) {
        // 渲染所有投射物
        for (ProjectileInstance instance : projectiles) {
            instance.sprite.draw(batch);
        }
    }

    // 負責生成新的投射物
    public void spawnProjectile(Vector2 characterCenter, Vector2 targetWorldPos) {
        float width = PROJECTILE_WIDTH;
        float height = PROJECTILE_HEIGHT;

        // 計算主方向向量（滑鼠朝向）
        Vector2 baseDirection = new Vector2(targetWorldPos).sub(characterCenter).nor();

        // 多發投射物時，讓每顆彈偏轉一定角度
        float angleStep = 15f; // 每顆彈偏轉的角度（度數）
        float startAngle = -(angleStep * (projectileCount - 1) / 2f); // 從負角度往右偏

        for (int i = 0; i < projectileCount; i++) {
            // 將主方向旋轉一定角度產生新方向
            Vector2 rotatedDir = new Vector2(baseDirection).rotateDeg(startAngle + i * angleStep);
            Vector2 velocity = new Vector2(rotatedDir).scl(projectileSpeed); // 單位速度向量 x 飛行速度

            Sprite projectile = new Sprite(projectileTexture);
            projectile.setSize(width, height);
            projectile.setOriginCenter();
            projectile.setPosition(characterCenter.x - width / 2, characterCenter.y - height / 2);

            // 計算旋轉角度，讓圖片朝向飛行方向（+90 為了修正圖片方向）
            float angleDeg = rotatedDir.angleDeg() + 90;
            projectile.setRotation(angleDeg);

            // 正確使用建構子初始化實體（包含 sprite 與速度）
            ProjectileInstance instance = new ProjectileInstance(projectile, velocity);

            // 加入到投射物陣列中
            projectiles.add(instance);
        }

        // 播放音效（一次發射只播放一次聲音）
        hitSound.play();
    }
    //用於給main設定投射物數量用。如：projectiles.setProjectileCount(5);
    public void setProjectileCount(int count) {
        this.projectileCount = Math.max(PROJECTILE_COUNT, count); // 最少一發(預設值)
    }
    //用於給main設定投射物數量用。如：projectiles.setProjectileSpeed(10.0f);
    public void setProjectileSpeed(float speed) {
        this.projectileSpeed = Math.max(PROJECTILE_SPEED, speed); // 最慢4.0(預設值)
    }
    //用於給main設定投射物數量用。如：projectiles.setSpawnInterval(0.2f);
    public void setSpawnInterval(float spawn) {
        this.spawnInterval = Math.min(SPAWN_INTERVAL, spawn); // 最多0.5(預設值)
    }
    //用於給main設定投射物範圍大小用。如：projectiles.setSpawnInterval(0.2f);
    // 設定投射物的寬度與高度，兩者同時變更（不得小於 0.1f）
    public void setProjectileSize(float size) {
        float clampedSize = Math.max(0.5f, size); // 避免比預設 0.5 小或負數
        this.PROJECTILE_WIDTH = clampedSize;
        this.PROJECTILE_HEIGHT = clampedSize;
    }
    //用於給main設定投射物傷害用。如：projectiles.setSpawnInterval(0.2f);
    public void setProjectileDamage(float damage) {
        this.projectiledamage = Math.min(PROJECTILE_Damage, damage); // 最多0.5(預設值)
    }

    // 檢查投射物是否超出攝影機視野邊界
    private boolean isOutOfView(Sprite sprite, Viewport viewport) {
        float camX = viewport.getCamera().position.x;
        float camY = viewport.getCamera().position.y;
        float camWidth = viewport.getWorldWidth();
        float camHeight = viewport.getWorldHeight();

        float left = camX - camWidth / 2;
        float right = camX + camWidth / 2;
        float bottom = camY - camHeight / 2;
        float top = camY + camHeight / 2;

        return (sprite.getX() + sprite.getWidth() < left ||
            sprite.getX() > right ||
            sprite.getY() + sprite.getHeight() < bottom ||
            sprite.getY() > top);
    }

    public void dispose() {
        projectileTexture.dispose();
        hitSound.dispose();
    }
    // 讓其他類別（例如 main.java 或 Monster_Generator）能夠取得目前畫面上所有的投射物實體陣列
    public Array<ProjectileInstance> getProjectiles() {
        return projectiles;
    }
}
