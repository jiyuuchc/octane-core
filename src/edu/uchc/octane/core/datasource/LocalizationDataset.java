package edu.uchc.octane.core.datasource;

import java.util.ArrayList;

public class LocalizationDataset {

	ArrayList<ArrayList<Localization>> data;
	int cnt = 0;

	public LocalizationDataset() {
		data = new ArrayList<ArrayList<Localization>>();
	}

//	public void addLocalization(Localization d) {
//
//		if (data.get(d.frame - 1) == null) {
//			data.add(d.frame - 1, new ArrayList<Localization>());
//		}
//
//		ArrayList<Localization> list = data.get(d.frame - 1);
//		list.add(d);
//
//		cnt++;
//	}

	public int getMaxFrame() {
		return data.size();
	}

	public int getNumberOfLocalizaitonsAtFrame(int f) { // 1-based frame number

		if (data.get(f-1) == null) {
			return 0;
		} else {
			return data.get(f-1).size();
		}
	}

	public int getTotalNumberOfLocalizations() {
		return cnt;
	}

	public Localization[] getAllLocalizationsAtFrame(int f) {
		return (Localization[]) data.get(f-1).toArray();
	}

	public Localization[][] toArrays() {

		Localization [][] arr = new Localization[getMaxFrame()][];

		for (int i = 0; i < arr.length; i++) {
			arr[i] = getAllLocalizationsAtFrame(i+1);
		}

		return arr;
	}
}
