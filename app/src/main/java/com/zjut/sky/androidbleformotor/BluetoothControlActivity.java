package com.zjut.sky.androidbleformotor;

import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class BluetoothControlActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = BluetoothControlActivity.class.getSimpleName();
    private BluetoothLeService mBluetoothLeService;

    private IntentFilter intentFilter = new IntentFilter();

    private static final byte MOTOR_INVERSE = 1;
    private static final byte MOTOR_REVERSE = 2;
    private static final byte MOTOR_STOP = 0;


    private Button btnOpen;
    private Button btnClose;
    private Button btnStop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_control);

        btnOpen = (Button) findViewById(R.id.open_btn);
        btnClose = (Button) findViewById(R.id.close_btn);
        btnStop = (Button) findViewById(R.id.stop_btn);

        btnOpen.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnClose.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.open_btn:

                break;
            case R.id.close_btn:
                break;
            case R.id.stop_btn:
                break;
        }
    }

}
