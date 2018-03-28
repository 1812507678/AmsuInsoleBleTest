package com.amsu.amsuinsolebletest;

import com.ble.ble.LeScanRecord;

public class LeDevice {
	private final String address;
	private String name;
	private String rxData = "No data";
	private LeScanRecord mRecord;
	private int rssi;
	private int rec40ScendCount;
	private boolean oadSupported = false;
	private String recPackageCountString;
	private boolean isSign;

	public LeDevice(String name, String address) {
		this.name = name;
		this.address = address;
	}

	public LeDevice(String name, String address, int rssi, byte[] scanRecord) {
		this.name = name;
		this.address = address;
		this.rssi = rssi;
		this.mRecord = LeScanRecord.parseFromBytes(scanRecord);
	}

	public boolean isOadSupported() {
		return oadSupported;
	}

	public void setOadSupported(boolean oadSupported) {
		this.oadSupported = oadSupported;
	}

	public LeScanRecord getLeScanRecord() {
		return mRecord;
	}

	public int getRssi() {
		return rssi;
	}

	public void setRssi(int rssi) {
		this.rssi = rssi;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public String getRxData() {
		return rxData;
	}

	public void setRxData(String rxData) {
		this.rxData = rxData;
	}



	public int getRec40ScendCount() {
		return rec40ScendCount;
	}

	public void setRec40ScendCount(int rec40ScendCount) {
		this.rec40ScendCount = rec40ScendCount;
	}

	public String getRecPackageCountString() {
		return recPackageCountString;
	}

	public void setRecPackageCountString(String recPackageCountString) {
		this.recPackageCountString = recPackageCountString;
	}

	public boolean isSign() {
		return isSign;
	}

	public void setSign(boolean sign) {
		isSign = sign;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LeDevice) {
			return ((LeDevice) o).getAddress().equals(address);
		}
		return false;
	}

	@Override
	public String toString() {
		return "LeDevice{" +
				"address='" + address + '\'' +
				", name='" + name + '\'' +
				", rxData='" + rxData + '\'' +
				", mRecord=" + mRecord +
				", rssi=" + rssi +
				", rec40ScendCount=" + rec40ScendCount +
				", oadSupported=" + oadSupported +
				", recPackageCountString='" + recPackageCountString + '\'' +
				", isSign=" + isSign +
				'}';
	}
}