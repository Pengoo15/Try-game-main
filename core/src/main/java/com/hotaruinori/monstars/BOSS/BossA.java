package com.hotaruinori.monstars.BOSS;

import com.hotaruinori.Attack.Missile;
import com.hotaruinori.Attack.MissileManager; // <--- 新增引入
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2; // 確保有這個 import
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;  // 碰撞判定用
import com.hotaruinori.Plays.Character;
import com.badlogic.gdx.math.Intersector;
import com.hotaruinori.monstars.other.MonsterAI;


public class BossA {

    private Sprite bossDisplaySprite;

    private Character character;
    private MonsterAI monsterAI;
    private MissileManager missileManager; // <--- 新增 MissileManager 引用

    private float monsterWidth = 2f; //怪物的寬
    private float monsterHeight = 2f; //怪物的高


    private float posX = 0.5f; //怪物的起始位置
    private float posY = 0.5f; //怪物的起始位置

    private float moveSpeed = 3f; //怪物的移動速度 //
    private float attackDistanceThreshold = 1f; // 怪物停止追蹤的距離，可以設小一點讓它更靠近
    private float attackRange = 10f; // 怪物的攻擊範圍不可小於技能範圍
    private float attackDamage = 10f; //怪物的攻擊傷害


    // --- 雷射屬性 ---
    private boolean isLaserActive = false; // 雷射是否正在發射中
    private Vector2 laserStartPoint = new Vector2(); // 雷射的起點
    private Vector2 laserEndPoint = new Vector2();   // 雷射的終點
    private float laserThickness = 0.5f;             // 雷射的粗細 (世界單位)
    private float laserDuration = 1.0f;              // 雷射持續時間 (秒) 也是發射速度
    private float currentLaserTime = 0f;             // 當前雷射已發射的時間

    private float currentLaserDamage; // <--- 新增：儲存當前雷射攻擊的傷害值
    private boolean laserHitAppliedThisAttack = false; // <--- 新增：追蹤本次雷射攻擊是否已造成傷害

    // <--- 新增雷射光線圖片相關變數
    private Texture laserAttackTexture;
    private TextureRegion laserAttackRegion;
    private float currentLaserRenderLength = 0f; // <--- 新增：當前雷射繪製的長度
    private float maxLaserLength = 5.0f; // 新增：雷射的最大長度 (世界單位)



    //圖片之後新增



    // --- 新增 HP 相關變數 ---
    private float maxHealth;
    private float currentHealth;
    private boolean isAlive; // 怪物是否存活


    // 可以新增一個引用來管理飛彈列表，飛彈由 BossA 產生
    private Array<Missile> activeMissiles;

    // 圖片動畫相關
    private TextureAtlas monsterAtlas;
    private Animation<TextureRegion> monsterWalkAnimation;


    private Animation<TextureRegion> monsterChargeAnimation; // 假設有充電動畫
    private Animation<TextureRegion> monsterLaserPrepareAnimation; // 假設有雷射準備動畫
    private Animation<TextureRegion> monsterIdleAnimation; // 假設有待機動畫
    private Animation<TextureRegion> monsterMISSILE; //發射飛彈

    private float stateTime=0f; // 用於追蹤動畫播放時間
    private CurrentBossState currentBossState; // 用於管理 Boss 當前的動畫狀態


    public enum CurrentBossState {
        IDLE,
        WALK,
        CHARGE_PREPARE,
        CHARGE_IMPACT,
        LASER_PREPARE,
        LASER_ATTACK,
        MISSILE_PREPARE,
        // 可以根據需要添加更多狀態，如 DEATH, HURT 等
    }


