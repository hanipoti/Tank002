package com.wkwkmaker.tank002;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
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


//public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
public class MainActivity extends AppCompatActivity  implements View.OnTouchListener {

    private BluetoothAdapter mBtAdapter; // BTアダプタ
    private BluetoothDevice mBtDevice; // BTデバイス
    private BluetoothSocket mBtSocket; // BTソケット
    private BufferedWriter writer;
    private BufferedReader reader;
    private Timer timerSend;

    static boolean nowCommunicating = false; //通信中か？
    static boolean nowJoysticking = false; //ジョイスティック操作中か？
    static boolean nowShooting = false; //発射操作中か？
    private int JoystickAreaTop = 0;
    private int JoystickAreaLeft = 0;
    private int JoystickAreaWidth = 0;

    private final StringBuffer info = new StringBuffer("Test onTouchEvent\n\n");
    private TextView textView;
    private TextView textViewXY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textJoystickArea);
        textViewXY = findViewById(R.id.textView2);

        Button button = findViewById(R.id.button2);
        button.setOnClickListener( v -> {
            info.delete(0, info.length());
            textView.setText(info);
        });






        //プリファレンスから前回の接続先をセットする
        SharedPreferences pref = getSharedPreferences("wkwk_TankTurret",MODE_PRIVATE);
        TextView tvc = findViewById(R.id.textViewConnectBTName);
        tvc.setText(pref.getString("selectedBTName",""));

        timerSend = new Timer();

