package net.lipoyang.robotarmmon;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements WiFiComm.WiFiCommListener{

    // デバッグ用
    private static final String TAG = "MainActivity";
    private static final boolean DEBUGGING = true;

    // Views
    private TextView textLocalAddr;
    private EditText editRemoteAddr;
    private Button buttonUpdate;
    private TextView textStatus;
    private TextView[] textCtrls;
    private TextView[] textServos;

    // WiFi Communication
    private WiFiComm mWiFiComm;

    // 接続確認コマンド送信用タイマ
    private Timer timerCommandA = null;

    // リモートアドレス
    private String mRemoteAddr = "";

    // データ
    int[] adval = new int[4];
    int[] servo = new int[4];

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
        textLocalAddr = (TextView)findViewById(R.id.textLocalAddr);
        editRemoteAddr = (EditText)findViewById(R.id.editRemoteAddr);
        buttonUpdate = (Button)findViewById(R.id.buttonUpdate);
        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // WiFi通信の停止
                stopWiFi();
                // WiFi通信の開始
                startWiFi();
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

        // load remote address
        SharedPreferences pref = getSharedPreferences("RemoteAddress", MODE_PRIVATE);
        //mRemoteAddr = "192.168.4.1";
        mRemoteAddr = pref.getString("RemoteAddress", "192.168.4.1");
        editRemoteAddr.setText(mRemoteAddr);

        // initialize WiFi
        mWiFiComm = WiFiComm.getInstance();
        mWiFiComm.init();
    }
    
    // 画面表示時
    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUGGING) Log.e(TAG, "+ ON RESUME +");
        
        // WiFi通信の開始
        startWiFi();
    }

    // 画面消去時
    @Override
    protected void onPause() {
        if(DEBUGGING) Log.e(TAG, "- ON PAUSE -");
        
        // WiFi通信の停止
        stopWiFi();
        
        super.onPause();
    }

    // 画面消滅時
    @Override
    protected void onDestroy() {
        if (DEBUGGING) Log.e(TAG, "-- ON DESTROY --");
        super.onDestroy();
    }

    /************************************************************
     * WiFi通信関連の処理
     ************************************************************/
    
    // WiFi通信の開始
    private void startWiFi(){
        
        // WiFiのローカルIPアドレスの表示
        String address = getWiFiIPAddress();
        textLocalAddr.setText(address);

        // WiFi通信の開始
        String remoteAddr = editRemoteAddr.getText().toString();
        if(!remoteAddr.equals(mRemoteAddr)){
            mRemoteAddr = remoteAddr;
            SharedPreferences pref = getSharedPreferences("RemoteAddress", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("RemoteAddress", mRemoteAddr);
            editor.apply();
        }
        if(DEBUGGING) Log.e(TAG, "mRemoteAddr = "+ mRemoteAddr);
        mWiFiComm.setListener(this);
        mWiFiComm.start(remoteAddr);

        // WiFi接続状態の表示
        if(mWiFiComm.isConnected()){
            textStatus.setText("接続済");
            textStatus.setTextColor(Color.parseColor("#32CD32"));
        }else{
            textStatus.setText("未接続");
            textStatus.setTextColor(Color.parseColor("#808080"));
        }
        
        // 接続確認コマンドの送信開始
        timerCommandA = new Timer();
        timerCommandA.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(mWiFiComm.isConnected()){
                    timerCommandA.cancel();
                    timerCommandA = null;
                }else{
                    String command = "#A$";
                    byte [] bCommand=command.getBytes();
                    mWiFiComm.send(bCommand);
                }
            }
        }, 0, 1000);
    }
    
    // WiFi通信の停止
    private void stopWiFi(){
        
        // 接続確認コマンドの送信停止
        if(timerCommandA != null){
            timerCommandA.cancel();
            timerCommandA = null;
        }
        // WiFi通信の停止
        mWiFiComm.stop();
        mWiFiComm.clearListener();
    }
    
    // WiFiのIPアドレスの取得
    private String getWiFiIPAddress(){
        String wifi_address = "---.---.---.---";

        try{
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                 interfaces.hasMoreElements();){
                NetworkInterface networkInterface = interfaces.nextElement();
                for (Enumeration<InetAddress> ipAddressEnum = networkInterface.getInetAddresses(); ipAddressEnum.hasMoreElements();){
                    InetAddress inetAddress = (InetAddress) ipAddressEnum.nextElement();
                    //---check that it is not a loopback address and it is ipv4---
                    if(!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)){
                        String address = inetAddress.getHostAddress();
                        if(address.substring(0,3).equals("192")){
                            wifi_address = address;
                            return wifi_address;
                        }
                    }
                }
            }
        }catch (SocketException ex){
            Log.e("getLocalIpv4Address", ex.toString());

        }
        return wifi_address;
    }

    /************************************************************
     * WiFiイベントリスナ
     ************************************************************/
    @Override
    public void onConnect() {
        if(DEBUGGING) Log.e(TAG, "onConnect");
        // Connected!
        textStatus.setText("接続済");
        textStatus.setTextColor(Color.parseColor("#32CD32"));
    }
    @Override
    public void onDisconnect() {
        if(DEBUGGING) Log.e(TAG, "onDisconnect");
        // Disconnected!
        textStatus.setText("未接続");
        textStatus.setTextColor(Color.parseColor("#808080"));
    }
    @Override
    public void onReceive(byte[] data) {
        // 先頭文字チェック
        if(data[0] != (byte)'#')
        {
            return;
        }
        // コマンド別処理
        switch (data[1])
        {
            // A/D値・サーボ指令値コマンド
            case (byte)'D':
                if (data[14] == (byte)'$')
                {
                    for(int i = 0; i < 4; i++)
                    {
                        adval[i] = ((int)data[2 + 2 * i] << 8)  | (int)data[3 + 2 * i];
                        servo[i] = (int)data[10 + i];
                        textCtrls[i].setText(String.format("%d", adval[i]));
                        textServos[i].setText(String.format("%d", servo[i]));
                    }
                }
                break;
            // 生存確認コマンド
            case (byte)'A':
                if(data[2] == (byte)'$')
                {
                    /*** onConnect()で処理するのでここでの処理は不要 ***/
                    //textStatus.setText("接続済");
                    //textStatus.setTextColor(Color.parseColor("#32CD32"));
                }
                break;
            // 不明なコマンド
            default:
                break;
        }
    }
}
