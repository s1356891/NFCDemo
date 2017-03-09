

package com.workspace.bin.nfcdemo;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.nfc.tech.IsoDep;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;

public class PbocCard {
	protected final static byte[] DFI_EP = { (byte) 0x10, (byte) 0x01 };

	protected final static byte[] DFN_PSE = { (byte) '1', (byte) 'P',
			(byte) 'A', (byte) 'Y', (byte) '.', (byte) 'S', (byte) 'Y',
			(byte) 'S', (byte) '.', (byte) 'D', (byte) 'D', (byte) 'F',
			(byte) '0', (byte) '1', };

	protected final static byte[] DFN_PXX = { (byte) 'P' };

	protected final static int MAX_LOG = 10;
	protected final static int SFI_LOG = 24;

	protected final static byte TRANS_CSU = 6;
	protected final static byte TRANS_CSU_CPX = 9;

	protected String name;
	protected String id;
	protected String serl;
	protected String version;
	protected String date;
	protected String count;
	protected String cash;
	protected String log;

	public static String load(IsoDep tech, Resources res) {
		final Iso7816.Tag tag = new Iso7816.Tag(tech);
		tag.connect();
		PbocCard card = null;
		do {
			if ((card = BeijingMunicipal.load(tag, res)) != null)
				break;
		} while (false);
		tag.close();
		return (card != null) ? card.toString(res) : null;
	}

	protected PbocCard(Iso7816.Tag tag) {
		id = tag.getID().toString();
	}


	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	protected static boolean addLog(final Iso7816.Response r,
									ArrayList<byte[]> l) {
		if (!r.isOkey())
			return false;

		final byte[] raw = r.getBytes();
		final int N = raw.length - 23;
		if (N < 0)
			return false;

		for (int s = 0, e = 0; s <= N; s = e) {
			l.add(Arrays.copyOfRange(raw, s, (e = s + 23)));
		}

		return true;
	}

	protected static ArrayList<byte[]> readLog(Iso7816.Tag tag, int sfi) {
		final ArrayList<byte[]> ret = new ArrayList<byte[]>(MAX_LOG);
		final Iso7816.Response rsp = tag.readRecord(sfi);
		if (rsp.isOkey()) {
			addLog(rsp, ret);
		} else {
			for (int i = 1; i <= MAX_LOG; ++i) {
				if (!addLog(tag.readRecord(sfi, i), ret))
					break;
			}
		}

		return ret;
	}

	protected void parseLog(ArrayList<byte[]>... logs) {
		final StringBuilder r = new StringBuilder();

		for (final ArrayList<byte[]> log : logs) {
			if (log == null)
				continue;

			if (r.length() > 0)
				r.append("<br />--------------");

			for (final byte[] v : log) {
				final int cash = Util.toInt(v, 5, 4);
				if (cash > 0) {
					r.append("<br />").append(
							String.format("%02X%02X.%02X.%02X %02X:%02X ",
									v[16], v[17], v[18], v[19], v[20], v[21],
									v[22]));

					final char t = (v[9] == TRANS_CSU || v[9] == TRANS_CSU_CPX) ? '-'
							: '+';

					r.append(t).append(Util.toAmountString(cash / 100.0f));

					final int over = Util.toInt(v, 2, 3);
					if (over > 0)
						r.append(" [o:")
								.append(Util.toAmountString(over / 100.0f))
								.append(']');

					r.append(" [").append(Util.toHexString(v, 10, 6))
							.append(']');
				}
			}
		}

		this.log = r.toString();
	}

	protected void parseBalance(Iso7816.Response data) {
		if (!data.isOkey() || data.size() < 4) {
			cash = null;
			return;
		}

		int n = Util.toInt(data.getBytes(), 0, 4);
		if (n > 100000 || n < -100000)
			n -= 0x80000000;

		cash = Util.toAmountString(n / 100.0f);
	}

	protected String formatInfo(Resources res) {
		if (serl == null)
			return null;

		final StringBuilder r = new StringBuilder();

		r.append(res.getString(R.string.lab_serl)).append(' ').append(serl);

		if (version != null) {
			final String sv = res.getString(R.string.lab_ver);
			r.append("<br />").append(sv).append(' ').append(version);
		}

		if (date != null) {
			final String sd = res.getString(R.string.lab_date);
			r.append("<br />").append(sd).append(' ').append(date);
		}

		if (count != null) {
			final String so = res.getString(R.string.lab_op);
			final String st = res.getString(R.string.lab_op_time);
			r.append("<br />").append(so).append(' ').append(count).append(st);
		}

		return r.toString();
	}

	protected String formatLog(Resources res) {
		if (log == null || log.length() < 1)
			return null;

		final StringBuilder ret = new StringBuilder();
		final String sl = res.getString(R.string.lab_log);
		ret.append("<b>").append(sl).append("</b><small>");
		ret.append(log).append("</small>");

		return ret.toString();
	}

	protected String formatBalance(Resources res) {
		if (cash == null || cash.length() < 1)
			return null;

		final String s = res.getString(R.string.lab_balance);
		final String c = res.getString(R.string.lab_cur_cny);
		return new StringBuilder("<b>").append(s)
				.append("<font color=\"teal\"> ").append(cash).append(' ')
				.append(c).append("</font></b>").toString();
	}

	protected String toString(Resources res) {
		final String info = formatInfo(res);
		final String hist = formatLog(res);
		final String cash = formatBalance(res);

		return CardManager.buildResult(name, info, cash, hist);
	}
}