    public BossA() {

        this.currentBossState = CurrentBossState.IDLE;


        monsterAtlas = new TextureAtlas(Gdx.files.internal("monsters/monsters/monster.atlas"));


        // <--- 載入雷射攻擊圖片
        try {
            laserAttackTexture = new Texture(Gdx.files.internal("monsters/LL.png"));
            laserAttackRegion = new TextureRegion(laserAttackTexture);
        } catch (Exception e) {
            Gdx.app.error("BossA", "無法載入 'monsters/bossLASERAttack.png' 圖片: " + e.getMessage());
            // 如果圖片載入失敗，可以考慮使用一個預設紋理或只在控制台輸出錯誤
            laserAttackRegion = null;
        }

        // 創建行走動畫
        Array<TextureAtlas.AtlasRegion> walkFrames = monsterAtlas.findRegions("bossWalk2"); // 假設你在 TexturePacker 中命名為 "bossWalk" + 數字
        if (walkFrames.size > 0) {
            monsterWalkAnimation = new Animation<TextureRegion>(0.1f, walkFrames, Animation.PlayMode.LOOP);
        } else {
            Gdx.app.error("BossA", "找不到 'bossWalk' 動畫幀。請檢查 monster.atlas 文件。");
            // 如果找不到動畫，使用一個預設圖片防止錯誤
            monsterWalkAnimation = new Animation<TextureRegion>(0.1f, new TextureRegion(new Texture("monsters/bossWalk2.png")));
        }

        // 創建待機動畫 (假設只有一張圖，或是一個很短的循環)
        Array<TextureAtlas.AtlasRegion> idleFrames = monsterAtlas.findRegions("bossWalk2"); // 假設命名為 "bossIdle" + 數字
        if (idleFrames.size > 0) {
            monsterIdleAnimation = new Animation<TextureRegion>(0.15f, idleFrames, Animation.PlayMode.LOOP);
        } else {
            // 如果沒有待機動畫，可以讓它保持在行走動畫的第一幀
            monsterIdleAnimation = new Animation<TextureRegion>(0.15f, monsterWalkAnimation.getKeyFrame(0));
            Gdx.app.error("BossA", "找不到 'bossWalk1' 動畫幀。");
        }


        // 創建充電動畫 (舉例，如果你的Boss有這個技能)
        Array<TextureAtlas.AtlasRegion> chargeFrames = monsterAtlas.findRegions("bossCharge"); // 假設命名為 "bossCharge" + 數字
        if (chargeFrames.size > 0) {
            monsterChargeAnimation = new Animation<TextureRegion>(0.1f, chargeFrames, Animation.PlayMode.NORMAL); // 播放一次
        } else {
            monsterChargeAnimation = null; // 或者指向一個預設幀
            Gdx.app.error("BossA", "找不到 'bossCharge' 動畫幀。");
        }

        //飛彈
        Array<TextureAtlas.AtlasRegion> MISSILE = monsterAtlas.findRegions("bossMISSILE1"); // 假設命名為 "bossCharge" + 數字
        if (MISSILE.size > 0) {
            monsterMISSILE = new Animation<TextureRegion>(0.1f, MISSILE, Animation.PlayMode.NORMAL); // 播放一次
        } else {
            monsterMISSILE = null; // 或者指向一個預設幀
            Gdx.app.error("BossA", "找不到 'bossCharge' 動畫幀。");
        }


        // 創建雷射準備動畫 (舉例)
        Array<TextureAtlas.AtlasRegion> laserPrepareFrames = monsterAtlas.findRegions("bossLASER"); // 假設命名為 "bossLaserPrepare" + 數字
        if (laserPrepareFrames.size > 0) {
            monsterLaserPrepareAnimation = new Animation<TextureRegion>(0.15f, laserPrepareFrames, Animation.PlayMode.NORMAL);
        } else {
            monsterLaserPrepareAnimation = null;
            Gdx.app.error("BossA", "找不到 'bossLASER' 動畫幀。");
        }

        // 3. 初始化用於繪製的 Sprite
        // 使用一個 Sprite 來管理位置、大小等屬性，它的 TextureRegion 會在 render 中動態更新
        // 初始時使用行走動畫的第一幀
        bossDisplaySprite = new Sprite(monsterWalkAnimation.getKeyFrame(0));
        bossDisplaySprite.setSize(monsterWidth, monsterHeight); // 設定 Boss 在世界單位中的大小
        bossDisplaySprite.setPosition(posX, posY); // 設定初始位置

        this.maxHealth = 1000f; // 設定 Boss 的最大生命值
        this.currentHealth = maxHealth; // 初始生命值等於最大生命值
        this.isAlive = true; // 初始為存活狀態




    }

    public void setPlayer(Character character) {
        this.character = character;
        this.monsterAI = new MonsterAI(this, character,
            moveSpeed,
            attackDistanceThreshold,
            attackRange,
            attackDamage);
    }

