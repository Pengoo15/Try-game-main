package com.hotaruinori.monstars.other;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public interface IMonster {
    Vector2 getCenterPosition(); // 取得怪物中心點座標
    float getX(); // 取得怪物 X 座標
    float getY(); // 取得怪物 Y 座標
    void setX(float x); // 設定怪物 X 座標
    void setY(float y); // 設定怪物 Y 座標
    void takeDamage(float damageAmount); // 怪物承受傷害
    void playIDLE(); // 播放待機動畫
    void playLaserPrepareAnimation(); // (可選擇，如果小怪沒有雷射，可以空實作)
    void playLaserAttackEffect(Vector2 targetPosition, float damage); // (可選擇)
    void spawnMissile(float speed, float damage); // (可選擇，如果小怪會發射飛彈)
    void spawnSpreadMissiles(int count, float spreadAngle, float speed, float damage); // (可選擇)
    void playChargePrepareAnimation(); // (可選擇)
    void playChargeImpactEffect(); // (可選擇)

    void render(SpriteBatch batch); // 繪製怪物
    boolean isAlive(); // 檢查怪物是否存活
    void dispose(); // 釋放怪物資源
    // 您可以根據需要添加更多方法，例如：
    // Rectangle getBounds(); // 取得碰撞矩形 (很重要，用於碰撞檢測)
    // float getAttackDamage(); // 取得怪物的攻擊傷害
    // float getAttackRange(); // 取得怪物的攻擊範圍 (如果它有非近戰攻擊)
}
