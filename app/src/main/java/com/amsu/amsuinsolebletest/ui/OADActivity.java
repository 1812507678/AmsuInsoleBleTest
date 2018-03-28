package com.amsu.amsuinsolebletest.ui;

import android.app.Dialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.ble.api.DataUtil;
import com.ble.ble.oad.OADListener;
import com.ble.ble.oad.OADProxy;
import com.ble.ble.oad.OADType;
import com.amsu.amsuinsolebletest.MainActivity;
import com.amsu.amsuinsolebletest.R;
import com.amsu.amsuinsolebletest.util.HandlerUtil;
import com.amsu.amsuinsolebletest.util.LeProxy;
import com.amsu.amsuinsolebletest.util.ToastUtil;
import com.ble.gatt.GattAttributes;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * CC2541的OAD需判断镜像类别是A还是B，只有类别不同才可以升级
 * <p>
 * 本Demo给出的几个发送间隔仅供参考
 */
public class OADActivity extends AppCompatActivity implements OnClickListener, OADListener {
    private final String TAG = "OADActivity";

    private static final int REQ_FILE_PATH = 1;

    // CC2541的升级测试程序（工程的assets目录），升级成功会替换模块程序（慎用）
    private static final String OAD_FILE_A = "A-DK-SL-01-20160125-V1.0.3.bin";
    private static final String OAD_FILE_B = "B-DK-SL-01-20160125-V1.0.3.bin";

    private TextView mTvConnectionState;
    private TextView mTvTargetImageType;
    private TextView mTvProgress;
    private TextView mTvBytes;
    private TextView mTvTime;
    private TextView mTvFilePath;
    private ProgressBar mProgressBar;
    private Button mBtnStart;

    private int mSendInterval = 20;//发送间隔
    private String mDeviceName;
    private String mDeviceAddress;
    private String mFilePath;
    private final ProgressInfo mProgressInfo = new ProgressInfo();
    private OADProxy mOADProxy;
    private LeProxy mLeProxy;

    private static class ProgressInfo {
        int iBytes;
        int nBytes;
        long milliseconds;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            Bundle data = msg.getData();

