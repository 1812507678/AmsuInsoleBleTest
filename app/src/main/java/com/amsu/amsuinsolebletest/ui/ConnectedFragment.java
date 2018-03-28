package com.amsu.amsuinsolebletest.ui;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.amsu.amsuinsolebletest.LeDevice;
import com.amsu.amsuinsolebletest.MainActivity;
import com.amsu.amsuinsolebletest.R;
import com.amsu.amsuinsolebletest.service.DfuService;
import com.amsu.amsuinsolebletest.util.Constant;
import com.amsu.amsuinsolebletest.util.HexAsciiWatcher;
import com.amsu.amsuinsolebletest.util.LeProxy;
import com.amsu.amsuinsolebletest.util.TimeUtil;
import com.ble.api.DataUtil;
import com.ble.gatt.GattAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class ConnectedFragment extends Fragment implements View.OnClickListener{
	private final String TAG = "ConnectedFragment";

	private static final int REQ_HEX_INPUT = 3;

	private List<String> mSelectedAddresses;
	private ConnectedDeviceListAdapter mDeviceListAdapter;
	private LeProxy mLeProxy;
	private HexAsciiWatcher mInputWatcher;

	private CheckBox mBoxAscii;
	private CheckBox mBoxEncrypt;
	private EditText mEdtInput;

	String mTestaddress;

	private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String address = intent.getStringExtra(LeProxy.EXTRA_ADDRESS);
			mTestaddress = address;

			switch (intent.getAction()){
				case LeProxy.ACTION_GATT_DISCONNECTED:// 断线
					mSelectedAddresses.remove(address);
					mDeviceListAdapter.removeDevice(address);
					break;

				case LeProxy.ACTION_RSSI_AVAILABLE:{// 更新rssi
					LeDevice device = mDeviceListAdapter.getDevice(address);
					if (device != null) {
						int rssi = intent.getIntExtra(LeProxy.EXTRA_RSSI, 0);
						device.setRssi(rssi);
						mDeviceListAdapter.notifyDataSetChanged();
					}
				}
				break;

				case LeProxy.ACTION_DATA_AVAILABLE:// 接收到从机数据
					if (!isHaveData){
						isHaveData = true;

					}

					displayRxData(intent);
					break;
			}
		}
	};

	boolean isHaveData;
	private long mCurrentTimeMillis =-1;

	private void displayRxData(Intent intent){
		String address = intent.getStringExtra(LeProxy.EXTRA_ADDRESS);
		String uuid = intent.getStringExtra(LeProxy.EXTRA_UUID);
		byte[] data = intent.getByteArrayExtra(LeProxy.EXTRA_DATA);

		LeDevice device = mDeviceListAdapter.getDevice(address);
		if (device != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("timestamp: " + TimeUtil.getTimeStamp() + '\n');
			sb.append("uuid: " + uuid + '\n');
			sb.append("len: " + data.length + '\n');
			sb.append("data: " + DataUtil.byteArrayToHex(data) + '\n');
			device.setRxData(sb.toString());
			int curRecCount = device.getRec40ScendCount()+4;
			device.setRec40ScendCount(curRecCount);
			mDeviceListAdapter.notifyDataSetChanged();

		}

		if (mCurrentTimeMillis==-1){
			mCurrentTimeMillis = System.currentTimeMillis();
		}
		if (System.currentTimeMillis()-mCurrentTimeMillis>=1000*40){
			//到40s
			for (String s:mSelectedAddresses){
				LeDevice ld = mDeviceListAdapter.getDevice(s);
				if (TextUtils.isEmpty(ld.getRecPackageCountString())){
					ld.setRecPackageCountString("数据包数量:"+ld.getRec40ScendCount());
					ld.setRec40ScendCount(0);
				}
				else {
					ld.setRecPackageCountString(ld.getRecPackageCountString()+"， "+ld.getRec40ScendCount());
					ld.setRec40ScendCount(0);
				}
			}
			mDeviceListAdapter.notifyDataSetChanged();
			mCurrentTimeMillis = System.currentTimeMillis();
		}
	}

	private IntentFilter makeFilter(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(LeProxy.ACTION_GATT_DISCONNECTED);
		filter.addAction(LeProxy.ACTION_RSSI_AVAILABLE);
		filter.addAction(LeProxy.ACTION_DATA_AVAILABLE);
		return filter;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDeviceListAdapter = new ConnectedDeviceListAdapter();
		mSelectedAddresses = new ArrayList<>();
		mLeProxy = LeProxy.getInstance();
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mLocalReceiver, makeFilter());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_connected, container, false);
		initView(view);
		return view;
	}

	private void initView(View view) {
		ListView listView = (ListView) view.findViewById(R.id.listView1);
		listView.setAdapter(mDeviceListAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				LeDevice device = mDeviceListAdapter.getItem(position);
				Intent intent = new Intent(getActivity(), BleSetActivity.class);
				intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, device.getAddress());
				intent.putExtra(MainActivity.EXTRA_DEVICE_NAME, device.getName());
				startActivity(intent);
			}
		});

		TextView tvInputBytes = (TextView) view.findViewById(R.id.tv_input_bytes);
		mEdtInput = (EditText) view.findViewById(R.id.edt_msg);
		mInputWatcher = new HexAsciiWatcher(getActivity());
		mInputWatcher.setHost(mEdtInput);
		mInputWatcher.setIndicator(tvInputBytes);
		mEdtInput.addTextChangedListener(mInputWatcher);

		mBoxEncrypt = (CheckBox) view.findViewById(R.id.cbox_encrypt);
		mBoxEncrypt.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateEditText(false);
			}
		});

		mBoxAscii = (CheckBox) view.findViewById(R.id.cbox_ascii);
		mBoxAscii.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				updateEditText(true);
			}
		});

		updateEditText(false);

		view.findViewById(R.id.btn_send).setOnClickListener(this);
		view.findViewById(R.id.btn_disconnect).setOnClickListener(this);
		mEdtInput.setOnClickListener(this);
		mEdtInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				Log.e(TAG, "onFocusChange() - hasFocus=" + hasFocus);
				if(hasFocus) goHexInputActivity();
			}
		});

		Button bu_lookecgline = (Button) view.findViewById(R.id.bu_lookecgline);
		Button bu_updatedevice = (Button) view.findViewById(R.id.bu_updatedevice);
		Button bu_sendupdatedorder = (Button) view.findViewById(R.id.bu_sendupdatedorder);

		bu_lookecgline.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), EcgLineActivity.class);
				if (mSelectedAddresses.size()>0){
					String mac_address = mSelectedAddresses.get(0);
					intent.putExtra("mac_address",mac_address);
				}
				startActivity(intent);
			}
		});

		bu_sendupdatedorder.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				UUID serUuid = UUID.fromString(Constant.readSecondGenerationInfoSerUuid);
				UUID charUuid = UUID.fromString(Constant.sendReceiveSecondGenerationClothCharUuid_1);
				if (mSelectedAddresses.size()>0){
					String mac_address = mSelectedAddresses.get(0);
					boolean send = LeProxy.getInstance().send(mac_address, serUuid, charUuid, "4231", false);
					Log.i(TAG,"进入升级模式: "+send);
				}
			}
		});

		bu_updatedevice.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startUpload();
			}
		});

		DfuServiceListenerHelper.registerProgressListener(getActivity(), mDfuProgressListener);
	}

	public void startUpload() {
		Log.i(TAG,"mSelectedAddresses:"+mSelectedAddresses);
		if (mSelectedAddresses.size()>0){
			String mac_address = mSelectedAddresses.get(0);
			Log.i(TAG,"mac_address:"+mac_address);


			//Uri uri = Uri.parse("http://119.29.201.120:8081/intellingence-web/upload_hardware/BMD_DFU_APP_20180130.zip");

			/*final String myUrlStr = "http://119.29.201.120:8081/intellingence-web/upload_hardware/BMD_DFU_APP_20180130.zip";
			URL url;
			Uri uri = null;
			try {
				url = new URL(myUrlStr);
				uri = Uri.parse( url.toURI().toString() );
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			Log.i(TAG,"uri:"+uri);*/

			new DfuServiceInitiator(mac_address)
					//.setDisableNotification(true)
					//.setKeepBond(true)
					.setZip(R.raw.testfile)
					.start(getContext(), DfuService.class);
		}



	}



	/*@Override
	public void onResume() {
		super.onResume();

		DfuServiceListenerHelper.registerProgressListener(getActivity(), mDfuProgressListener);

	}*/

	@Override
	public void onPause() {
		super.onPause();
		DfuServiceListenerHelper.unregisterProgressListener(getActivity(), mDfuProgressListener);
	}

	private final DfuProgressListener mDfuProgressListener = new DfuProgressListener() {
		@Override
		public void onDeviceConnecting(String deviceAddress) {
			Log.i(TAG, "onDeviceConnecting");
		}

		@Override
		public void onDeviceConnected(String deviceAddress) {
			Log.i(TAG, "onDeviceConnected");
		}

		@Override
		public void onDfuProcessStarting(String deviceAddress) {
			Log.i(TAG, "onDfuProcessStarting");
		}

		@Override
		public void onDfuProcessStarted(String deviceAddress) {
			Log.i(TAG, "onDfuProcessStarted");
		}

		@Override
		public void onEnablingDfuMode(String deviceAddress) {
			Log.i(TAG, "onEnablingDfuMode");
		}

		@Override
		public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
			Log.i(TAG, "onProgressChanged");
			Log.i(TAG, "onProgressChanged" + percent);
			//dfuDialogFragment.setProgress(percent);
		}

		@Override
		public void onFirmwareValidating(String deviceAddress) {
			Log.i(TAG, "onFirmwareValidating");
		}

		@Override
		public void onDeviceDisconnecting(String deviceAddress) {

			Log.i(TAG, "onDeviceDisconnecting");
		}

		@Override
		public void onDeviceDisconnected(String deviceAddress) {
			Log.i(TAG, "onDeviceDisconnected");
		}

		@Override
		public void onDfuCompleted(String deviceAddress) {
			Log.i(TAG, "onDfuCompleted");
			//stopDfu();
			//dfuDialogFragment.getProgressBar().setIndeterminate(true);
			//升级成功，重新连接设备
		}

		@Override
		public void onDfuAborted(String deviceAddress) {
			Log.i(TAG, "onDfuAborted");
			//升级流产，失败
		}

		@Override
		public void onError(String deviceAddress, int error, int errorType, String message) {
			Log.i(TAG, "onError errorType:"+errorType+"message:"+message);
			//stopDfu();
			//dfuDialogFragment.dismiss();
			//Toast.makeText(mContext, "升级失败，请重新点击升级。", Toast.LENGTH_SHORT).show();
		}
	};


	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.btn_send:
				send();//TODO
