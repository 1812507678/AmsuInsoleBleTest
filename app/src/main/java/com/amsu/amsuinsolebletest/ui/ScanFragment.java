package com.amsu.amsuinsolebletest.ui;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.amsu.amsuinsolebletest.LeDevice;
import com.amsu.amsuinsolebletest.R;
import com.amsu.amsuinsolebletest.util.LeProxy;
import com.amsu.amsuinsolebletest.util.ToastUtil;
import com.ble.api.DataUtil;
import com.ble.ble.LeScanRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

public class ScanFragment extends Fragment {
    private final static String TAG = "ScanFragment";
    private static final long SCAN_PERIOD = 3000;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();
    private boolean mScanning;
    private LeProxy mLeProxy;

    private SwipeRefreshLayout mRefreshLayout;
    private SharedPreferences mSp;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLeProxy = LeProxy.getInstance();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        BluetoothManager bm = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bm.getAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRefreshLayout.setRefreshing(true);
        scanLeDevice(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    private void initView(View view) {
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                scanLeDevice(true);
            }
        });

        ListView listView = (ListView) view.findViewById(R.id.listView1);
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setOnItemClickListener(mOnItemClickListener);
        listView.setOnItemLongClickListener(mOnItemLongClickListener);


        mSp = getContext().getSharedPreferences("data", MODE_PRIVATE);

        Set<String> signDeviceAddressFromSP = getSignDeviceAddressFromSP();
        Log.i(TAG,"signDeviceAddressFromSP:"+signDeviceAddressFromSP);
        if (signDeviceAddressFromSP!=null){
            sginDeviceAddress = signDeviceAddressFromSP;
        }

        Button iv_base_rightimage = getActivity().findViewById(R.id.iv_base_rightimage);
        iv_base_rightimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSP();
                sginDeviceAddress.clear();

                for (LeDevice device:mLeDeviceListAdapter.mLeDevices){
                    device.setSign(false);
                }
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        });

        //mSp.edit().putString("test","456").apply();

        String test = mSp.getString("test", "");
        Log.i(TAG,"test:"+test);
    }

    private void clearSP() {
        mSp.edit().clear().apply();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (mBluetoothAdapter.isEnabled()) {
                if (mScanning)
                    return;
                mScanning = true;
                mLeDeviceListAdapter.clear();
                mHandler.postDelayed(mScanRunnable, SCAN_PERIOD);
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                ToastUtil.showMsg(getActivity(), R.string.scan_bt_disabled);
            }
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mRefreshLayout.setRefreshing(false);
            mHandler.removeCallbacks(mScanRunnable);
            mScanning = false;
        }
    }

    private final Runnable mScanRunnable = new Runnable() {

        @Override
        public void run() {
            scanLeDevice(false);
        }
    };

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //单击连接设备
            scanLeDevice(false);
            LeDevice device = mLeDeviceListAdapter.getItem(position);
            mLeProxy.connect(device.getAddress(), false);
        }
    };

    private final OnItemLongClickListener mOnItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            //长按查看广播数据
            LeDevice device = mLeDeviceListAdapter.getItem(position);
            showAdvDetailsDialog(device);
            return true;
        }
    };

    //显示广播数据
    private void showAdvDetailsDialog(LeDevice device) {
        LeScanRecord record = device.getLeScanRecord();

        StringBuilder sb = new StringBuilder();
        sb.append(device.getAddress() + "\n\n");
        sb.append('[' + DataUtil.byteArrayToHex(record.getBytes()) + "]\n\n");
        sb.append(record.toString());

        TextView textView = new TextView(getActivity());
        textView.setPadding(32, 32, 32, 32);
        textView.setText(sb.toString());

        Dialog dialog = new Dialog(getActivity());
        dialog.setTitle(device.getName());
        dialog.setContentView(textView);
        dialog.show();
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        //private Map<String,LeDevice> mLeDevicesMap;
        private List<LeDevice> mLeDevices;
        private LayoutInflater mInflater;

        public LeDeviceListAdapter() {
            //mLeDevicesMap = new HashMap<>();
            mLeDevices = new ArrayList<>();
            mInflater = getActivity().getLayoutInflater();

        }

        public void addDevice(LeDevice device) {
            if (!mLeDevices.contains(device)) {
                device.setSign(sginDeviceAddress.contains(device.getAddress()));
                mLeDevices.add(device);
                Collections.sort(mLeDevices,new RssiComparator());
            }

            //mLeDevicesMap.put(address,device);
           // mLeDevices.clear();
            //mLeDevices.addAll(mLeDevicesMap.values());
        }

        public void clear() {
            mLeDevices.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public LeDevice getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            final ViewHolder viewHolder;

            if (view == null) {
                view = mInflater.inflate(R.layout.item_device_list, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.txt_rssi);
                viewHolder.bt_sign = (Button) view.findViewById(R.id.bt_sign);
                viewHolder.txt_issign = (TextView) view.findViewById(R.id.txt_issign);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final LeDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (!TextUtils.isEmpty(deviceName))
                //viewHolder.deviceName.setText(deviceName.substring(0,10));
                viewHolder.deviceName.setText(deviceName.substring(0, 6)+deviceName.substring(deviceName.length()-1-3, deviceName.length()));
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRssi.setText("rssi: " + device.getRssi() + "dbm");

            viewHolder.bt_sign.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    device.setSign(true);
                    sginDeviceAddress.add(device.getAddress());
                    saveSignDeviceAddressToSP(sginDeviceAddress);

                    viewHolder.txt_issign.setVisibility(View.VISIBLE);
                    viewHolder.bt_sign.setVisibility(View.GONE);
                }
            });

            if (device.isSign()){
                viewHolder.txt_issign.setVisibility(View.VISIBLE);
                viewHolder.bt_sign.setVisibility(View.GONE);
            }
            else {
                viewHolder.txt_issign.setVisibility(View.GONE);
                viewHolder.bt_sign.setVisibility(View.VISIBLE);
            }

            return view;
        }
    }

    private void saveSignDeviceAddressToSP(Set<String> sginDeviceAddress) {
        Log.i(TAG,"sginDeviceAddress:"+sginDeviceAddress);
        clearSP();
        mSp.edit().putStringSet("sginDeviceAddress",sginDeviceAddress).apply();

        Set<String> signDeviceAddressFromSP = getSignDeviceAddressFromSP();
        Log.i(TAG,"signDeviceAddressFromSP:"+signDeviceAddressFromSP);
    }

    private Set<String> getSignDeviceAddressFromSP() {
        return mSp.getStringSet("sginDeviceAddress",null);
    }

    private Set<String> sginDeviceAddress = new HashSet<>();

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.i(TAG,"onLeScan:"+device);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!TextUtils.isEmpty(device.getName()) && device.getName().length()<25 && device.getName().startsWith("AMSU")){
                        mLeDeviceListAdapter.addDevice(new LeDevice(device.getName(), device.getAddress(), rssi, scanRecord));
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }

                    /*mLeDeviceListAdapter.addDevice(new LeDevice(device.getName(), device.getAddress(), rssi, scanRecord));
                    mLeDeviceListAdapter.notifyDataSetChanged();*/
                }
            });
        }
    };

    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        TextView txt_issign;
        Button bt_sign;
    }

    private class RssiComparator implements Comparator<LeDevice> {
        @Override
        public int compare(LeDevice o1, LeDevice o2) {
            if (o2.isSign() &&  !o1.isSign()) {
                return -1;
            }
            else if (o2.isSign() && o1.isSign()){
                return o2.getRssi()-(o1.getRssi());
            }
            else if (!o2.isSign() && !o1.isSign()){
                return o2.getRssi()-(o1.getRssi());
            }
            else {
                return 1;
            }
        }
    }

}