            switch (msg.what) {                case HandlerUtil.MSG_OAD_PREPARED:
                    mBtnStart.setText(R.string.oad_cancel);
                    // 准备就绪，开始升级
                    mOADProxy.startProgramming(mSendInterval);
                    break;

                case HandlerUtil.MSG_OAD_FINISH:
                case HandlerUtil.MSG_OAD_INTERRUPT:
                    displayData(data);
                    mBtnStart.setText(R.string.oad_start);
                    break;

                case HandlerUtil.MSG_OAD_PROGRESS_CHANGED:
                    displayData(data);
                    break;
            }
        }
    };

    private void displayData(Bundle data) {
        mProgressInfo.iBytes = data.getInt(HandlerUtil.EXTRA_I_BYTES);
        mProgressInfo.nBytes = data.getInt(HandlerUtil.EXTRA_N_BYTES);
        mProgressInfo.milliseconds = data.getLong(HandlerUtil.EXTRA_MILLISECONDS);

        updateProgressUi();
    }

    private void updateProgressUi() {
        long seconds = mProgressInfo.milliseconds / 1000;

        int progress = 0;
        if (mProgressInfo.nBytes != 0) {
            progress = 100 * mProgressInfo.iBytes / mProgressInfo.nBytes;
        }
        String time = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
        String bytes = mProgressInfo.iBytes / 1024 + "KB/" + mProgressInfo.nBytes / 1024 + "KB";

        mTvProgress.setText(progress + "%");
        mTvTime.setText(time);
        mTvBytes.setText(bytes);
        mProgressBar.setProgress(progress);
    }

    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String address = intent.getStringExtra(LeProxy.EXTRA_ADDRESS);
            if (!address.equals(mDeviceAddress)) return;

            switch (intent.getAction()){
                case LeProxy.ACTION_GATT_DISCONNECTED:// 断线
                    mTvConnectionState.setText(R.string.disconnected);
                    break;

                case LeProxy.ACTION_DATA_AVAILABLE:{
                    String uuid = intent.getStringExtra(LeProxy.EXTRA_UUID);
                    byte[] data = intent.getByteArrayExtra(LeProxy.EXTRA_DATA);
                    if(GattAttributes.TI_OAD_Image_Identify.toString().equals(uuid)){
                        short ver = DataUtil.buildUint16(data[1], data[0]);
                        Character imgType = ((ver & 1) == 1) ? 'B' : 'A';
                        // 显示模块当前程序的镜像类型（A/B）
                        mTvTargetImageType.setText("Target Image Type: " + imgType);

                    } else if (GattAttributes.TI_OAD_Image_Block.toString().equals(uuid)) {
                        Log.e(TAG, "OAD Block Rx: " + DataUtil.byteArrayToHex(data));
                    }
                }
                break;
            }
        }
    };


    private IntentFilter makeFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(LeProxy.ACTION_GATT_DISCONNECTED);
        filter.addAction(LeProxy.ACTION_DATA_AVAILABLE);
        return filter;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oad);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mDeviceName = getIntent().getStringExtra(MainActivity.EXTRA_DEVICE_NAME);
        mDeviceAddress = getIntent().getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        initView();

        mLeProxy = LeProxy.getInstance();
        mOADProxy = mLeProxy.getOADProxy(this, OADType.cc2541_oad);// CC2541普通OAD
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, makeFilter());

        new Timer().schedule(new GetTargetImgInfoTask(mDeviceAddress), 100, 100);
    }

    private void initView() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setSubtitle(mDeviceAddress);

        mTvConnectionState = (TextView) findViewById(R.id.oad_tv_state);
        mTvTargetImageType = (TextView) findViewById(R.id.oad_tv_image_type);
        mTvProgress = (TextView) findViewById(R.id.oad_tv_progress);
        mTvBytes = (TextView) findViewById(R.id.oad_tv_bytes);
        mTvTime = (TextView) findViewById(R.id.oad_tv_time);
        mTvFilePath = (TextView) findViewById(R.id.oad_tv_filepath);
        mProgressBar = (ProgressBar) findViewById(R.id.oad_progressBar);
        mBtnStart = (Button) findViewById(R.id.oad_btn_start);

        mTvFilePath.setText(mFilePath);
        mBtnStart.setOnClickListener(this);

        updateProgressUi();

        findViewById(R.id.oad_btn_load_file).setOnClickListener(this);

        // 发送间隔
        final String[] intervals = getResources().getStringArray(R.array.oad_send_interval_values);
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                intervals);
        Spinner intervalSpinner = (Spinner) findViewById(R.id.oad_sp_send_interval);
        intervalSpinner.setAdapter(intervalAdapter);
        intervalSpinner.setSelection(1);// 默认20ms
        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String s = intervals[position];
                mSendInterval = Integer.valueOf(s.substring(0, s.indexOf("ms")));
                Log.i(TAG, "发送间隔：" + mSendInterval + "ms");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private class GetTargetImgInfoTask extends TimerTask {
        int i = 0;
        BluetoothGatt gatt;
        BluetoothGattCharacteristic charIdentify;
        BluetoothGattCharacteristic charBlock;

        public GetTargetImgInfoTask(String address) {
            gatt = mLeProxy.getBluetoothGatt(address);
            if (gatt != null) {
                BluetoothGattService oadService = gatt.getService(GattAttributes.TI_OAD_Service);
                if (oadService != null) {
                    charIdentify = oadService.getCharacteristic(GattAttributes.TI_OAD_Image_Identify);
                    charIdentify.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    mLeProxy.setCharacteristicNotification(gatt, charIdentify, true);

                    charBlock = oadService.getCharacteristic(GattAttributes.TI_OAD_Image_Block);
                    // // 这里打开Notify是测试用的，实际开发中不要打开，否则会影响OAD升级
                    // mLeProxy.setCharacteristicNotification(gatt, this.charBlock, true);
                }
            }
        }

        @Override
        public void run() {
            switch (i) {
                case 0:
                    charIdentify.setValue(new byte[]{0});
                    Log.e(TAG, "write 0: " + gatt.writeCharacteristic(charIdentify));
                    break;

                case 1:
                    charIdentify.setValue(new byte[]{1});
                    Log.e(TAG, "write 1: " + gatt.writeCharacteristic(charIdentify));
                    break;

                default:
                    Log.w(TAG, "$GetTargetImgInfoTask.cancel(): " + cancel());
                    break;
            }
            i++;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 重写返回键事件
            if (mOADProxy.isProgramming()) {
                ToastUtil.showMsg(this, R.string.oad_programming);
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 重写返回键事件
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mOADProxy.isProgramming()) {
                ToastUtil.showMsg(this, R.string.oad_programming);
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");
        mOADProxy.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.oad_btn_load_file:// 加载升级文件
                showLoadFileMenu();
                break;

            case R.id.oad_btn_start:
                if (mOADProxy.isProgramming()) {
                    // 取消升级
                    mOADProxy.stopProgramming();
                } else {
                    // 开始升级
                    if (mFilePath != null) {
                        boolean isAssets = mFilePath.equals(OAD_FILE_A) || mFilePath.equals(OAD_FILE_B);
                        mOADProxy.prepare(mDeviceAddress, mFilePath, isAssets);
                    } else {
                        ToastUtil.showMsg(this, R.string.oad_please_select_a_image);
                    }
                }
                break;
        }
    }

    private void showLoadFileMenu() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ListView menuList = new ListView(this);
        String[] menus = getResources().getStringArray(R.array.load_file_menus);
        menuList.setAdapter(new ArrayAdapter<>(this, R.layout.text_view, menus));
        menuList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                switch (position) {
                    case 0:// 加载测试文件A
                        mFilePath = OAD_FILE_A;
                        mTvFilePath.setText(mFilePath);
                        break;

                    case 1:// 加载测试文件B
                        mFilePath = OAD_FILE_B;
                        mTvFilePath.setText(mFilePath);
                        break;

                    case 2:// 加载本地文件（Download目录）
                        startActivityForResult(new Intent(OADActivity.this, FileActivity.class), REQ_FILE_PATH);
                        break;
                }
                dialog.dismiss();
            }
        });
        dialog.setContentView(menuList);
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQ_FILE_PATH) {
            String filepath = data.getStringExtra(FileActivity.EXTRA_FILE_PATH);
            if (filepath != null) {
                mFilePath = filepath;
                mTvFilePath.setText(mFilePath);
            }
            Log.e(TAG, "########### " + mFilePath);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPrepared(String address) {
        // 准备就绪，开始升级
        if (!address.equals(mDeviceAddress)) {
            Log.e(TAG, "address = " + address + ", Current address = " + mDeviceAddress);
            return;
        }
        HandlerUtil.handleMsg(mHandler, HandlerUtil.MSG_OAD_PREPARED, null);
    }

    @Override
    public void onFinished(String address, int nBytes, long milliseconds) {
        // 升级完毕
        if (!address.equals(mDeviceAddress)) {
            Log.e(TAG, "address = " + address + ", Current address = " + mDeviceAddress);
            return;
        }
        handleMessage(HandlerUtil.MSG_OAD_FINISH, nBytes, nBytes, milliseconds);
    }

    @Override
    public void onInterrupted(String address, int iBytes, int nBytes, long milliseconds) {
        // 升级中断
        if (!address.equals(mDeviceAddress)) {
            Log.e(TAG, "address = " + address + ", Current Address = " + mDeviceAddress);
            return;
        }
        handleMessage(HandlerUtil.MSG_OAD_INTERRUPT, iBytes, nBytes, milliseconds);
    }

    @Override
    public void onProgressChanged(String address, int iBytes, int nBytes, long milliseconds) {
        // 升级进度
        if (!address.equals(mDeviceAddress)) {
            Log.e(TAG, "address = " + address + ", Current Address = " + mDeviceAddress);
            return;
        }
        handleMessage(HandlerUtil.MSG_OAD_PROGRESS_CHANGED, iBytes, nBytes, milliseconds);
    }

    @Override
    public void onBlockWrite(byte[] arg0) {
    }

    /**
     * @param what
     * @param iBytes 已经升级（发送）的字节数
     * @param nBytes 总的字节数
     * @param milliseconds 升级时间（ms）
     */
    private void handleMessage(int what, int iBytes, int nBytes, long milliseconds) {
        Bundle data = new Bundle();
        data.putInt(HandlerUtil.EXTRA_I_BYTES, iBytes);
        data.putInt(HandlerUtil.EXTRA_N_BYTES, nBytes);
        data.putLong(HandlerUtil.EXTRA_MILLISECONDS, milliseconds);
        HandlerUtil.handleMsg(mHandler, what, data);
    }

}