    // --- 新增動畫切換方法 ---
    public void setBossState(CurrentBossState newState) {
        if (this.currentBossState != newState) {
            this.currentBossState = newState;
            stateTime = 0f; // 切換狀態時，動畫時間歸零，確保從第一幀開始播放新動畫
        }
    }



    // 新增方法供 MonsterAI 呼叫
    public void playLaserPrepareAnimation() {
        setBossState(CurrentBossState.LASER_PREPARE);
        System.out.println("BossA: 播放雷射準備動畫");
    }

    public void playLaserAttackEffect(Vector2 targetPosition,float damageAmount) {
        // 設定雷射的起點為 Boss 的中心位置
        laserStartPoint.set(getCenterPosition());


        // <--- 限制雷射終點的邏輯 ---
        Vector2 directionToTarget = new Vector2(targetPosition).sub(laserStartPoint);
        if (directionToTarget.len() > maxLaserLength) {
            // 如果目標超出最大射程，則將終點限制在最大射程處
            laserEndPoint.set(laserStartPoint).add(directionToTarget.nor().scl(maxLaserLength));
        } else {
            // 否則，直接設定為目標位置
            laserEndPoint.set(targetPosition);
        }
        // --- 限制雷射終點的邏輯結束 ---

        // 設定雷射的終點為目標位置（通常是玩家的位置）

        isLaserActive = true;       // 啟用雷射
        currentLaserTime = 0f;      // 重置雷射計時器
        currentLaserRenderLength = 0f; // <--- 重置雷射繪製長度
        this.currentLaserDamage = damageAmount; // <--- 儲存傷害值
        this.laserHitAppliedThisAttack = false; // <--- 重置傷害標誌
        System.out.println("BossA: 雷射光束已啟動！");
        //7/14
        setBossState(CurrentBossState.LASER_ATTACK);

        // 你也可以在這裡播放雷射音效
    }

    public void playIDLE() {
        setBossState(CurrentBossState.IDLE); // 可以是一個獨立的攻擊幀，或者回到IDLE/WALK
        System.out.println("BossA: 繪製雷射光束");

    }


//    public void playMISSILE() {
//        setBossState(CurrentBossState.MISSILE_PREPARE); // 可以是一個獨立的攻擊幀，或者回到IDLE/WALK
//        System.out.println("BossA: 繪製雷射光束");
//
//    }

    // <--- 新增這個方法，讓 Maingame 可以設定 MissileManager
    public void setMissileManager(MissileManager manager) {
        this.missileManager = manager;
    }

    // 新增方法供 MonsterAI 呼叫以生成飛彈
    public void spawnMissile(float speed, float damage) {
        if (missileManager == null) {
            System.err.println("Error: MissileManager not set in BossA!");
            return;
        }
        setBossState(CurrentBossState.MISSILE_PREPARE);  //發射飛彈動畫
        missileManager.addMissile(getCenterPosition().x, getCenterPosition().y, speed, damage);
        System.out.println("BossA: 生成飛彈，目標玩家 (透過 MissileManager)");
    }

    // 新增一個方法來生成扇形飛彈
    public void spawnSpreadMissiles(int numberOfMissiles, float spreadAngle, float missileSpeed, float missileDamage) {
        if (missileManager == null) {
            System.err.println("Error: MissileManager not set in BossA!");
            return;
        }
        setBossState(CurrentBossState.MISSILE_PREPARE); //發射飛彈動畫
        missileManager.addSpreadMissiles(
            getCenterPosition().x,
            getCenterPosition().y,
            numberOfMissiles,
            spreadAngle,
            missileSpeed,
            missileDamage
        );
        System.out.println("BossA: 生成 " + numberOfMissiles + " 枚扇形飛彈 (透過 MissileManager)。");
    }


    public void playChargePrepareAnimation() {
        setBossState(CurrentBossState.CHARGE_PREPARE);
        System.out.println("BossA: 播放衝撞準備動畫");
    }

    public void playChargeImpactEffect() {
//        setBossState(CurrentBossState.CHARGE_PREPARE); // 衝撞擊中後的狀態
        System.out.println("BossA: 播放衝撞擊中效果");
        // TODO: 這裡應該觸發衝撞的實際傷害邏輯
    }

