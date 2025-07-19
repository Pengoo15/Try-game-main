package com.hotaruinori.monstars.other;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.hotaruinori.Plays.Character;
import com.hotaruinori.monstars.BOSS.BossA;

public class MonsterAI {

    private BossA monster;
    private Character character;


    // 攻擊變數
    // 移除多餘的 attackdistance 變數

    private float timeInCooldownState = 0f; // 進入 COOLDOWN 狀態後經過的時間

    //範圍
    private float attackRange; // 怪物可以攻擊的距離範圍
    private float LASER_Range=4f; //可以放雷射的範圍
    private float MISSILE_Range=4f; //可以放飛彈的範圍
    private float Charge_Range=3f; //可以衝撞的範圍
    private float Charge_Bang=3f; //衝撞的爆炸範圍
    //之後會新增
    private float MISSILE_BIG_Range=600f;

    // 為不同攻擊模式設定獨立的冷卻時間
    private float genericAttackCooldownDuration = 1f; // 所有攻擊後共用的短暫冷卻時間
    private float laserCooldown = 5.0f; // 雷射攻擊冷卻時間 (範例值)
    private float missileCooldown = 5.0f; // 飛彈攻擊冷卻時間 (範例值)
    private float chargeCooldown = 5.0f; // 衝撞攻擊冷卻時間 (範例值)


    private float attackDamage; // 怪物造成的傷害值
    // 為不同攻擊模式設定獨立的計時器
    private float timeSinceLastLaserAttack;
    private float timeSinceLastMissileAttack;
    private float timeSinceLastChargeAttack;

    // 雷射和衝撞可能需要額外的準備時間或持續時間
    private float laserPreparationTime = 1f; // 雷射準備時間
    private float laserDuration = 0.5f;       // 雷射持續時間
    private float chargePreparationTime = 1f; // 衝撞準備時間
    private float chargeDuration = 2f;      // 衝撞移動持續時間 (或衝撞距離)
    private Vector2 chargeTargetPosition;     // 衝撞的目標位置
    //傷害係數
    private float laserDamageCoefficient = 2f;
    private float chargeDamageCoefficient = 3f;
    private float missileDamageCoefficient = 1.5f;
    //飛彈細項
    private int numberOfMissiles = 1; // 發射 3 枚飛彈
    private float spreadAngle = 45f;  // 總扇形角度為 45 度 (從最左到最右)
    private float missileSpeed = 3.0f;
    // 追蹤和攻擊參數
    private float moveSpeed;
    private float attackDistanceThreshold; // 當距離小於此值時停止追蹤，避免抖動

    // BossA 狀態
    public enum AttackType {
        LASER_ATTACK,
        MISSILE_ATTACK,
        CHARGE_ATTACK
    }

    public enum BossState {
        CHASING, // 追蹤玩家
        ATTACK_DECIDING, // 新增一個狀態，用於決定使用哪種攻擊

        ATTACKING_LASER,
        ATTACKING_MISSILE,
        ATTACKING_CHARGE,
        COOLDOWN // 統一的冷卻狀態，或為每種攻擊設置獨立冷卻狀態
    }
    private BossState currentState;
    private boolean laserEffectTriggered = false;








    public MonsterAI(BossA monster, Character character, float moveSpeed, float attackDistanceThreshold,
                     float attackRange, float attackDamage) {
        this.monster = monster;
        this.character = character;
        this.moveSpeed = moveSpeed;
        this.attackDistanceThreshold = attackDistanceThreshold;

        this.attackRange = attackRange;
        this.attackDamage = attackDamage;

        // 初始化所有冷卻計時器，使其在遊戲開始時可以立即使用
        this.timeSinceLastLaserAttack = laserCooldown;
        this.timeSinceLastMissileAttack = missileCooldown;
        this.timeSinceLastChargeAttack = chargeCooldown;

        this.currentState = BossState.CHASING; // 初始狀態為追蹤
    }

