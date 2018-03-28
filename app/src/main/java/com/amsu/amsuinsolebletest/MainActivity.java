package com.amsu.amsuinsolebletest;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;

import com.amsu.amsuinsolebletest.ui.ConnectedFragment;
import com.amsu.amsuinsolebletest.ui.ScanFragment;
import com.amsu.amsuinsolebletest.util.LeProxy;
import com.amsu.amsuinsolebletest.util.ToastUtil;
import com.ble.ble.BleService;

public class MainActivity extends FragmentActivity {
	private static final String TAG = "MainActivity";

	public static final String EXTRA_DEVICE_ADDRESS = "extra_device_address";
	public static final String EXTRA_DEVICE_NAME = "extra_device_name";

	private static final int FRAGMENT_SCAN = 0;
	private static final int FRAGMENT_CONNECTED = 1;

	private static final int REQUEST_ENABLE_BT = 2;

	private BluetoothAdapter mBluetoothAdapter;
	private String[] mFragmentTitles;
	private FragmentTabHost mFragmentTabHost;
	private MainActivity mContext;

	private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String address = intent.getStringExtra(LeProxy.EXTRA_ADDRESS);

			switch (intent.getAction()){
				case LeProxy.ACTION_GATT_CONNECTED:
					ToastUtil.showMsg(mContext, R.string.scan_connected, address + " ");
					break;
				case LeProxy.ACTION_GATT_DISCONNECTED:
					ToastUtil.showMsg(mContext, R.string.scan_disconnected, address + " ");
					break;
				case LeProxy.ACTION_CONNECT_ERROR:
					ToastUtil.showMsg(mContext, R.string.scan_connection_error, address + " ");
					break;
				case LeProxy.ACTION_CONNECT_TIMEOUT:
					ToastUtil.showMsg(mContext, R.string.scan_connect_timeout, address + " ");
					break;
				case LeProxy.ACTION_GATT_SERVICES_DISCOVERED:
					Toast.makeText(mContext, "Services discovered: " + address, Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};


	private IntentFilter makeFilter(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(LeProxy.ACTION_GATT_CONNECTED);
		filter.addAction(LeProxy.ACTION_GATT_DISCONNECTED);
		filter.addAction(LeProxy.ACTION_CONNECT_ERROR);
		filter.addAction(LeProxy.ACTION_CONNECT_TIMEOUT);
		filter.addAction(LeProxy.ACTION_GATT_SERVICES_DISCOVERED);
		return filter;
	}

	private final ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.w(TAG, "onServiceDisconnected()");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LeProxy.getInstance().setBleService(service);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		checkBLEFeature();
		initView();
		bindService(new Intent(this, BleService.class), mConnection, BIND_AUTO_CREATE);
		LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, makeFilter());
	}

	private void checkBLEFeature() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}


	}

	private void initView() {
		mFragmentTitles = getResources().getStringArray(R.array.fragment_titles);
		mFragmentTabHost = (FragmentTabHost) findViewById(R.id.tabhost);
		FragmentManager fragmentManager = getFragmentManager();
		mFragmentTabHost.setup(this, getSupportFragmentManager(), R.id.frame_content);
		for (int i = 0; i < mFragmentTitles.length; i++) {
			TabSpec ts = mFragmentTabHost.newTabSpec(mFragmentTitles[i]);
			ts.setIndicator(mFragmentTitles[i]);
			switch (i) {
				case FRAGMENT_SCAN:
					mFragmentTabHost.addTab(ts, ScanFragment.class, null);
					break;
				case FRAGMENT_CONNECTED:
					mFragmentTabHost.addTab(ts, ConnectedFragment.class, null);
					break;
			}
		}

		ActionBar actionBar = getActionBar();
		Log.i(TAG,"actionBar:"+actionBar);
		if (actionBar != null) {
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM); //Enable自定义的View
			actionBar.setCustomView(R.layout.base_activity_head);//设置自定义的布局：actionbar_custom
		}



	}



	@Override
	protected void onResume() {
		super.onResume();
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver);
		unbindService(mConnection);
	}
}