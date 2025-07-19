package com.hotaruinori.main;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.hotaruinori.Attack.MissileManager;
import com.hotaruinori.Plays.Character;
import com.hotaruinori.Plays.CharacterMovement;
import com.hotaruinori.Plays.ExpBall;
import com.hotaruinori.Plays.Projectiles;
import com.hotaruinori.main.other.*;
import com.hotaruinori.monstars.BOSS.BossA;

public class Main implements ApplicationListener {
    // 遊戲資源
    private CharacterMovement characterMovement;
    private Music music;
    private InfiniteBackground infiniteBackground;
    private HUD hud;  //新增UI
    //渲染相關
    private SpriteBatch spriteBatch;
    private FitViewport viewport;

    //遊戲物件
    private Character character;
    private Vector2 touchPos;
    private Rectangle characterRectangle;
    private Projectiles rainDrops;
    private BossA boss1;
    //5/31飛彈
    private MissileManager missileManager;

    //暫停選單
    private PauseMenu pauseMenu;
    //結束選單
    private GameOverMenu gameOverMenu;
    //怪物生成器
    private Monster_Generator monsterGenerator;


    @Override
    public void create() {
        //初始化基礎資源
        infiniteBackground = new InfiniteBackground("background2.png");
        // 初始化隨機背景物件，用來隨機產生背景裝飾物的函式，你可以控制中心點與範圍（這邊用 Vector2(0, 0) 為中心，範圍 20x10，代表覆蓋整個地圖的寬與高）。
        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
        hud = new HUD();  //HUD介面
        //初始化渲染系統
        spriteBatch = new SpriteBatch();
        viewport = new FitViewport(8, 5);

        // 初始化角色
        character = new Character();
        character.setBlockingObjects(infiniteBackground.getBlockingObjects());
        //移動操作功能，現在獨立出來了
        characterMovement = new CharacterMovement(character, viewport);

        // 初始化投射物系統，相關參數後續升級系統做好再放入其中，先在main做呼叫
        rainDrops = new Projectiles("drop.png", "drop.mp3");
        rainDrops.setProjectileCount(5);  //設定投射物數量，後續放進升級系統
        rainDrops.setProjectileSpeed(10.0f);  //設定投射物速度，後續放進升級系統
        rainDrops.setSpawnInterval(0.1f);  //設定投射物發射間隔，後續放進升級系統
        rainDrops.setProjectileSize(0.5f);  //設定投射物發射大小，後續放進升級系統
        rainDrops.setProjectileDamage(10.0f);  //設定投射物發射傷害，後續放進升級系統

        //初始化怪物
        boss1 = new BossA();
        boss1.setPlayer(character);  //初始化追蹤位置並將"玩家的位置"傳給怪物的class

        // 其他物件
        touchPos = new Vector2();
        characterRectangle = new Rectangle();

        // 5/31新增飛彈系列  讓 BossA 知道 MissileManager
        missileManager = new MissileManager(); // <--- 初始化 MissileManager
        missileManager.setPlayerCharacter(character); // <--- 將玩家角色傳給 MissileManager
        boss1.setMissileManager(missileManager);
        // ✅ 初始化怪物生成器（每 5 秒生成一次怪物）
        monsterGenerator = new Monster_Generator(character, viewport.getCamera(), 5.0f, missileManager);

        //設置音樂
        music.setLooping(true);
        music.setVolume(.5f);
        music.play();
        //暫停選單與結束選單
        pauseMenu = new PauseMenu();
        gameOverMenu = new GameOverMenu();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.resize(width, height);      // HUD 畫面
    }

    @Override
    public void render() {
        // 優先處理 Game Over 狀態
        if (gameOverMenu.isVisible()) {
            gameOverMenu.render();  // 顯示 Game Over 畫面
            return;                 // 停止 input() 和 logic()，遊戲停止
        }

        // ESC 鍵控制暫停與恢復
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (pauseMenu.isVisible()) {
                pauseMenu.hide();
            } else {
                pauseMenu.show(() -> {
                    pauseMenu.hide(); // Resume
                }, () -> {
                    Gdx.app.exit(); // Exit
                });
            }
        }

