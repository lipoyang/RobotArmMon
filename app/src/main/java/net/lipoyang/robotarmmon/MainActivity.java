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
                // UDP通信の更新
                // TODO
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
        String remoteAddr = pref.getString("RemoteAddress", "192.168.4.1");
        editRemoteAddr.setText(remoteAddr);

        // initialize WiFi
        mWiFiComm = WiFiComm.getInstance();
        mWiFiComm.init();
    }
    // 画面表示時
    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUGGING) Log.e(TAG, "+ ON RESUME +");

        // WiFiのローカルIPアドレスの表示
        String address = getWiFiIPAddress();
        textLocalAddr.setText(address);

        // WiFi通信の開始
        String remoteAddr = editRemoteAddr.toString();
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
    }

    // 画面消去時
    @Override
    protected void onPause()
    {
        if(DEBUGGING) Log.e(TAG, "- ON PAUSE -");

        // stop WiFi
        mWiFiComm.stop();
        mWiFiComm.clearListener();

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
    public void onReceive(byte[] value) {
        // mResultText.setText(new String(value));
    }
}
