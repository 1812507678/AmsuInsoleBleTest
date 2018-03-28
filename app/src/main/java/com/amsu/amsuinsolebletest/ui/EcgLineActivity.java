package com.amsu.amsuinsolebletest.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.amsu.amsuinsolebletest.R;
import com.amsu.amsuinsolebletest.ui.view.EcgView;
import com.amsu.amsuinsolebletest.util.Constant;
import com.amsu.amsuinsolebletest.util.EcgFilterUtil_1;
import com.amsu.amsuinsolebletest.util.LeProxy;
import com.ble.api.DataUtil;

public class EcgLineActivity extends AppCompatActivity {

    private static final String TAG = "EcgLineActivity";
    private EcgView pv_healthydata_path;
    private EcgFilterUtil_1 ecgFilterUtil_1;
    private TextView tv_heart;
    private LeProxy mLeProxy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg_line);

        initView();
    }

    private void initView() {
        pv_healthydata_path = (EcgView) findViewById(R.id.pv_healthydata_path);
        tv_heart = findViewById(R.id.tv_heart);

        ecgFilterUtil_1 = new EcgFilterUtil_1();


        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, makeFilter());

        mLeProxy = LeProxy.getInstance();

        Intent intent = getIntent();
        String mac_address = intent.getStringExtra("mac_address");
        mConnectedAddress = mac_address;
    }


    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case LeProxy.ACTION_GATT_CONNECTED:
                    Log.i(TAG,"已连接 " );
                    break;
                case LeProxy.ACTION_GATT_DISCONNECTED:
                    Log.w(TAG,"已断开 ");

                    break;
                case LeProxy.ACTION_CONNECT_ERROR:
                    Log.w(TAG,"连接异常 ");

                    break;
                case LeProxy.ACTION_CONNECT_TIMEOUT:
                    Log.w(TAG,"连接超时 ");

                    break;
                case LeProxy.ACTION_DATA_AVAILABLE:// 接收到从机数据
                    //Log.i(TAG,"接收到从机数据");

                    byte[] data = intent.getByteArrayExtra(LeProxy.EXTRA_DATA);
                    dealwithLebDataChange(DataUtil.byteArrayToHex(data));

                    break;
            }
        }
    };

    int [] ecgInts = new int[10];
    String allHeartString = "心率：";
    private int mPreHeartRate;
    private String mConnectedAddress;

    private void dealwithLebDataChange(String hexData) {
        String[] split = hexData.split(" ");
        if (split.length==20){
            for (int i=0;i<split.length/2;i++){
                short i1 = (short) Integer.parseInt(split[2 * i] + split[2 * i + 1], 16);
                Log.i(TAG,""+i1);
                //ecgInts[i] = i1 /256+120;
                ecgInts[i] = i1 /16;
                Log.i(TAG,"ecgInts[i]:"+ecgInts[i]);
            }

            //滤波
            for (int i=0;i<ecgInts.length;i++){
                ecgInts[i] = ecgFilterUtil_1.miniEcgFilterLp(ecgFilterUtil_1.miniEcgFilterHp (ecgFilterUtil_1.NotchPowerLine(ecgInts[i], 1)));
            }
            pv_healthydata_path.addEcgOnGroupData(ecgInts);
        }
        else  if (split.length==1){
            int curHeartRate = Integer.parseInt(split[0] , 16);
            allHeartString += curHeartRate+", ";
            tv_heart.setText(allHeartString);

            if (curHeartRate!=mPreHeartRate){
                //心率不一样则改变灯的闪烁状态
                String data  = "4238FF01";  //导联脱落
                byte[] bytes = DataUtil.hexToByteArray(data);
                //boolean send = mLeProxy.send(mConnectedAddress, Constant.clothNewSerUuid, Constant.clothNewSendReciveDataCharUuid, bytes, false);
                //Log.i(TAG,"send:"+send);
            }

            mPreHeartRate = curHeartRate;
        }
        else  if (split.length==11 && hexData.startsWith("4131")){
            //主机状态：41 31 2B 01 02 00 00 00 02 03 05
            /*主机状态查询,包括导联连接状态, 返回八字节数据，
            第一字节表示导联状态。1为连接，0为未连接，
            第二字节表示充电状态，1为在充电（不判断电量），2为电量正常，3为电量低，
            第三字节表示红色LED状态0xFF为常亮，0x00为关闭，其他值为闪烁间隔，
            第四字节表示绿色LED状态，具体同红色，
            第五字节表示蓝色LED状态，具体同红色,
            第六字节表示开机触摸键延时，
            第七字节表示关机触摸键延时，
            第八字节表示蓝牙广播超时。*/
        }
    }

    public static IntentFilter makeFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(LeProxy.ACTION_GATT_CONNECTED);
        filter.addAction(LeProxy.ACTION_GATT_DISCONNECTED);
        filter.addAction(LeProxy.ACTION_CONNECT_ERROR);
        filter.addAction(LeProxy.ACTION_CONNECT_TIMEOUT);
        filter.addAction(LeProxy.ACTION_GATT_SERVICES_DISCOVERED);

        filter.addAction(LeProxy.ACTION_GATT_DISCONNECTED);
        filter.addAction(LeProxy.ACTION_RSSI_AVAILABLE);
        filter.addAction(LeProxy.ACTION_DATA_AVAILABLE);
        return filter;
    }

    public void openEcgData(View view) {

    }

    public void judgeState(View view) {
        String data  = "4131";  //导联脱落
        byte[] bytes = DataUtil.hexToByteArray(data);
        Log.i(TAG,"mConnectedAddress:"+mConnectedAddress);
        boolean send = mLeProxy.send(mConnectedAddress, Constant.clothNewSerUuid, Constant.clothNewSendReciveDataCharUuid, bytes, false);
        Log.i(TAG,"检查状态send:"+send);

    }
}