    /**
     * 更新怪物的行為邏輯
     * @param deltaTime 遊戲幀時間
     */
    public void update(float deltaTime) {
        // 更新所有冷卻計時器
        timeSinceLastLaserAttack += deltaTime;
        timeSinceLastMissileAttack += deltaTime;
        timeSinceLastChargeAttack += deltaTime;

        if (character == null) {
            return;
        }

        Vector2 monsterCenter = monster.getCenterPosition();
        Vector2 targetCenter = character.getCenterPosition();

        float deltaX = targetCenter.x - monsterCenter.x;
        float deltaY = targetCenter.y - monsterCenter.y;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        switch (currentState) {
            case CHASING:
                if (distance > attackDistanceThreshold) {
                    float normalizedDirectionX = deltaX / distance;
                    float normalizedDirectionY = deltaY / distance;

                    float velocityX = normalizedDirectionX * moveSpeed * deltaTime;
                    float velocityY = normalizedDirectionY * moveSpeed * deltaTime;

                    monster.setX(monster.getX() + velocityX);
                    monster.setY(monster.getY() + velocityY);
                }

                // 判斷是否進入攻擊範圍，並準備進入攻擊決策狀態
                // 這裡可以根據距離決定優先使用哪種攻擊
                if (distance <= attackRange) { // 稍微擴大檢測範圍，以便提前決策遠程攻擊
                    currentState = BossState.ATTACK_DECIDING;
                }
                break;

            case ATTACK_DECIDING:
                // 收集所有目前可用的攻擊選項
                Array<AttackType> availableAttacks = new Array<>();

                // 根據距離和冷卻時間判斷哪些攻擊可用
                // 雷射 (遠程)
                if (distance <= LASER_Range && timeSinceLastLaserAttack >= laserCooldown) {
                    availableAttacks.add(AttackType.LASER_ATTACK);
                }

                // 飛彈 (中遠程)
                if (distance <= MISSILE_Range && timeSinceLastMissileAttack >= missileCooldown) {
                    availableAttacks.add(AttackType.MISSILE_ATTACK);
                }
                // 衝撞 (中近程)
                if (distance <= Charge_Range && timeSinceLastChargeAttack >= chargeCooldown) {
                    availableAttacks.add(AttackType.CHARGE_ATTACK);
                }


                // 如果有可用攻擊，則從中隨機選擇一個
                if (availableAttacks.size > 0) {
                    AttackType chosenAttack = availableAttacks.random(); // 隨機選擇一個
                    switch (chosenAttack) {
                        case LASER_ATTACK:
                            currentState = BossState.ATTACKING_LASER;
                            timeSinceLastLaserAttack = 0;
                            laserEffectTriggered = false;
                            break;
                        case MISSILE_ATTACK:
                            currentState = BossState.ATTACKING_MISSILE;
                            timeSinceLastMissileAttack = 0;
                            break;
                        case CHARGE_ATTACK:
                            currentState = BossState.ATTACKING_CHARGE;
                            chargeTargetPosition = new Vector2(targetCenter.x, targetCenter.y);
                            timeSinceLastChargeAttack = 0;
                            break;

                    }
                } else {
                    // 如果沒有任何攻擊可用，則回到追蹤狀態
                    currentState = BossState.CHASING;
                }
                break;



            case ATTACKING_LASER:
                performLaserAttack(deltaTime);
                if (timeSinceLastLaserAttack >= laserPreparationTime + laserDuration) {
                    currentState = BossState.COOLDOWN;
                    timeInCooldownState = 0; // <--- 重置冷卻計時器
                }
                break;

            case ATTACKING_MISSILE:
                performMissileAttack();
                currentState = BossState.COOLDOWN;
                timeInCooldownState = 0; // <--- 重置冷卻計時器
                break;

            case ATTACKING_CHARGE:
                performChargeAttack(deltaTime);
                Vector2 currentMonsterPos = monster.getCenterPosition();
                if ((timeSinceLastChargeAttack - chargePreparationTime) >= chargeDuration) {
                    performChargeImpactDamage();
                    currentState = BossState.COOLDOWN;
                    timeInCooldownState = 0; // <--- 重置冷卻計時器
                }
                break;

            case COOLDOWN:
                timeInCooldownState += deltaTime; // <--- 更新冷卻計時器
                // 只有當「通用冷卻時間」結束時才切換回追蹤
                if (timeInCooldownState >= genericAttackCooldownDuration) {
                    currentState = BossState.CHASING;
                }
                // 注意：這裡不再檢查單個攻擊的冷卻，那些冷卻會獨立計時，只用於 ATTACK_DECIDING
                break;
        }
    }

