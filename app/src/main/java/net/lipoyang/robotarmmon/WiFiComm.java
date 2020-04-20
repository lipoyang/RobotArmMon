package net.lipoyang.robotarmmon;

import android.os.Handler;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * WiFi Communication Class
 */
public class WiFiComm {
    // Debugging
    private static final String TAG = "WiFiComm";
    private static final boolean DEBUGGING = true;

    /**
     * Event Listener of WiFi Communication
     */
    public interface WiFiCommListener {
        public void onConnect();
        public void onDisconnect();
        public void onReceive(byte[] data);
    }

    // event listener
    private WiFiCommListener mWiFiCommListener = null;
    // is connected
    private boolean mIsConnected = false;
    // WiFi Thread Flag
    private boolean mThreadFlag = false;
    // remote IP address
    private InetAddress mRemoteAddr;
    // local & remote port number
    private final int REMOTE_PORT = 4002;
    private final int LOCAL_PORT  = 4001;
    // handler for events
    private Handler mHandler = new Handler();
    // WiFi receiving thread
    private Thread mThread;

    /************************************************************
     *  for Singleton
     ************************************************************/
    // static instance for singleton
    private static WiFiComm sWiFiComm = new WiFiComm();

    // private constructor for singleton
    private WiFiComm(){

    }

    // get singleton instance
    public static WiFiComm getInstance(){
        return sWiFiComm;
    }

    /************************************************************
     *  Public APIs
     ************************************************************/

    // init WiFi
    public void init() {
        mIsConnected = false;
    }

    // start WiFi
    public boolean start(String remoteAddr){
        try {
            mRemoteAddr = InetAddress.getByName(remoteAddr);
        } catch (Exception e) {
            if(DEBUGGING) Log.e(TAG, "start failed! : "+ remoteAddr);
            return false;
        }
        // start WiFi receiving thread
        mThreadFlag = true;
        mThread = new Thread(new ReceivingThread());
        mThread.start();
        return true;
    }

    // stop WiFi
    public void stop(){
        // terminate WiFi receiving thread
        mThreadFlag = false;
        try{
            mThread.join();
        }catch(Exception e){
            if(DEBUGGING) Log.e(TAG, "terminate failed!");
        }
    }

    // set event listener
    public void setListener(WiFiCommListener listener){
        mWiFiCommListener = listener;
    }

    // clear event listener
    public void clearListener(){
        mWiFiCommListener = null;
    }

    // is connected to GPduino WiFi
    public boolean isConnected(){
        return mIsConnected;
    }

    // write data to GPduino WiFi
    public void send(final byte[] data)
    {
        Thread sendingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    //if(DEBUGGING) Log.e(TAG, "send! "+new String(data) + " to " + mRemoteAddr.toString());
                    DatagramSocket sendSocket = new DatagramSocket();
                    DatagramPacket sendPacket =
                            new DatagramPacket(data, data.length, mRemoteAddr, REMOTE_PORT);

                    sendSocket.send(sendPacket);
                    sendSocket.close();
                }
                catch(Exception e)
                {
                    if(DEBUGGING) Log.e(TAG, "send failed! "+ e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        sendingThread.start();
    }

    /************************************************************
     *  WiFi Receiving Thread
     ************************************************************/
    class ReceivingThread implements Runnable{
        @Override
        public void run() {
            int cntNoData = 0;
            byte[] buffer = new byte[256];

            if(DEBUGGING) Log.e(TAG, "ReceivingThread Begins!");

            // UDP socket
            DatagramSocket receiveSocket;
            try{
                receiveSocket = new DatagramSocket(LOCAL_PORT);
            }catch(Exception ex){
                if(DEBUGGING) Log.e(TAG, "can't create socket:"+ex.getMessage());
                return;
            }

            // unless thread exit
            while(mThreadFlag){
                try
                {
                    // receive (500msec time out)
                    receiveSocket.setSoTimeout(500);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    receiveSocket.receive(packet);

                    // get infomation
                    InetAddress addr = packet.getAddress();
                    int len = packet.getLength();
                    final byte[] bytedata = new byte[len];
                    System.arraycopy(buffer, 0, bytedata, 0, len);
                    // if(DEBUGGING) Log.e(TAG, "mReceivingThread:"+addr.toString()+":"+ new String(bytedata));

                    // from GPduino WiFi?
                    if(addr.equals(mRemoteAddr)){
                        // connected!
                        cntNoData = 0;
                        if(!mIsConnected){
                            if(DEBUGGING) Log.e(TAG, "mReceivingThread: connected!");
                            mIsConnected = true;
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(mWiFiCommListener != null)
                                        mWiFiCommListener.onConnect();
                                }
                            });
                        }
                        // data received!
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(mWiFiCommListener != null)
                                    mWiFiCommListener.onReceive(bytedata);
                            }
                        });
                    }
                }
                catch(Exception e)
                {
                    //if(DEBUGGING) Log.e(TAG, "can't receive:"+e.getMessage());

                    // no data for 3sec => disconnected
                    if(mIsConnected){
                        cntNoData++;
                        if(cntNoData>=6){
                            mIsConnected = false;
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(mWiFiCommListener != null)
                                        mWiFiCommListener.onDisconnect();
                                }
                            });
                        }
                    } else {
                        cntNoData = 0;
                    }
                    try{
                        Thread.sleep(500);
                    }catch(Exception e2){
                        if(DEBUGGING) Log.e(TAG, "sleep failed!");
                    }
                }
            }
            receiveSocket.close();
            if(DEBUGGING) Log.e(TAG, "ReceivingThread Ends!");
        }
    }
}
