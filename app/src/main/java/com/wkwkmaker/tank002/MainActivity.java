package com.wkwkmaker.tank002;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBtAdapter; // BTアダプタ
    private BluetoothDevice mBtDevice; // BTデバイス
    private BluetoothSocket mBtSocket; // BTソケット
    private BufferedWriter writer;
    private BufferedReader reader;

    private Timer timerSend;

    private boolean nowCommunicating = false; //通信中か？
    private boolean nowJoysticking = false; //ジョイスティック操作中か？
    private int nowJoystickingID = 0;
    private boolean nowShooting = false; //発射操作中か？
    private int nowShootingID = 0;

    private TextView tvJoystickArea;
    private TextView tvShootingArea;
    private TextView tvMessage;
    private SeekBar sbAreaMax;

    private TextView textViewXY;

    private float JoystickX = 0;
    private float JoystickY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvJoystickArea = findViewById(R.id.tvJoystickArea);
        tvShootingArea = findViewById(R.id.tvShootingArea);
        tvMessage = findViewById(R.id.textViewMessage);
        sbAreaMax = findViewById(R.id.seekberAreaMax);

        textViewXY = findViewById(R.id.textView2);


        //プリファレンスから前回のBluetooth接続先をセットする
        SharedPreferences pref = getSharedPreferences("wkwk_TankTurret", MODE_PRIVATE);
        TextView tvc = findViewById(R.id.textViewConnectBTName);
        tvc.setText(pref.getString("selectedBTName", ""));


        //シークバーのイベント定義
        sbAreaMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar,int progress, boolean fromUser) {
                //シークバーが変更されたらテキストボックスに数値を表示する
                TextView tv = findViewById(R.id.textView4);
                tv.setText(String.valueOf(progress));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        timerSend = new Timer();
        //タイマーに直接スケジュールを追加して実行
        timerSend.schedule(new TimerTask() {
            @Override
            public void run() {
                if (tvMessage.getText().equals("通信中")) {
                    nowCommunicating = true;

                    String s = "WM"; //WakuWakuMakerのWM
                    s = s + "T"; //TurretのT

                    if (nowJoysticking) {
                        Rect r = new Rect();
                        tvJoystickArea.getGlobalVisibleRect(r);

                        //操作エリアを８分割し、外周１は最大、中央の２は操作無効、その間がリニアに遷移する

                        //操作範囲の最大値
                        int areaMax = sbAreaMax.getProgress();

                        //X
                        float DX = r.right - r.left;   //エリアのサイズ
                        float JX = JoystickX - r.left; //ポイントの位置
                        if (JX < DX / 8){
                            s = s + "L" + String.format("%03d",areaMax);
                        }else if(JX < DX / 8 * 3){
                            s = s + "L" + String.format("%03d",(int)(areaMax - (JX - (DX / 8)) / (DX / 4) * areaMax));
                        }else if(JX < DX / 8 * 5){
                            s = s + "L000";
                        }else if(JX < DX / 8 * 7){
                            s = s + "R" + String.format("%03d",(int)((JX - (DX / 8 * 5)) / (DX / 4) * areaMax));
                        }else{
                            s = s + "R" + String.format("%03d",areaMax);
                        }
                        //Y
                        float DY = r.bottom - r.top;
                        float JY = JoystickY - r.top;
                        if (JY < DY / 8){
                            s = s + "U" + String.format("%03d",areaMax);
                        }else if(JY < DY / 8 * 3){
                            s = s + "U" + String.format("%03d",(int)(areaMax - (JY - (DY / 8)) / (DY / 4) * areaMax));
                        }else if(JY < DY / 8 * 5){
                            s = s + "U000";
                        }else if(JY < DY / 8 * 7){
                            s = s + "D" + String.format("%03d",(int)((JY - (DY / 8 * 5)) / (DY / 4) * areaMax));
                        }else{
                            s = s + "D" + String.format("%03d",areaMax);
                        }

                    } else {
                        //ジョイスティック操作がされていない場合
                        s = s + "L000U000";
                    }

                    if (nowShooting) {
                        s = s + "1";
                    } else {
                        s = s + "0";
                    }

                    //チェックsum計算
                    int csum = 0;
                    for (int i = 0; i < 8; i++) {
                        if (i != 3) {
                            csum = csum + Integer.parseInt(s.substring(i + 4, i + 5));
                        }
                    }
                    s = s + Integer.toString(csum % 10); //１桁目だけを使用する

                    textViewXY.setText(s);

                    try {
                        writer.write(s);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        tvMessage.setText("データ送信失敗");
                    }
//                    }
                } else if (nowCommunicating) {  //通信中から切り替わったら停止命令を出す
                    try {
                        writer.write("WMS0000000000");   //ストップ命令
                        writer.flush();
                        writer.write("WMS0000000000");   //ストップ命令
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        tvMessage.setText("データ送信失敗");
                    }
                    nowCommunicating = false;
                }
            }
        }, 0, 50);

        tvMessage.setText("未接続");
    }


    @Override
    public boolean onTouchEvent(MotionEvent me) {

        //MessageBox("TOP=" + tvShootingArea.getTop() + " Height=" + tvShootingArea.getHeight() + " LEFT=" + tvShootingArea.getLeft() + " Width=" + tvShootingArea.getWidth());


        switch (me.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (nowJoysticking == false && isitRange(tvJoystickArea, me)) {
                    nowJoysticking = true;
                    nowJoystickingID = me.getPointerId(me.getActionIndex());
                    JoystickX = me.getX(me.getActionIndex());
                    JoystickY = me.getY(me.getActionIndex());
                }
                if (nowShooting == false && isitRange(tvShootingArea, me)) {
                    nowShooting = true;
                    nowShootingID = me.getPointerId(me.getActionIndex());
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (nowJoysticking) {
                    if (me.findPointerIndex(nowJoystickingID) == me.getActionIndex()) {
                        nowJoysticking = false;
                    }
                }
                if (nowShooting) {
                    if (me.findPointerIndex(nowShootingID) == me.getActionIndex()) {
                        nowShooting = false;
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (nowJoysticking) {
                    JoystickX = me.getX(me.findPointerIndex(nowJoystickingID));
                    JoystickY = me.getY(me.findPointerIndex(nowJoystickingID));
                }
                break;
            default:
                nowJoysticking = false;
                nowShooting = false;

                break;
        }

        if (nowJoysticking || nowShooting) {
            if (tvMessage.getText().equals("接続中")) {
                tvMessage.setText("通信中");
            }
        } else {
            if (tvMessage.getText().equals("通信中")) {
                tvMessage.setText("接続中");
            }
        }


        return false;
    }

    //モーションイベントの座標がテキストビュー内に入っていたらTrue
    private boolean isitRange(TextView tv, MotionEvent me) {
        Rect rect = new Rect();
        tv.getGlobalVisibleRect(rect);

        if (rect.top <= me.getY(me.getActionIndex()) &&
                rect.bottom >= me.getY(me.getActionIndex()) &&
                rect.left <= me.getX(me.getActionIndex()) &&
                rect.right >= me.getX(me.getActionIndex())) {
            return true;
        }
        return false;
    }



    // アクションバーを表示するメソッド
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }


    // オプションメニューのアイテムが選択されたときに呼び出されるメソッド
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_connect:    //接続
                // BTの準備 --------------------------------------------------------------
                tvMessage.setText("接続準備中");

                // BTアダプタのインスタンスを取得
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBtAdapter == null) {
                    tvMessage.setText("BTアダプタの取得失敗null");
                    return true;
                }
                if (!mBtAdapter.isEnabled()) {
                    tvMessage.setText("Bluetoothが無効になってます");
                    return true;
                }

                //BTデバイス名を取得
                String BTName = (String) ((TextView) findViewById(R.id.textViewConnectBTName)).getText();

                // BTデバイスのインスタンスを取得
                Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
                for (BluetoothDevice dev : bondedDevices) {
                    if (dev.getName().equals(BTName)) {
                        mBtDevice = dev;
                    }
                }
                if (mBtDevice == null) {
                    tvMessage.setText("デバイスが見つからない(" + BTName + ")");
                    return true;
                }

                // BTソケットのインスタンスを取得
                try {
                    // 接続に使用するプロファイルを指定
                    mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                } catch (IOException e) {
                    tvMessage.setText("ソケットの取得失敗");
                    e.printStackTrace();
                }

                // ソケットを接続する
                try {
                    mBtSocket.connect();
                    writer = new BufferedWriter(new OutputStreamWriter(mBtSocket.getOutputStream(), "ASCII"));
                    reader = new BufferedReader(new InputStreamReader(mBtSocket.getInputStream(), "ASCII"));
                    tvMessage.setText("接続中");
                } catch (IOException e) {
                    tvMessage.setText("デバイスへの接続失敗");
                    e.printStackTrace();
                }

                return true;

            case R.id.item_disconnect:
                // ソケットを閉じる
                tvMessage.setText("未接続");
                try {
                    mBtSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;


            case R.id.item_BTSelect:
                // BTアダプタのインスタンスを取得
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                // BTデバイスの名前をリストに入れる
                List<String> BTlist = new ArrayList<>();
                Set<BluetoothDevice> bondedDevicesB = mBtAdapter.getBondedDevices();
                for (BluetoothDevice dev : bondedDevicesB) {
                    BTlist.add(dev.getName());
                }
                //Listから文字配列に変換
                String[] BTListS = BTlist.toArray(new String[BTlist.size()]);

                //ダイアログ(リスト)の表示
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("接続先")
                        .setItems(BTListS, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //選択したアイテムを接続先として設定する
                                TextView tvc = findViewById(R.id.textViewConnectBTName);
                                tvc.setText(BTListS[which]);
                                //接続先名をプリファレンスに保存する
                                SharedPreferences pref = getSharedPreferences("wkwk_TankTurret", MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("selectedBTName", BTListS[which]);
                                editor.commit();
                            }
                        })
                        .show();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ソケットを閉じる
        try {
            mBtSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
