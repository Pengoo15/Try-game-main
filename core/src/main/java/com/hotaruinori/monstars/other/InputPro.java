package com.hotaruinori.monstars.other;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
/*
 **
 **
 **  控制輸入的class
 **
 **
 */

public class InputPro implements InputProcessor {

    float X_SPD =0f; //不動
    float Y_SPD =0f; //不動

    private boolean moveRight = false;
    private boolean moveLeft = false;
    private boolean moveUp = false;
    private boolean moveDown = false;

    @Override
    public boolean keyDown(int keycode) {  //按下時

        if (keycode == Input.Keys.D) {  //按鍵 可修改 現在D=右 A=左 W=上 S=下
            moveRight = true;
        }
        if (keycode == Input.Keys.A) {
            moveLeft = true;
        }
        if (keycode == Input.Keys.W) {
            moveUp = true;
        }
        if (keycode == Input.Keys.S) {
            moveDown = true;
        }



        return true;
    }

    @Override
    public boolean keyUp(int keycode) {  //放開時

        if (keycode == Input.Keys.D) {  //按鍵 可修改 現在D=右 A=左 W=上 S=下
            moveRight = false;
        }
        if (keycode == Input.Keys.A) {
            moveLeft = false;
        }
        if (keycode == Input.Keys.W) {
            moveUp = false;
        }
        if (keycode == Input.Keys.S) {
            moveDown = false;
        }



        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
    public void updateSpeed(float ALLSPD) { //更新移動的方法  使用時傳回的參數就是移動速度
        this.X_SPD = 0f; //只能是0
        this.Y_SPD = 0f; //只能是0


        if (moveRight) {
            X_SPD = ALLSPD;
        }
        if (moveLeft) {
            X_SPD = -ALLSPD;
        }
        if (moveUp) {
            Y_SPD = ALLSPD;
        }
        if (moveDown) {
            Y_SPD = -ALLSPD;
        }
    }

}
