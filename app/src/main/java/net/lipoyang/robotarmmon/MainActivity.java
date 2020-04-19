package net.lipoyang.robotarmmon;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    // デバッグ用
    private static final String TAG = "MainActivity";
    private static final boolean DEBUGGING = true;

    // Views
    private TextView textLocalAddr1;
    private TextView textLocalAddr2;
    private EditText editRemoteAddr;
    private Button buttonUpdate;
    private TextView textStatus;
    private TextView[] textCtrls;
    private TextView[] textServos;

    /************************************************************
     * ライフサイクルイベント
     ************************************************************/

    // 画面生成時
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(DEBUGGING) Log.e(TAG, "++ ON CREATE ++");

        // Views
        textLocalAddr1 = (TextView)findViewById(R.id.textLocalAddr1);
        textLocalAddr2 = (TextView)findViewById(R.id.textLocalAddr2);
        editRemoteAddr = (EditText)findViewById(R.id.editRemoteAddr);
        buttonUpdate = (Button)findViewById(R.id.buttonUpdate);
        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // UDP通信の更新
                updateUdp();
            }
        });
        textStatus = (TextView)findViewById(R.id.textStatus);
        textCtrls = new TextView[4];
        textServos = new TextView[4];
        textCtrls[0] = (TextView)findViewById(R.id.textCtrl1);
        textCtrls[1] = (TextView)findViewById(R.id.textCtrl2);
        textCtrls[2] = (TextView)findViewById(R.id.textCtrl3);
        textCtrls[3] = (TextView)findViewById(R.id.textCtrl4);
        textServos[0] = (TextView)findViewById(R.id.textServo1);
        textServos[1] = (TextView)findViewById(R.id.textServo2);
        textServos[2] = (TextView)findViewById(R.id.textServo3);
        textServos[3] = (TextView)findViewById(R.id.textServo4);

    }
    // 画面表示時
    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUGGING) Log.e(TAG, "+ ON RESUME +");

        // UDP通信の更新
        updateUdp();
    }

    // 画面消去時
    @Override
    protected void onPause()
    {
        if(DEBUGGING) Log.e(TAG, "- ON PAUSE -");
        super.onPause();
    }

    // 画面消滅時
    @Override
    protected void onDestroy() {
        if (DEBUGGING) Log.e(TAG, "-- ON DESTROY --");
        super.onDestroy();
    }

    /************************************************************
     * UDP通信処理
     ************************************************************/

    // UDP通信の更新
    private void updateUdp()
    {

    }
}
