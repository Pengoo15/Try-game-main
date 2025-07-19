package com.hotaruinori.main.other;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * GameOverMenu 類別負責顯示遊戲結束畫面，包括 Game Over 標題、得分、重新開始與離開按鈕。
 * 與 PauseMenu 結構類似，使用 Scene2D UI 架構管理。
 */
public class GameOverMenu {
    private Stage stage;                // 管理 UI 元件的舞台
    private boolean visible = false;    // 是否顯示 Game Over 畫面
    private BitmapFont font;            // 自訂字型
    private Skin skin;                  // UI 樣式設定
    private Texture bgTex;              // 半透明背景貼圖
    private Label scoreLabel;           // 顯示分數的 Label
    private Runnable onRestart;         // 重新開始時執行的行為
    private Runnable onExit;            // 離開遊戲時執行的行為

    /**
     * 建構子：初始化 UI 與按鈕配置
     */
    public GameOverMenu() {
        stage = new Stage(new ScreenViewport());

        // 預設 UI 事件處理給 Stage（可在 show() 時再設）
        Gdx.input.setInputProcessor(stage);

        // 建立字型
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/myfont.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 64;
        parameter.shadowOffsetX = 2;
        parameter.shadowOffsetY = 2;
        parameter.shadowColor = new Color(0, 0, 0, 1);
        font = generator.generateFont(parameter);
        generator.dispose();

        skin = new Skin();

        LabelStyle labelStyle = new LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        skin.add("default", buttonStyle);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // 建立半透明黑色背景
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.6f);
        pixmap.fill();
        bgTex = new Texture(pixmap);
        pixmap.dispose();

        Drawable bg = new NinePatchDrawable(new NinePatch(bgTex, 0, 0, 0, 0));
        table.setBackground(bg);

        Label titleLabel = new Label("Game Over", skin);
        titleLabel.setFontScale(1.8f);

        scoreLabel = new Label("Score: 0", skin);
        scoreLabel.setFontScale(1.2f);

        TextButton restartButton = new TextButton("Restart", skin);
        restartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onRestart != null) onRestart.run();
            }
        });

        TextButton exitButton = new TextButton("Exit Game", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onExit != null) onExit.run();
            }
        });

        table.add(titleLabel).padBottom(40).row();
        table.add(scoreLabel).padBottom(30).row();
        table.add(restartButton).pad(20).row();
        table.add(exitButton).pad(20);
    }

    /**
     * 顯示 Game Over 畫面
     * @param score 玩家得分
     * @param onRestart 點擊 Restart 時的行為
     * @param onExit 點擊 Exit Game 時的行為
     */
    public void show(int score, Runnable onRestart, Runnable onExit) {
        this.visible = true;
        this.onRestart = onRestart;
        this.onExit = onExit;
        scoreLabel.setText("Score: " + score);
        Gdx.input.setInputProcessor(stage);
    }

    /** 隱藏 Game Over 畫面 */
    public void hide() {
        this.visible = false;
        this.onRestart = null;
        this.onExit = null;
        Gdx.input.setInputProcessor(null);
    }

    /** 回傳是否正在顯示 Game Over */
    public boolean isVisible() {
        return visible;
    }

    /** 渲染 Game Over 畫面 */
    public void render() {
        if (!visible) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    /** 釋放資源 */
    public void dispose() {
        if (stage != null) stage.dispose();
        if (font != null) font.dispose();
        if (skin != null) skin.dispose();
        if (bgTex != null) bgTex.dispose();
    }
}