    /**
     * 執行攻擊動作
     */
    /**
     * 執行雷射攻擊
     * @param deltaTime 遊戲幀時間
     */
    private void performLaserAttack(float deltaTime) {
        // 雷射攻擊通常有準備時間和持續時間
        if (timeSinceLastLaserAttack < laserPreparationTime) {
            monster.playLaserPrepareAnimation(); //準備發射雷射動畫
            // 準備階段：播放準備動畫，可能顯示雷射瞄準線
            System.out.println("Boss 正在準備雷射攻擊...");
            laserEffectTriggered = false;
            // TODO: 在 BossA 中繪製雷射瞄準線或預警效果

        } else if (timeSinceLastLaserAttack < laserPreparationTime + laserDuration) {

            if (!laserEffectTriggered) { // <--- 確保只在雷射攻擊開始時觸發一次效果
                monster.playLaserAttackEffect(character.getCenterPosition(), attackDamage * laserDamageCoefficient); // <--- 呼叫 BossA 的雷射啟動方法
                laserEffectTriggered = true;
            }
            monster.playLaserPrepareAnimation();
            // 攻擊階段：發射雷射，造成傷害
            System.out.println("Boss 發射雷射！");
            // TODO: 在 BossA 中繪製雷射光束效果
            // 傷害邏輯：雷射可能對路徑上的所有玩家造成傷害，或者對玩家持續造成傷害
            // 這裡簡化為對玩家造成一次傷害 (可以在一個特定時刻造成，或持續造成)
            // 假設每幀都造成傷害，需要根據deltaTime調整傷害量
            // character.takeDamage(attackDamage * deltaTime / laserDuration); // 範例持續傷害
            // 為了簡化，可以只在雷射開始時造成一次傷害


        } else {
            // 雷射攻擊結束
            monster.playIDLE();
            System.out.println("雷射攻擊結束。");
            // 可以切換狀態或由 update 方法處理切換
        }
    }

    /**
     * 執行飛彈攻擊
     */
//    private void performMissileAttackBIG() {
//        if (character != null) {
//            System.out.println("Boss 發射飛彈！");
//            // 呼叫 BossA 的方法來生成飛彈
//            // 這裡可以傳入飛彈的獨立速度和傷害值，或者使用 Boss 的預設值
//                monster.spawnMissile(300f, attackDamage * 1.5f); // 範例飛彈速度和傷害
//
//
//            // 觸發飛彈發射動畫/音效 (可以放在 BossA 的 spawnMissile 裡)
//        }
//    }
    private void performMissileAttack() {
        if (character != null) {
            System.out.println("Boss 發射飛彈！");
            // 設定扇形發射的參數
            int numberOfMissiles = this.numberOfMissiles; // 發射 多枚飛彈
            float spreadAngle = this.spreadAngle;  // 總扇形角度(從最左到最右)
            float missileSpeed = this.missileSpeed; //飛彈飛行速度
            float missileDamage = attackDamage * missileDamageCoefficient ;

            // 呼叫 BossA 的方法來生成多枚飛彈
            monster.spawnSpreadMissiles(numberOfMissiles, spreadAngle, missileSpeed, missileDamage);

            // 觸發飛彈發射動畫/音效
        }
    }

    /**
     * 執行衝撞攻擊 (移動階段)
     * @param deltaTime 遊戲幀時間
     */
    private void performChargeAttack(float deltaTime) {
        if (timeSinceLastChargeAttack < chargePreparationTime) {
            monster.playChargePrepareAnimation();  //播放衝撞準備動畫
            // 衝撞準備階段：怪物可能停頓，發出怒吼或準備動畫
            System.out.println("Boss 正在準備衝撞！");
            // TODO: 在 BossA 中播放準備動畫
        } else {
            monster.playChargePrepareAnimation();  //播放衝撞準備動畫
            // 衝撞移動階段：怪物朝目標位置快速移動
            System.out.println("Boss 正在衝撞！");
            Vector2 monsterCenter = monster.getCenterPosition();
            // 計算衝撞方向
            Vector2 direction = new Vector2(chargeTargetPosition).sub(monsterCenter).nor();
            // 移動怪物
            monster.setX(monster.getX() + direction.x * moveSpeed * 3 * deltaTime); // 衝撞速度可以更快
            monster.setY(monster.getY() + direction.y * moveSpeed * 3 * deltaTime);
            // TODO: 在衝撞路徑上檢測碰撞並造成傷害
            // 這個複雜度較高，可以簡單處理為：到達目標點後造成傷害
        }
    }

    /**
     * 衝撞結束時造成傷害 (或在衝撞路徑上檢測碰撞造成傷害)
     */
    private void performChargeImpactDamage() {
        if (character != null) {

            // 在衝撞結束時，檢查玩家是否在終點附近，如果是，造成傷害
            monster.playChargePrepareAnimation();  //播放衝撞準備動畫
            Vector2 monsterCenter = monster.getCenterPosition();
            float impactRange =  Charge_Bang; // 衝撞的爆炸範圍
            if (monsterCenter.dst(character.getCenterPosition()) < impactRange) {
//                monster.playChargeImpactEffect();  // 播放衝撞擊中效果

                System.out.println("Boss 衝撞擊中玩家，造成 " + attackDamage * 3 + " 點傷害！");
                character.takeDamage(attackDamage * chargeDamageCoefficient); // 衝撞傷害最高
                monster.playIDLE();
            } else {
                monster.playChargePrepareAnimation();  //播放衝撞準備動畫
                monster.playIDLE();
                System.out.println("Boss 衝撞未擊中玩家。");
            }
            // 觸發衝撞結束動畫/音效
        }
    }
}