//				sendLargeMtu();
				break;

			case R.id.btn_disconnect:
				disconnect();
				break;

			case R.id.edt_msg:
				goHexInputActivity();
				break;
		}
	}

	private void goHexInputActivity(){
		if(!mBoxAscii.isChecked()){
			Intent intent = new Intent(getActivity(), HexInputActivity.class);
			intent.putExtra(HexInputActivity.EXTRA_MAX_LENGTH, mInputWatcher.getMaxLength());
			intent.putExtra(HexInputActivity.EXTRA_HEX_STRING, mEdtInput.getText().toString());
			startActivityForResult(intent, REQ_HEX_INPUT);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_HEX_INPUT && resultCode == Activity.RESULT_OK) {
			String hexStr = data.getStringExtra(HexInputActivity.EXTRA_HEX_STRING);
			mEdtInput.setText(hexStr);
			mEdtInput.setSelection(hexStr.length());
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// 向勾选的设备发送数据
	private void send(){
		String inputStr = mEdtInput.getText().toString();
		if (inputStr.length() > 0) {
			byte[] data;
			if(mBoxAscii.isChecked()){
				// 这里将换行符替换成Windows系统的，不过这样统计的字节数就会偏少
				inputStr = inputStr.replaceAll("\r\n", "\n");
				inputStr = inputStr.replaceAll("\n", "\r\n");

				data = inputStr.getBytes();
			}else{
				data = DataUtil.hexToByteArray(inputStr);
			}

			Log.e(TAG, inputStr + " -> " + DataUtil.byteArrayToHex(data));

			for (int i = 0; i < mSelectedAddresses.size(); i++) {
				//mLeProxy.send(mSelectedAddresses.get(i), data, mBoxEncrypt.isChecked());

				/*UUID serUuid = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
				UUID charUuid = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

				UUID serUuid = UUID.fromString("00000001-0000-1000-8000-00805f9b34fb");
				UUID charUuid = UUID.fromString("00000002-0000-1000-8000-00805f9b34fb");*/

				/*byte[] head = DataUtil.hexToByteArray("41372B");
				byte[] userInfo = AesEncodeUtil.encryptReturnBytes("18689463192");
				byte[] end = DataUtil.hexToByteArray("01");

				byte[] all = new byte[20];
				System.arraycopy(head,0,all,0,head.length);
				System.arraycopy(userInfo,0,all,head.length,userInfo.length);
				System.arraycopy(end,0,all,head.length+userInfo.length,end.length);

				mLeProxy.send(mSelectedAddresses.get(i),serUuid, charUuid,all, mBoxEncrypt.isChecked());*/

				UUID serUuid = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
				UUID charUuid = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
				mLeProxy.send(mSelectedAddresses.get(i),serUuid, charUuid,inputStr, mBoxEncrypt.isChecked());

			}


		}
	}

	// 断开勾选的设备
	private void disconnect(){
		for (int i = 0; i < mSelectedAddresses.size(); i++) {
			mLeProxy.disconnect(mSelectedAddresses.get(i));
		}
	}

	private void updateEditText(boolean clearText){
		mLeProxy.setDecode(mBoxEncrypt.isChecked());

		mInputWatcher.setTextType(mBoxAscii.isChecked() ? HexAsciiWatcher.ASCII : HexAsciiWatcher.HEX);
		int maxLen;//可输入的字符串长度
		String hintText;
		if (mBoxAscii.isChecked()) {
			if (mBoxEncrypt.isChecked()) {
				maxLen = 17;
			} else {
				maxLen = 20;
			}
			hintText = getString(R.string.connected_send_ascii_hint, maxLen);
		} else {
			if (mBoxEncrypt.isChecked()) {
				maxLen = 34;
			} else {
				maxLen = 40;
			}
			hintText = getString(R.string.connected_send_hex_hint, maxLen / 2);
		}
		mInputWatcher.setMaxLength(maxLen);
		mEdtInput.setHint(hintText);
		if (clearText){
			mEdtInput.setText("");
			mInputWatcher.setIndicatorText(getString(R.string.input_bytes, 0));
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
		mSelectedAddresses.clear();
		mDeviceListAdapter.clear();
		List<BluetoothDevice> connectedDevices = mLeProxy.getConnectedDevices();
		for (int i = 0; i < connectedDevices.size(); i++) {
			String name = connectedDevices.get(i).getName();
			String address = connectedDevices.get(i).getAddress();
			mSelectedAddresses.add(address);
			mDeviceListAdapter.addDevice(new LeDevice(name, address));
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mLocalReceiver);
	}

	private class ConnectedDeviceListAdapter extends BaseAdapter {
		List<LeDevice> mDevices;
		LayoutInflater mInflater;

		public ConnectedDeviceListAdapter() {
			mDevices = new ArrayList<>();
			mInflater = getActivity().getLayoutInflater();
		}

		private boolean isOadSupported(String address) {
			BluetoothGatt gatt = mLeProxy.getBluetoothGatt(address);
			if (gatt != null) {
				BluetoothGattService oadService = gatt.getService(GattAttributes.TI_OAD_Service);
				return oadService != null;
			}
			return false;
		}

		public LeDevice getDevice(String address) {
			for (LeDevice connectedLeDevice : mDevices) {
				if (connectedLeDevice.getAddress().equals(address)) {
					return connectedLeDevice;
				}
			}
			return null;
		}

		public void addDevice(LeDevice device) {
			if (!mDevices.contains(device)) {
				device.setOadSupported(isOadSupported(device.getAddress()));
				mDevices.add(device);
				notifyDataSetChanged();
			}
		}

		public void removeDevice(String address) {
			int location = -1;

			for (int i = 0; i < mDevices.size(); i++) {
				if (mDevices.get(i).getAddress().equals(address)) {
					location = i;
					break;
				}
			}

			if (location != -1) {
				mDevices.remove(location);
				notifyDataSetChanged();
			}
		}

		public void clear() {
			mDevices.clear();
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mDevices.size();
		}

		@Override
		public LeDevice getItem(int position) {
			return mDevices.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.item_device_list, null);
				holder = new ViewHolder();
				holder.tvName = (TextView) convertView.findViewById(R.id.device_name);
				holder.tvAddress = (TextView) convertView.findViewById(R.id.device_address);
				holder.tvRxData = (TextView) convertView.findViewById(R.id.txt_rx_data);
				holder.tvRssi = (TextView) convertView.findViewById(R.id.txt_rssi);
				holder.txt_datacount = (TextView) convertView.findViewById(R.id.txt_datacount);
				holder.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox1);
				holder.btnOAD = (Button) convertView.findViewById(R.id.btn_oad);
				holder.imageView = (ImageView) convertView.findViewById(R.id.imageView1);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.imageView.setVisibility(View.VISIBLE);
			holder.checkBox.setOnCheckedChangeListener(null);

			final LeDevice device = mDevices.get(position);
			String name = device.getName();
			if (name == null || device.getName().trim().length() == 0) {
				holder.tvName.setText(R.string.unknown_device);
			} else {
				holder.tvName.setText(name);
			}
			holder.tvAddress.setText(device.getAddress());
			holder.checkBox.setVisibility(View.VISIBLE);
			holder.tvRxData.setVisibility(View.VISIBLE);
			holder.tvRxData.setText(device.getRxData());
			holder.tvRssi.setText("rssi: " + device.getRssi() + "dbm");
			holder.txt_datacount.setText(device.getRecPackageCountString());
			final String address = device.getAddress();
			holder.checkBox.setChecked(mSelectedAddresses.contains(address));
			holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// 勾选设备，发送数据时只给已勾选的设备发送
					if (isChecked) {
						if (!mSelectedAddresses.contains(address)) {
							mSelectedAddresses.add(address);
						}
					} else {
						if (mSelectedAddresses.contains(address)) {
							mSelectedAddresses.remove(address);
						}
					}
					Log.i(TAG, "Selected " + mSelectedAddresses.size());
				}
			});

			if (device.isOadSupported()) {
				holder.btnOAD.setVisibility(View.VISIBLE);
				holder.btnOAD.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// 进入OAD界面
						Intent oadIntent = new Intent(getActivity(), OADActivity.class);
						oadIntent.putExtra(MainActivity.EXTRA_DEVICE_NAME, device.getName());
						oadIntent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, device.getAddress());
						startActivity(oadIntent);
					}
				});
			}

			return convertView;
		}
	}

	private static class ViewHolder {
		TextView tvName, tvAddress, tvRxData, tvRssi,txt_datacount;
		CheckBox checkBox;
		Button btnOAD;
		ImageView imageView;
	}
}