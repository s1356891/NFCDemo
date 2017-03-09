
package com.workspace.bin.nfcdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.widget.TextView;


public final class NFCard extends Activity{
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private Resources res;
	private TextView board;

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nfcard);
		board = (TextView) findViewById(R.id.board);
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		onNewIntent(getIntent());
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected void onPause() {
		super.onPause();

		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(this);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected void onResume() {
		super.onResume();

		if (nfcAdapter != null)
        nfcAdapter.enableForegroundDispatch(this, pendingIntent,
                CardManager.FILTERS, CardManager.TECHLISTS);

		refreshStatus();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final Parcelable p = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		showData((p != null) ? CardManager.load(p, res) : null);
	}



	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		refreshStatus();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void refreshStatus() {
		final Resources r = this.res;

		final String tip;
		if (nfcAdapter == null)
			tip = r.getString(R.string.tip_nfc_notfound);
		else if (nfcAdapter.isEnabled())
			tip = r.getString(R.string.tip_nfc_enabled);
		else
			tip = r.getString(R.string.tip_nfc_disabled);

		final StringBuilder s = new StringBuilder(
				r.getString(R.string.app_name));

		s.append("  --  ").append(tip);
		setTitle(s);
	}
	private void showData(String data) {
		if (data == null || data.length() == 0) {
			return;
		}
		final TextView board = this.board;
		final Resources res = this.res;
		final int padding = res.getDimensionPixelSize(R.dimen.pnl_margin);
		board.setPadding(padding, padding, padding, padding);
		board.setTextSize(res.getDimension(R.dimen.text_small));
		board.setText(Html.fromHtml(data));
	}





}
