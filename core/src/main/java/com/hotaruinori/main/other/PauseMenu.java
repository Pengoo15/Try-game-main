package com.hotaruinori.main.other;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * 暫停選單類別，負責顯示暫停畫面及提供「繼續遊戲」和「結束遊戲」按鈕。
 * 使用 LibGDX 的 Scene2D UI 框架進行管理與渲染。
 */
public class PauseMenu {
    private Stage stage;                // Scene2D 的舞台，管理所有 UI 元件（Actor）
    private boolean visible = false;    // 控制暫停選單是否顯示的旗標
    private Runnable onResume;          // 點擊「Resume」按鈕時要執行的回呼函式 (lambda 或匿名類別)
    private Runnable onExit;            // 點擊「Exit Game」按鈕時要執行的回呼函式
    private BitmapFont font;            // 自訂字型，使用 FreeTypeFontGenerator 載入 TTF 字型檔並產生
    private Skin skin;                  // 用於儲存並管理 UI 元件的樣式 (LabelStyle, TextButtonStyle 等)
    private Texture bgTex;              // 半透明背景用的貼圖 (Texture 需記得 dispose)

    /**
     * 建構子：初始化暫停選單的 UI 元件，載入字型與樣式，建立並配置按鈕與標題。
     */
    public PauseMenu() {
        // 建立舞台並設定視口為螢幕尺寸的 ScreenViewport，確保 UI 元件大小固定不會隨攝影機縮放變動
        stage = new Stage(new ScreenViewport());

        // 將輸入事件處理權交給 stage，讓按鈕可正常接收點擊事件
        Gdx.input.setInputProcessor(stage);

        // 使用 FreeTypeFontGenerator 從 TTF 字型檔建立 BitmapFont，方便 UI 文字渲染
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/myfont.ttf"));

        // 設定字型生成參數：字型大小與陰影設定
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 64;                             // 字型大小
        parameter.shadowOffsetX = 2;                     // 陰影水平偏移
        parameter.shadowOffsetY = 2;                     // 陰影垂直偏移
        parameter.shadowColor = new Color(0, 0, 0, 1);  // 陰影顏色：不透明黑色

        // 產生 BitmapFont，供 UI 元件使用
        font = generator.generateFont(parameter);

        // 產生完字型後要釋放字型生成器的資源，避免記憶體洩漏
        generator.dispose();

        // 建立 Skin，管理所有 UI 樣式設定
        skin = new Skin();

        // 設定 Label 的樣式（字型與顏色），並加到 Skin 中作為預設樣式
        LabelStyle labelStyle = new LabelStyle();
        labelStyle.font = font;               // 指定字型
        labelStyle.fontColor = Color.WHITE;  // 文字顏色為白色
        skin.add("default", labelStyle);     // 加入 Skin，名稱為 "default"

        // 設定 TextButton 的樣式，使用相同字型與顏色
        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.font = font;              // 使用相同字型
        buttonStyle.fontColor = Color.WHITE; // 白色文字
        skin.add("default", buttonStyle);    // 加入 Skin，名稱為 "default"

        // 使用 Table 排版 UI 元件，方便垂直堆疊與調整間距
        Table table = new Table();
        table.setFillParent(true); // Table 會自動撐滿整個舞台（stage）
        stage.addActor(table);     // 將 Table 加入舞台

        // 使用 Pixmap 製作一個 1x1 黑色半透明貼圖當作背景
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.5f); // RGBA = 黑色 + 50%透明
        pixmap.fill();                  // 填滿 Pixmap

        // 把 Pixmap 轉成 Texture 貼圖，用於 Table 背景
        bgTex = new Texture(pixmap);

        // Pixmap 資源使用完畢後立即釋放，避免記憶體洩漏
        pixmap.dispose();

        // 使用 NinePatchDrawable 包裝 Texture，NinePatch 可用於拉伸或平鋪背景
        Drawable bg = new NinePatchDrawable(new NinePatch(bgTex, 0, 0, 0, 0));

        // 設定 Table 背景為剛剛建立的半透明黑色背景
        table.setBackground(bg);

