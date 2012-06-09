package jp.ddo.dekuyou.liveview.plugins.facebook;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.SessionStore;

public class FaceBookOAuth extends Activity {

	Facebook facebook;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		facebook = new Facebook(Const.APP_KEY);

		SessionStore.restore(facebook, this);
		Log.d(this.getClass().getName(),
				String.valueOf(facebook.isSessionValid()));

		if (!facebook.isSessionValid()) {

			facebook.authorize(this, Const.PERMISSIONS,
					new LoginDialogListener());
		} else {
			Toast.makeText(this, "LiveFBview is ready.", Toast.LENGTH_LONG)
					.show();
			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Log.d(this.getClass().getName(), "onActivityResult :");

		facebook.authorizeCallback(requestCode, resultCode, data);

	}

	//
	// My asynchronous dialog listener
	//
	private final class LoginDialogListener implements
			com.facebook.android.Facebook.DialogListener {
		@Override
		public void onComplete(Bundle values) {

			Log.d(this.getClass().getName(),
					"LiveFBview Bundle:" + values.toString());
			SessionStore.save(facebook, FaceBookOAuth.this);

			endProcessing();
		}

		private void endProcessing() {
			Intent intent = new Intent();
			intent.setClassName("jp.ddo.dekuyou.liveview.plugins.facebook",
					"jp.ddo.dekuyou.liveview.plugins.facebook.FaceBookOAuth");
			startActivity(intent);
			finish();
		}

		@Override
		public void onFacebookError(FacebookError error) {

			Log.d(this.getClass().getName(), "LiveFBview FacebookError:"
					+ error.toString());
			endProcessing();


		}

		@Override
		public void onError(DialogError e) {

			Log.d(this.getClass().getName(),
					"LiveFBview DialogError:" + e.toString());
			endProcessing();


		}

		@Override
		public void onCancel() {

			Log.d(this.getClass().getName(), "LiveFBview Cancel:");
			finish();

		}

	}

}