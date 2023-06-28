package ic.mp.project_jol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.IOException;

public class CheckActivity extends Activity {

    CheckBox openFan, openWind, openWindow;

    int select;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_insetting);

        settingLayout();

        //데이터 가져오기
        Intent intent = getIntent();
        int data = intent.getIntExtra("data",0);
        Log.d("귀가받아오는값 테스트", "값은 : "+ data);
        switch (data) {
            case 1 :
                break;
            case 2 :
                openWindow.setChecked(true);
                break;
            case 3 :
                openWind.setChecked(true);
                break;
            case 4 :
                openFan.setChecked(true);
                break;
            case 5 :
                openWind.setChecked(true);
                openWindow.setChecked(true);
                break;
            case 6 :
                openFan.setChecked(true);
                openWindow.setChecked(true);
                break;
            case 7 :
                openWind.setChecked(true);
                openFan.setChecked(true);
                break;
            case 8 :
                openFan.setChecked(true);
                openWind.setChecked(true);
                openWindow.setChecked(true);
                break;
            default:
                break;
        }

    }

    //확인 버튼 클릭
    public void mOnClose(View v){

        //선택 상태 값 전달
        if (openFan.isChecked() && openWind.isChecked() && openWindow.isChecked()) {
            select = 8;
        }else if (openFan.isChecked() && openWind.isChecked()) {
            select = 7;
        }else if (openFan.isChecked() && openWindow.isChecked()) {
            select = 6;
        }else if (openWind.isChecked() && openWindow.isChecked()) {
            select = 5;
        }else if (openFan.isChecked()) {
            select = 4;
        }else if (openWind.isChecked()) {
            select = 3;
        }else if (openWindow.isChecked()) {
            select = 2;
        }else {
            select = 1;
        }

        //데이터 전달하기
        Intent intent = new Intent();
        intent.putExtra("result", select);
        setResult(RESULT_OK, intent);
        Log.d("신호", "체크신호값 : " + select);

        //액티비티(팝업) 닫기
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //바깥레이어 클릭시 안닫히게
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //안드로이드 백버튼 막기
        return;
    }

    private void settingLayout() {
        openFan = findViewById(R.id.openFan);
        openWind = findViewById(R.id.openWind);
        openWindow = findViewById(R.id.openWindow);
    }
}

