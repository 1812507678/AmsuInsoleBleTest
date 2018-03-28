package com.amsu.amsuinsolebletest.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class HandlerUtil {
	public static final int MSG_DISCONNECTED = 3;
	public static final int MSG_UPDATE_RSSI = 4;
	public static final int MSG_RX_DATA = 5;
	public static final int MSG_RX_REG_DATA = 6;// 寄存器数据
	public static final int MSG_RX_DEVICE_NAME = 7;

	public static final String EXTRA_MAC = "extra_mac";
	public static final String EXTRA_NAME = "extra_name";
	public static final String EXTRA_RSSI = "extra_rssi";
	public static final String EXTRA_DATA = "extra_data";
	public static final String EXTRA_REG_FLAG = "extra_reg_flag";
	public static final String EXTRA_REG_DATA = "extra_reg_data";

	// OAD
	public static final int MSG_OAD_IMAGE_TYPE = 8;
	public static final int MSG_OAD_PREPARED = 9;
	public static final int MSG_OAD_PROGRESS_CHANGED = 10;
	public static final int MSG_OAD_INTERRUPT = 11;
	public static final int MSG_OAD_FINISH = 12;

	public static final String EXTRA_IMAGE_TYPE = "extra_image_type";
	public static final String EXTRA_I_BYTES = "extra_i_bytes";
	public static final String EXTRA_N_BYTES = "extra_n_bytes";
	public static final String EXTRA_MILLISECONDS = "extra_milliseconds";

	public static void handleMsg(Handler mHandler, int what, Bundle data){
		Message msg = new Message();
		msg.what = what;
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
}