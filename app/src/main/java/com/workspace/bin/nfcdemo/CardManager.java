

package com.workspace.bin.nfcdemo;

import android.content.IntentFilter;
import android.content.res.Resources;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Parcelable;
import android.util.Log;

public final class CardManager {
	private static final String SP = "<br />------------------------------</b><br />";
	public static String[][] TECHLISTS;
	public static IntentFilter[] FILTERS;

	static {
		try {
			TECHLISTS = new String[][] { { IsoDep.class.getName() },
					{ NfcV.class.getName() }, { NfcF.class.getName() }, };
			FILTERS = new IntentFilter[] { new IntentFilter(
					NfcAdapter.ACTION_TECH_DISCOVERED, "*/*") };
		} catch (Exception e) {
		}
	}

	public static String buildResult(String n, String i, String d, String x) {
		if (n == null)
			return null;

		final StringBuilder s = new StringBuilder();
		s.append(n);

		if (d != null)
			s.append(SP).append(d);

		if (x != null)
			s.append(SP).append(x);
		
		if (i != null)
			s.append(SP).append(i);
		
		return s.toString();
	}

	public static String load(Parcelable parcelable, Resources res) {
		final Tag tag = (Tag) parcelable;

		final IsoDep isodep = IsoDep.get(tag);

		if (isodep != null) {
			return PbocCard.load(isodep, res);
		}

		return null;
	}
}