//        Button btn = findViewById(R.id.button);
//        btn.setOnTouchListener(this);
        TextView tv = findViewById(R.id.textJoystickArea);
        tv.setOnTouchListener(this);


        //タイマーに直接スケジュールを追加して実行
        timerSend.schedule(new TimerTask() {
            @Override
            public void run() {
                TextView tvm = findViewById(R.id.textViewMessage);
                if (tvm.getText().equals("通信中")) {
                    nowCommunicating = true;

//                    if (((RadioButton) findViewById(R.id.radioButtonOpeModeLR)).isChecked() || ((RadioButton) findViewById(R.id.radioButtonOpeModeUD)).isChecked()) {
                        String s = "WM"; //WakuWakuMakerのWM
                            s = s + "T"; //TurretのT

//                        s = s + CheckedNo((RadioButton) findViewById(R.id.radioButtonL1Stop), (RadioButton) findViewById(R.id.radioButtonL1Extends), (RadioButton) findViewById(R.id.radioButtonL1Shrink));
//                        s = s + CheckedNo((RadioButton) findViewById(R.id.radioButtonL2Stop), (RadioButton) findViewById(R.id.radioButtonL2Extends), (RadioButton) findViewById(R.id.radioButtonL2Shrink));
//                        s = s + CheckedNo((RadioButton) findViewById(R.id.radioButtonL3Stop), (RadioButton) findViewById(R.id.radioButtonL3Extends), (RadioButton) findViewById(R.id.radioButtonL3Shrink));
//                        s = s + CheckedNo((RadioButton) findViewById(R.id.radioButtonL4Stop), (RadioButton) findViewById(R.id.radioButtonL4Extends), (RadioButton) findViewById(R.id.radioButtonL4Shrink));
//                        s = s + CheckedNo((RadioButton) findViewById(R.id.radioButtonR1Stop), (RadioButton) findViewById(R.id.radioButtonR1Extends), (RadioButton) findViewById(R.id.radioButtonR1Shrink));
//                        s = s + CheckedNo((RadioButton) findViewById(R.id.radioButtonR2Stop), (RadioButton) findViewById(R.id.radioButtonR2Extends), (RadioButton) findViewById(R.id.radioButtonR2Shrink));
//                        s = s + CheckedNo((RadioButton) findViewById(R.id.radioButtonR3Stop), (RadioButton) findViewById(R.id.radioButtonR3Extends), (RadioButton) findViewById(R.id.radioButtonR3Shrink));
//                        s = s + CheckedNo((RadioButton) findViewById(R.id.radioButtonR4Stop), (RadioButton) findViewById(R.id.radioButtonR4Extends), (RadioButton) findViewById(R.id.radioButtonR4Shrink));

                        //チェックsum計算
                        int csum = 0;
                        for (int i = 0; i < 8;i++){
                            csum = csum + Integer.parseInt(s.substring(i + 3 ,i + 4));
                        }
                        s = s + Integer.toString(csum % 10); //１桁目だけを使用する

                        try {
                            writer.write(s);
                            writer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                            MessageBox("データ送信失敗");
                        }
//                    }
                }else if(nowCommunicating){  //通信中から切り替わったら停止命令を出す
                    try {
                        writer.write("WMS000000000");   //ストップ命令
                        writer.flush();
                        writer.write("WMS000000000");   //ストップ命令
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        MessageBox("データ送信失敗");
                    }
                    nowCommunicating = false;
                }
            }
        }, 0, 50);

        MessageBox("未接続");
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        //info.append("onTouchEvent\n");

        if (JoystickAreaTop == 0) {
            TextView tv = findViewById(R.id.textJoystickArea);
            JoystickAreaTop = tv.getTop();
            JoystickAreaLeft = tv.getLeft();
            JoystickAreaWidth = tv.getWidth();
            MessageBox("TOP=" + JoystickAreaTop +" LEFT=" + JoystickAreaLeft +" Width=" + JoystickAreaWidth);
        }


        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                info.append("ACTION_DOWN\n");

                break;
            case MotionEvent.ACTION_UP:
                info.append("ACTION_UP\n");

                break;
            case MotionEvent.ACTION_MOVE:
                //info.append("ACTION_MOVE\n");

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                info.append("ACTION_POINTER_DOWN\n");

                break;
            case MotionEvent.ACTION_POINTER_UP:
                info.append("ACTION_POINTER_UP\n");

                break;
            case MotionEvent.ACTION_CANCEL:
                info.append("ACTION_CANCEL\n");

                break;
        }
        textViewXY.setText("X=" + motionEvent.getX() +" Y=" + motionEvent.getY());
        textView.setText(info);



        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        info.append(v.getId());
        info.append(" onTouch\n");
        textView.setText(info);

        TextView tvm = findViewById(R.id.textViewMessage);
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_DOWN:
                if (tvm.getText().equals("接続中")) {
                    MessageBox("通信中");
                }
                break;
            case MotionEvent.ACTION_UP:
                if (tvm.getText().equals("通信中")) {
                    MessageBox("接続中");
                }
                break;
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
                MessageBox("接続準備中");

                // BTアダプタのインスタンスを取得
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBtAdapter == null) {
                    MessageBox("BTアダプタの取得失敗null");
                    return true;
                }
                if (!mBtAdapter.isEnabled()) {
                    MessageBox("Bluetoothが無効になってます");
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
                    MessageBox("デバイスが見つからない(" + BTName + ")");
                    return true;
                }

                // BTソケットのインスタンスを取得
                try {
                    // 接続に使用するプロファイルを指定
                    mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                } catch (IOException e) {
                    MessageBox("ソケットの取得失敗");
                    e.printStackTrace();
                }

                // ソケットを接続する
                try {
                    mBtSocket.connect();
                    writer = new BufferedWriter(new OutputStreamWriter(mBtSocket.getOutputStream(), "ASCII"));
                    reader = new BufferedReader(new InputStreamReader(mBtSocket.getInputStream(), "ASCII"));
                    MessageBox("接続中");
                } catch (IOException e) {
                    MessageBox("デバイスへの接続失敗");
                    e.printStackTrace();
                }

                return true;

            case R.id.item_disconnect:
                // ソケットを閉じる
                MessageBox("未接続");
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

    private void MessageBox(String str) {
        TextView tvm = findViewById(R.id.textViewMessage);
        tvm.setText(str);

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
