package com.hotaruinori;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * 近戰武器：名刀電光丸
 * 不需要渲染投射物，只判定近距離區域攻擊與渲染刀光範圍。
 */
//public class Weapon_Lightning_Sword extends Weapon_Base {
//
//    private float attackCooldown = 1.0f; // 攻擊間隔秒數
//    private float timer = 0;             // 時間累加器
//    private float range = 1.0f;          // 劍的攻擊範圍半徑
//
//    public Weapon_Lightning_Sword(String iconPath) {
//        this.attackDamage = 2.0f; // 劍傷害較高
//    }
//
//    @Override
//    public void update(float delta, Rectangle characterRect, Viewport viewport, Character character, BossA boss1, Monster_Generator monsterGenerator) {
//        timer += delta;
//        if (timer >= attackCooldown) {
//            timer = 0;
//
//            Vector2 center = character.getCenterPosition();
//            Rectangle attackRange = new Rectangle(center.x - range / 2, center.y - range / 2, range, range);
//
//            // 攻擊 Boss
//            if (attackRange.overlaps(boss1.BossA_Rectangle())) {
//                boss1.takeDamage(attackDamage);
//            }
//
//            // 攻擊怪物
//            for (BossA boss : monsterGenerator.getMonsters()) {
//                if (boss != null && boss.isAlive() && attackRange.overlaps(boss.BossA_Rectangle())) {
//                    boss.takeDamage(attackDamage);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void dispose() {
//        super.dispose(); // 圖示釋放由 Weapon 父類處理
//    }
//}
