package com.hotaruinori.Plays;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class ExpBall {
    // 讀取整張經驗球的圖片（假設圖片檔名是Exp_Ball.png且放在assets根目錄）
    private static Texture fullTexture = new Texture("Exp_Ball.png");

    // 從整張圖片中切割出兩種球的子圖片區域
    // TextureRegion(圖片, 起始X, 起始Y, 寬, 高)
    private static TextureRegion smallExpRegion = new TextureRegion(fullTexture, 0, 0, 49, 49);
    private static TextureRegion largeExpRegion = new TextureRegion(fullTexture, 49, 0, 49, 49);

    // 存放所有目前出現的經驗球物件（活躍中的）
    private static ArrayList<ExpBall> activeBalls = new ArrayList<>();

    // 經驗球在世界的座標位置 (Vector2有x,y兩個float欄位)
    private Vector2 position;

    // 這顆經驗球給的經驗值
    private int value;

    // 這顆經驗球使用的圖片區域（小球或大球）
    private TextureRegion region;

    // 用圓形區域判斷是否和玩家重疊（碰撞判定）
    private Circle bounds;

    // 拾取範圍半徑，這是世界單位，不是像素
    private static final float PICKUP_RADIUS = 0.5f;

    // 這個球的「大小」— 代表在世界座標中的寬和高，單位是世界單位 (viewport 單位)
    private float size;

    /**
     * 建構子
     * @param x 經驗球的世界X座標
     * @param y 經驗球的世界Y座標
     * @param value 經驗值
     * @param region 使用哪個子圖
     * @param size 這顆球在世界座標的大小（邊長）
     */
    public ExpBall(float x, float y, int value, TextureRegion region, float size) {
        this.position = new Vector2(x, y);
        this.value = value;
        this.region = region;
        this.size = size;
        this.bounds = new Circle(x, y, PICKUP_RADIUS);
    }

    /**
     * 根據機率隨機產生經驗球，並加入活躍列表
     * @param x 生成位置X
     * @param y 生成位置Y
     */
    public static void spawn(float x, float y) {
        Random random = new Random();
        float roll = random.nextFloat();
        System.out.println("掉球了");

        // 10%機率生成大球，大球給20經驗，大小0.25世界單位
        if (roll < 0.1f) {
            activeBalls.add(new ExpBall(x, y, 50, largeExpRegion, 0.25f));
        }
        // 70%機率生成小球，小球給5經驗，大小0.2世界單位
        else if (roll < 0.8f) {
            activeBalls.add(new ExpBall(x, y, 20, smallExpRegion, 0.2f));
        }
        // 其他情況不生成球
    }

    /**
     * 更新邏輯：檢查是否和玩家碰撞，若碰撞就增加經驗並移除經驗球
     * @param deltaTime 每幀時間秒數 (通常用來計算動畫或移動，這裡暫時沒用)
     * @param player 主角物件
     */
    public static void update(float deltaTime, Character player) {
        Iterator<ExpBall> iterator = activeBalls.iterator();
        while (iterator.hasNext()) {
            ExpBall ball = iterator.next();
            // 使用圓形範圍判斷是否和玩家碰撞
            if (ball.bounds.overlaps(player.getBounds())) {
                // 撞到就把經驗值加給玩家
                player.addExp(ball.value);
                // 移除已被拾取的球
                iterator.remove();
            }
        }
    }

    /**
     * 繪製所有活躍的經驗球
     * @param batch 用來畫圖的SpriteBatch，繪製時需要先呼叫 batch.begin()，繪製後呼叫 batch.end()
     */
    public static void render(SpriteBatch batch) {
        for (ExpBall ball : activeBalls) {
            // 計算繪製起點，使圖片能以球心為中心繪製
            // ball.position 是球中心點位置
            // drawX, drawY 是左下角繪製點
            float drawX = ball.position.x - ball.size / 2f;
            float drawY = ball.position.y - ball.size / 2f;

            // batch.draw(region, x, y, width, height)
            // 寬高是世界單位，控制圖片縮放大小
            batch.draw(ball.region, drawX, drawY, ball.size, ball.size);
        }
    }
}