    // 新增這個方法，讓遊戲迴圈可以更新怪物的 AI 行為
    public MonsterAI getMonsterAI() {
        return monsterAI;
    }

    public void render(SpriteBatch batch) {

        if (!isAlive) { // Boss 死亡後不繪製
            return;
            // TODO: 可以考慮在這裡繪製 Boss 的血條
        }

        stateTime += Gdx.graphics.getDeltaTime();
        TextureRegion currentFrame = null;
        switch (currentBossState) {
            case WALK:
                currentFrame = monsterWalkAnimation.getKeyFrame(stateTime, true); // true表示循環播放
                break;
            case IDLE:
                currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                break;
            case CHARGE_PREPARE:
                // 不再在這裡自動切換狀態。MonsterAI 會在適當的時候切換。
                if (monsterChargeAnimation != null) {
                    currentFrame = monsterChargeAnimation.getKeyFrame(stateTime, false);
                } else {
                    currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                }
                break;
            case LASER_PREPARE:
                // 同樣，不自動切換狀態。
                if (monsterLaserPrepareAnimation != null) {
                    currentFrame = monsterLaserPrepareAnimation.getKeyFrame(stateTime, false);
                } else {
                    currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                }
                break;


            case MISSILE_PREPARE:
                // 同樣，不自動切換狀態。
                if (monsterMISSILE != null) {
                    currentFrame = monsterMISSILE.getKeyFrame(stateTime, false);
                } else {
                    currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                }
                break;

            //7/14
            case LASER_ATTACK:
                // 在雷射攻擊狀態下，可以繼續顯示雷射準備動畫，或者如果有專屬的雷射攻擊動畫，則使用它
                if (monsterLaserPrepareAnimation != null) { // 這裡沿用準備動畫，您可以替換為專屬的雷射攻擊動畫
                    currentFrame = monsterLaserPrepareAnimation.getKeyFrame(stateTime, false);
                } else {
                    currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                }
                break;


            // TODO: 添加 CHARGE_IMPACT 和 LASER_ATTACK 的處理
            default:
                currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
                break;
        }
        // 確保 currentFrame 不為 null，以防萬一
        if (currentFrame == null) {
            currentFrame = monsterIdleAnimation.getKeyFrame(stateTime, true);
        }


        // 3. 更新 bossDisplaySprite 的紋理區域和位置
        bossDisplaySprite.setRegion(currentFrame);
        // posX 和 posY 應該由 MonsterAI 或遊戲邏輯來更新
        bossDisplaySprite.setPosition(posX, posY);




        bossDisplaySprite.draw(batch); // 只繪製這一個 Sprite


        // --- 更新雷射狀態（時間）---
        if (isLaserActive) {
            currentLaserTime += Gdx.graphics.getDeltaTime();

            // <--- 更新雷射繪製長度
            Vector2 totalLaserDirection = new Vector2(laserEndPoint).sub(laserStartPoint);
            float totalLaserLength = totalLaserDirection.len();
            currentLaserRenderLength = (currentLaserTime / laserDuration) * totalLaserLength;
            // 確保雷射長度不會超過總長度
            if (currentLaserRenderLength > totalLaserLength) {
                currentLaserRenderLength = totalLaserLength;
            }


            // <--- 雷射碰撞偵測和扣血邏輯 ---
            // 只有當雷射活躍且尚未造成傷害時才進行碰撞偵測
            if (!laserHitAppliedThisAttack && character != null) {
                // 獲取玩家的碰撞矩形
                // 假設 Character 類別有 getX(), getY(), getWidth(), getHeight() 方法
                Rectangle characterBounds = new Rectangle(character.getX(), character.getY(), character.getWidth(), character.getHeight());

                // 計算當前雷射光線的實際終點（考慮漸進長度）
                Vector2 currentEffectiveLaserEndPoint = new Vector2(laserStartPoint).add(totalLaserDirection.nor().scl(currentLaserRenderLength));

                // 檢查雷射線段是否與玩家碰撞矩形相交
                if (Intersector.intersectSegmentRectangle(laserStartPoint, currentEffectiveLaserEndPoint, characterBounds)) {
                    character.takeDamage(this.currentLaserDamage); // 對玩家造成傷害
                    System.out.println("玩家受到雷射傷害！" + this.currentLaserDamage + " 點傷害！ (來自雷射碰撞)");
                    laserHitAppliedThisAttack = true; // 標記本次攻擊已造成傷害
                }
            }
            // --- 雷射碰撞偵測和扣血邏輯結束 ---




            if (currentLaserTime >= laserDuration) {
                isLaserActive = false;
                System.out.println("BossA: 雷射光束已停止。");
                setBossState(CurrentBossState.IDLE); // 雷射結束後切換回待機狀態
            } else {
                // <--- 繪製雷射光線 (使用圖片) ---
                if (laserAttackRegion != null) { // 確保圖片已載入
                    Vector2 laserDirection = new Vector2(laserEndPoint).sub(laserStartPoint);
                    float laserAngle = laserDirection.angleDeg(); // 計算雷射的角度（度）

                    // 繪製雷射圖片，使用 currentLaserRenderLength
                    batch.draw(laserAttackRegion,
                        laserStartPoint.x, laserStartPoint.y,
                        0, 0,
                        currentLaserRenderLength, laserThickness, // <--- 使用 currentLaserRenderLength
                        1f, 1f,
                        laserAngle);
                } else {
                    System.err.println("BossA: 雷射攻擊圖片未載入，無法繪製雷射光線。");
                }
            }
        }



        // TODO: 在這裡繪製 Boss 的血條 (如果需要的話)

    }
    // 怪物的碰撞判定用
    public Rectangle BossA_Rectangle() {
        // 直接使用 bossDisplaySprite 的邊界來獲取碰撞矩形，這樣更準確，因為它的位置和大小是最新的
        return bossDisplaySprite.getBoundingRectangle();
    }