        // 建立 Label 元件顯示 "Paused" 標題，使用剛設定好的 skin 樣式
        Label pausedLabel = new Label("Paused", skin);

        // 調整字型大小的縮放比例，因為字型本身已經很大，故再放大 1.5 倍以視覺更舒適
        pausedLabel.setFontScale(1.5f);

        // 建立 Resume 按鈕，使用 skin 預設樣式
        TextButton resumeButton = new TextButton("Resume", skin);

        // 給 Resume 按鈕新增點擊事件監聽器
        // run() 是 Runnable 介面裡的唯一方法，代表執行「裡面的程式碼」。
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 按下 Resume 按鈕時，如果 onResume 回呼函式不為 null，執行它
                if (onResume != null) onResume.run();
            }
        });

        // 建立 Exit Game 按鈕
        TextButton exitButton = new TextButton("Exit Game", skin);

        // 給 Exit Game 按鈕新增點擊事件監聽器
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 按下 Exit Game 按鈕時，如果 onExit 回呼函式不為 null，執行它
                if (onExit != null) onExit.run();
            }
        });

        // 將標題與按鈕加入 Table，並設定垂直排列與間距
        table.add(pausedLabel).padBottom(40).row();  // 標題，下方間距 40px，換行
        table.add(resumeButton).pad(40).row();       // Resume 按鈕，四周間距 40px，換行
        table.add(exitButton).pad(40);               // Exit 按鈕，四周間距 40px，不換行
    }

    /**
     * 渲染暫停選單畫面。
     * 呼叫此方法會更新 UI 狀態並繪製所有元件。
     * 若暫停選單不可見則直接跳過。
     */
    public void render() {
        if (!visible) return; // 若未顯示暫停選單，直接結束方法，不執行繪製

        // 啟用 OpenGL 的混合模式，支援透明度
        Gdx.gl.glEnable(GL20.GL_BLEND);

        // 讓 stage 執行邏輯更新，如按鈕狀態、動畫等，deltaTime 為與上一幀時間差（秒）
        stage.act(Gdx.graphics.getDeltaTime());

        // 繪製舞台上的所有 Actor（包含 Label、按鈕等 UI 元件）
        stage.draw();
    }

    /**
     * 顯示暫停選單，並設定點擊 Resume 與 Exit 的行為。
     * @param onResume 按下 Resume 時執行的 Runnable (可以用 lambda 傳入行為)
     * @param onExit 按下 Exit Game 時執行的 Runnable
     */
    public void show(Runnable onResume, Runnable onExit) {
        this.visible = true;      // 將暫停選單設為顯示
        this.onResume = onResume; // 設定 Resume 按鈕的回呼行為
        this.onExit = onExit;     // 設定 Exit 按鈕的回呼行為

        // 暫停選單顯示時將輸入權限交給 stage，讓按鈕可接收輸入事件
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * 隱藏暫停選單，且釋放輸入控制權。
     */
    public void hide() {
        this.visible = false;     // 不顯示暫停選單
        this.onResume = null;     // 清除 Resume 回呼
        this.onExit = null;       // 清除 Exit 回呼

        // 隱藏暫停選單時取消輸入處理（需自行在主遊戲邏輯中設定輸入控制）
        Gdx.input.setInputProcessor(null);
    }

    /**
     * 是否正在顯示暫停選單。
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * 釋放所有使用到的資源，避免記憶體洩漏。
     * Stage、BitmapFont、Skin、Texture 等都會被釋放。
     * 請確保遊戲結束或不再使用暫停選單時呼叫此方法。
     */
    public void dispose() {
        if (stage != null) {
            stage.dispose();   // 釋放 Stage 佔用的資源（包含所有 Actor）
            stage = null;      // 清除舞台參考
        }
        if (font != null) {
            font.dispose();    // 釋放字型資源
            font = null;
        }
        if (skin != null) {
            skin.dispose();    // 釋放 Skin 及其內部資源
            skin = null;
        }
        if (bgTex != null) {
            bgTex.dispose();   // 釋放背景貼圖資源
            bgTex = null;
        }
    }
}
