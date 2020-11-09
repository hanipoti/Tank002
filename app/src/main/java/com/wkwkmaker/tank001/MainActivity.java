package com.wkwkmaker.tank001;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity  implements View.OnTouchListener {

    private BluetoothAdapter mBtAdapter; // BTアダプタ
    private BluetoothDevice mBtDevice; // BTデバイス
    private BluetoothSocket mBtSocket; // BTソケット
    private BufferedWriter writer;
    private BufferedReader reader;
    private Timer timerSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerSend =new Timer();

        Button btn= findViewById(R.id.button);
        btn.setOnTouchListener(this);


        //タイマーに直接スケジュールを追加して実行
        timerSend.schedule(new TimerTask() {
            @Override
            public void run() {
                TextView tvm = findViewById(R.id.textViewMessage);
                if (tvm.getText().equals("通信中")){
                    if (((RadioButton)findViewById(R.id.radioButtonOpeModeLR)).isChecked() || ((RadioButton)findViewById(R.id.radioButtonOpeModeUD)).isChecked()){
                        String s = "828";

                        if (((RadioButton)findViewById(R.id.radioButtonOpeModeLR)).isChecked()) s = s + "E";
                        if (((RadioButton)findViewById(R.id.radioButtonOpeModeUD)).isChecked()) s = s + "L";

                        s = s + CheckedNo((RadioButton)findViewById(R.id.radioButtonL1Stop),(RadioButton)findViewById(R.id.radioButtonL1Extends),(RadioButton)findViewById(R.id.radioButtonL1Shrink));
                        s = s + CheckedNo((RadioButton)findViewById(R.id.radioButtonL2Stop),(RadioButton)findViewById(R.id.radioButtonL2Extends),(RadioButton)findViewById(R.id.radioButtonL2Shrink));
                        s = s + CheckedNo((RadioButton)findViewById(R.id.radioButtonL3Stop),(RadioButton)findViewById(R.id.radioButtonL3Extends),(RadioButton)findViewById(R.id.radioButtonL3Shrink));
                        s = s + CheckedNo((RadioButton)findViewById(R.id.radioButtonL4Stop),(RadioButton)findViewById(R.id.radioButtonL4Extends),(RadioButton)findViewById(R.id.radioButtonL4Shrink));
                        s = s + CheckedNo((RadioButton)findViewById(R.id.radioButtonR1Stop),(RadioButton)findViewById(R.id.radioButtonR1Extends),(RadioButton)findViewById(R.id.radioButtonR1Shrink));
                        s = s + CheckedNo((RadioButton)findViewById(R.id.radioButtonR2Stop),(RadioButton)findViewById(R.id.radioButtonR2Extends),(RadioButton)findViewById(R.id.radioButtonR2Shrink));
                        s = s + CheckedNo((RadioButton)findViewById(R.id.radioButtonR3Stop),(RadioButton)findViewById(R.id.radioButtonR3Extends),(RadioButton)findViewById(R.id.radioButtonR3Shrink));
                        s = s + CheckedNo((RadioButton)findViewById(R.id.radioButtonR4Stop),(RadioButton)findViewById(R.id.radioButtonR4Extends),(RadioButton)findViewById(R.id.radioButtonR4Shrink));
                        s = s + "9";
                        try {
                            writer.write(s);
                            writer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                            MessageBox("データ送信失敗");
                        }
                    }
                }
            }
        }, 0, 50);

        MessageBox("未接続");
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
            case R.id.item1:    //接続
                // BTの準備 --------------------------------------------------------------
                MessageBox("接続中");

                // BTアダプタのインスタンスを取得
                mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBtAdapter == null) {
                    MessageBox("アダプタの取得失敗");
                }
                if (!mBtAdapter.isEnabled()) {
                    MessageBox("アダプタの取得失敗");
                }

                // 相手先BTデバイスのインスタンスを取得
                Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();

//                ArrayList<String> devices = new ArrayList<>();
//                for (BluetoothDevice dev : bondedDevices) {
//                    devices.add(dev.getName());
//                }
//                String[] items = devices.toArray(new String[devices.size()]);
//
//                int whitchBtn = 0;
//
//                new AlertDialog.Builder(this)
//                        .setTitle("接続先")
//                        .setItems(items, new DialogInterface.OnClickListener(){
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                for (BluetoothDevice dev : bondedDevices) {
//                                    if (dev.getName().equals(items[which])) {
//                                        mBtDevice = dev;
//                                    }
//                                }
//                            }
//                        })
//                        .show();


                for (BluetoothDevice dev : bondedDevices) {
                    if (dev.getName().equals("WK-TANK4-1")) {
                        mBtDevice = dev;
                    }
                }
                if (mBtDevice == null) {
                    MessageBox("デバイスの取得失敗(WK-TANK4-1)");
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
            case R.id.item2:
                // ソケットを閉じる
                MessageBox("未接続");
                try {
                    mBtSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void MessageBox(String str){
        TextView tvm= findViewById(R.id.textViewMessage);
        tvm.setText(str);

    }

    private String CheckedNo(RadioButton rb1,RadioButton rb2,RadioButton rb3){
        if (rb1.isChecked()){
            return "0";
        }
        if (rb2.isChecked()){
            return "1";
        }
        if (rb3.isChecked()){
            return "2";
        }
        return "9";
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
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
                    try {
                        writer.write("828S000000009");   //ストップ命令
                        writer.flush();
                        writer.write("828S000000009");   //ストップ命令
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        MessageBox("データ送信失敗");
                    }
                }
                break;
        }
        return true;
    }

    public void sendMessage(View v) {
//        TextView tvm = findViewById(R.id.textViewMessage);
//        if (tvm.getText().equals("接続中")) {
//            MessageBox("通信中");
//        }else if (tvm.getText().equals("通信中")) {
//            MessageBox("接続中");
//            try {
//                writer.write("828S000000009");   //ストップ命令
//                writer.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//                MessageBox("データ送信失敗");
//            }
//         }

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