    // --- 新增 Boss 受傷方法 ---
    public void takeDamage(float damageAmount) {
        if (!isAlive) { // 如果已經死亡，不再受傷
            return;
        }
        currentHealth -= damageAmount;
        System.out.println("Boss 受到 " + damageAmount + " 點傷害！當前 HP: " + currentHealth);

        if (currentHealth <= 0) {
            currentHealth = 0; // 確保 HP 不會變成負值
            isAlive = false;
            System.out.println("Boss 已被擊敗！");
            // TODO: 播放死亡動畫、掉落物品、遊戲勝利等邏輯
        }
    }
    // --- 新增獲取 HP 和存活狀態的方法 ---
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
        monsterAtlas.dispose();

        if (laserAttackTexture != null) { // <--- 釋放雷射圖片資源
            laserAttackTexture.dispose();
        }




    }

    public float getX() {
        return posX;
    }

    public float getY() {
        return posY;
    }

    // 新增這些方法，供 MonsterAI 讀取和設定 BossA 的位置
    public Vector2 getCenterPosition() {
        // 假設 bossSprite 繪製時的寬高是 2f，這裡要與 render 裡的數值一致
        return new Vector2(bossDisplaySprite.getX() + bossDisplaySprite.getWidth() / 2,
            bossDisplaySprite.getY() + bossDisplaySprite.getHeight() / 2);
    }

    // 設置 Boss 的位置
    public void setX(float x) {
        this.posX = x; // 更新內部坐標
        bossDisplaySprite.setX(x); // 更新 Sprite 坐標
    }

    public void setY(float y) {
        this.posY = y; // 更新內部坐標
        bossDisplaySprite.setY(y); // 更新 Sprite 坐標
    }

    // 提供一個方法讓 AI 更新 Boss 的實際移動
    public void updatePosition(float newX, float newY) {
        setX(newX);
        setY(newY);
    }

    // 可以提供方法讓 AI 知道 Boss 的尺寸
    public float getMonsterWidth() {
        return monsterWidth;
    }

    public float getMonsterHeight() {
        return monsterHeight;
    }

    // 提供獲取雷射資訊的方法，供 Maingame 繪製
    public boolean isLaserActive() {
        return isLaserActive;
    }

    public Vector2 getLaserStartPoint() {
        return laserStartPoint;
    }

    public Vector2 getLaserEndPoint() {
        return laserEndPoint;
    }

    public float getLaserThickness() {
        return laserThickness;
    }

}