        // 遊戲邏輯只在未暫停與未死亡狀態時進行
        if (!pauseMenu.isVisible()) {
            input();
            logic();

            // 加上角色死亡檢查：在 logic() 裡好像也可以
            if (character.getCurrentHealth() <= 0) {
                gameOverMenu.show(character.getTotalExp(), () -> {
                    //restartGame(); // TODO: 尚未實作重啟遊戲功能
                }, () -> {
                    Gdx.app.exit(); // 離開
                });
                return;
            }
        }

        draw(); // 渲染遊戲畫面
        pauseMenu.render(); // 繪製暫停選單（如果開啟）
    }

    private void input() {
        characterMovement.handle(Gdx.graphics.getDeltaTime());
    }

    private void logic() {
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        // 取得自上一幀以來經過的時間，飛彈、怪物生成用
        float deltaTime = Gdx.graphics.getDeltaTime(); // <--- 在這裡取得時間差！
        // 更新投射物
        rainDrops.update(Gdx.graphics.getDeltaTime(), characterRectangle, viewport, character, boss1, monsterGenerator);

        if (boss1.isAlive()) { // <--- 新增判斷
            boss1.getMonsterAI().update(deltaTime); // 更新怪物 AI
        }
        //飛彈
        missileManager.update(deltaTime); // <--- 由 MissileManager 更新所有飛彈
        // ✅ 更新怪物生成器
        monsterGenerator.update(deltaTime);
        // 經驗球
        ExpBall.update(deltaTime, character);
        hud.setExp(character.getCurrentExp(), character.getNextLevelExp(), character.getCurrentLevel());
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        // 更新攝影機位置：讓它跟隨角色
        Vector2 center = character.getCenterPosition();
        viewport.getCamera().position.set(center.x, center.y, 0);
        viewport.getCamera().update();
        // 更新攝影機
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);

        spriteBatch.begin();

        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        infiniteBackground.render(spriteBatch, character.getCenterPosition(), worldWidth, worldHeight);
        // 更新怪物、角色與子彈
        boss1.render(spriteBatch); // 將 batch 傳遞給 BossA 的 render 方法
        character.render(spriteBatch);
        //更新角色座標與地圖物件碰撞判定
        infiniteBackground.generateChunksAround(character.getCenterPosition(), 1); //
        character.setBlockingObjects(infiniteBackground.getBlockingObjects());// 或其他範圍大小
        //更新子彈
        rainDrops.render(spriteBatch);
        // 只有當 Boss 存活時才更新和繪製 Boss 的行為
        if (boss1.isAlive()) { // <--- 新增判斷
            boss1.render(spriteBatch); // 繪製 Boss
        }
        else {
            // TODO: Boss 死亡後的遊戲邏輯，例如顯示遊戲勝利畫面
//            System.out.println("遊戲勝利！");
        }

        //飛彈
        missileManager.render(spriteBatch);    // <--- 由 MissileManager 渲染所有飛彈
        monsterGenerator.render(spriteBatch);
        // 經驗球
        ExpBall.render(spriteBatch);
        spriteBatch.end();
        // 額外使用 ShapeRenderer 畫經驗值條
        //LibGDX 要求你不能在 SpriteBatch.begin() 和 ShapeRenderer.begin() 同時使用，否則會觸發錯誤。
        hud.render(spriteBatch); // <-- 注意一定要在 batch.end() 後呼叫
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        infiniteBackground.dispose();
        music.dispose();
        spriteBatch.dispose();
        character.dispose();
        rainDrops.dispose();
        boss1.dispose();
        pauseMenu.dispose();
        //5/31飛彈
        missileManager.dispose(); // <--- 釋放 MissileManager 的資源
        hud.dispose();
    }
}
