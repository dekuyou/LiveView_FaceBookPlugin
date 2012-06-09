package jp.ddo.dekuyou.liveview.plugins.facebook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.SessionStore;
import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;
import com.sonyericsson.extras.liveview.plugins.PluginUtils;

public class FaceBookPluginService extends AbstractPluginService {

	// Our handler.
	private Handler mHandler = null;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Create handler.
		if (mHandler == null) {
			mHandler = new Handler();
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopWork();
	}

	boolean sandbox = true;

	/**
	 * Plugin is sandbox.
	 */
	protected boolean isSandboxPlugin() {
		return sandbox;
	}

	// Is loop running?
	private boolean mWorkerRunning = false;
	// Preferences - update interval
	// private static final String UPDATE_INTERVAL = "updateInterval";

	private long mUpdateInterval = 1 * 1000;
	// Counter
	private int mCounter = 1;
	/**
	 * The runnable used for posting to handler
	 */
	private Runnable mAnnouncer = new Runnable() {

		@Override
		public void run() {
			Log.d("jp.ddo.dekuyou.liveview.plugins.twittertl2", "mAnnouncer");
			try {
				sendAnnounce("Hello", "Hello world number " + mCounter++);
			} catch (Exception re) {
				Log.e(PluginConstants.LOG_TAG,
						"Failed to send image to LiveView.", re);
			}

			scheduleTimer();
		}

	};

	private void sendAnnounce(String header, String body) {
		Log.d(this.getPackageName(), body);
		try {
			if (mWorkerRunning && (mLiveViewAdapter != null)
					&& mSharedPreferences.getBoolean("NEWSFEEDEnabled", true)) {
				mLiveViewAdapter.sendAnnounce(mPluginId, mMenuIcon, header,
						body, System.currentTimeMillis(),
						"http://en.wikipedia.org/wiki/Hello_world_program");
				Log.d(this.getPackageName(), "Announce sent to LiveView");
			} else {
				Log.d(this.getPackageName(), "LiveView not reachable");
			}
		} catch (Exception e) {
			Log.e(this.getPackageName(), "Failed to send announce", e);
		}
	}

	/**
	 * Schedules a timer.
	 */
	private void scheduleTimer() {
		Log.d(this.getPackageName(), String.valueOf(mWorkerRunning));
		if (mWorkerRunning) {
			mHandler.postDelayed(mAnnouncer, mUpdateInterval);

		}
	}

	/**
	 * Must be implemented. Starts plugin work, if any.
	 */
	protected void startWork() {

		FaceBookBean bean = FaceBookBean.getInstance();

		if (bean.getFb() == null) {
			bean.setFb(new Facebook(Const.API_KEY));

			SessionStore.restore(bean.getFb(), this);
		}

		mLiveViewAdapter.screenOn(mPluginId);
		PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId, "LiveFBview!",
				128, 10);

		try {

			if (!bean.getFb().isSessionValid()) {
				throw new FileNotFoundException();
			}

			if (bean.getStatuses() == null && TL.NEWSFEED.equals(bean
					.getNowMode())){
				bean.setStatuses(null);
				bean.setNo(0);

				getTimeline();
			}

			bean.setMsgrow(0);
			makeMsgBitmap();

			doDraw();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();

			// TODO Auto-generated catch block
			mHandler.postDelayed(new Runnable() {
				public void run() {
					// First message to LiveView
					try {
						mLiveViewAdapter.clearDisplay(mPluginId);
					} catch (Exception e) {
						Log.e(PluginConstants.LOG_TAG,
								"Failed to clear display.");
					}
					PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId,
							"Please do OAuth!", 128, 10);
				}
			}, Const.TITE_DELAY);

		}

	}

	private void getTimeline() throws MalformedURLException, JSONException,
			IOException {

		mHandler.postDelayed(new Runnable() {
			public void run() { //
				try {
					mLiveViewAdapter.vibrateControl(mPluginId, 0, 50);
				} catch (Exception e) {
					Log.e(PluginConstants.LOG_TAG, "Failed to Change TL.");
				}
			}
		}, Const.TITE_DELAY);

		FaceBookBean bean = FaceBookBean.getInstance();

		switch (bean.getNowMode()) {
		case NEWSFEED:
			bean.setStatuses(bean.getFb().request(Const.PATH_FEED));

			break;
		// case WALL:
		// if (bean.getStatuses() == null) {
		// bean.setStatuses(bean.getTwitter().getMentions(paging));
		//
		// } else {
		// bean.getStatuses()
		// .addAll(bean.getTwitter().getMentions(paging));
		// }
		// break;
		//	
		default:
			bean.setNowMode(TL.NEWSFEED);
			bean.setStatuses(bean.getFb().request(Const.PATH_FEED));
			break;
		}

	}

	//
	Integer slugId = null;

	private void doDraw() {
		FaceBookBean bean = FaceBookBean.getInstance();
		Bitmap drawBitmap = null;
		try {

			Log.d(this.getPackageName(), "bitmap.getHeight():   "
					+ bean.getBitmap().getHeight());
			Log.d(this.getPackageName(), "bitmap.getRowBytes(): "
					+ bean.getBitmap().getRowBytes());
			Log.d(this.getPackageName(), "bitmap.getDensity(): "
					+ bean.getBitmap().getDensity());

			int scrollpixel = 16;

			if (bean.getBitmap().getHeight() < scrollpixel
					* (bean.getMsgrow() + 1) + 128
					&& bean.getMsgrow() > 0) {
				bean.setMsgrow(bean.getMsgrow() - 1);
				return;

			}
			Log.d(this.getPackageName(), "msgrow: " + bean.getMsgrow());

			drawBitmap = Bitmap.createBitmap(bean.getBitmap(), 0, scrollpixel
					* bean.getMsgrow(), 128, 128);

			bean.setDrawBitmap(drawBitmap);

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}

		mHandler.postDelayed(new Runnable() {
			public void run() { //
				try {
					FaceBookBean bean = FaceBookBean.getInstance();
					Bitmap sendBitmap1 = Bitmap.createBitmap(bean
							.getDrawBitmap(), 0, 0, 128, 32);

					mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 0,
							sendBitmap1);

					sendBitmap1.recycle();

				} catch (Exception e) {
					Log.e(PluginConstants.LOG_TAG, "Failed to drowBitmap .");
				}

			}
		}, Const.DRAW_MSG_DELAY);

		mHandler.postDelayed(new Runnable() {
			public void run() { //
				try {
					FaceBookBean bean = FaceBookBean.getInstance();
					Bitmap sendBitmap2 = Bitmap.createBitmap(bean
							.getDrawBitmap(), 0, 32, 128, 32);

					mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 32,
							sendBitmap2);

					sendBitmap2.recycle();

				} catch (Exception e) {
					Log.e(PluginConstants.LOG_TAG, "Failed to drowBitmap .");
				}

			}
		}, Const.DRAW_MSG_DELAY2);
		mHandler.postDelayed(new Runnable() {
			public void run() { //
				try {
					FaceBookBean bean = FaceBookBean.getInstance();
					Bitmap sendBitmap2 = Bitmap.createBitmap(bean
							.getDrawBitmap(), 0, 64, 128, 32);

					mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 64,
							sendBitmap2);

					sendBitmap2.recycle();

				} catch (Exception e) {
					Log.e(PluginConstants.LOG_TAG, "Failed to drowBitmap .");
				}

			}
		}, Const.DRAW_MSG_DELAY3);
		mHandler.postDelayed(new Runnable() {
			public void run() { //
				try {
					FaceBookBean bean = FaceBookBean.getInstance();
					Bitmap sendBitmap2 = Bitmap.createBitmap(bean
							.getDrawBitmap(), 0, 96, 128, 32);

					mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 96,
							sendBitmap2);

					sendBitmap2.recycle();

				} catch (Exception e) {
					Log.e(PluginConstants.LOG_TAG, "Failed to drowBitmap .");
				}

			}
		}, Const.DRAW_MSG_DELAY4);

		mHandler.postDelayed(new Runnable() {
			public void run() { //
				mLiveViewAdapter.screenOnAuto(mPluginId);

			}
		}, Const.TITE_DELAY);

	}

	private void makeMsgBitmap() throws IllegalStateException, JSONException,
			ParseException {
		FaceBookBean bean = FaceBookBean.getInstance();

		String modeString = "";
		String screenNameString = "";
		String msgString = "";
		String msgtime = "";
		String idString = "";

		boolean isRt = false;
		switch (bean.getNowMode()) {
		case NEWSFEED:
			if (bean.getStatuses() == null || "".equals(bean.getStatuses())) {
				return;
			}
			JSONObject jo = bean.getData().getJSONObject(bean.getNo());

			modeString = "NEWS_FEED";

			if (!jo.isNull("from")) {
				JSONObject fromObj = new JSONObject(jo.getString("from"));

				if (!fromObj.isNull("name"))
					screenNameString = fromObj.getString("name");

				if (!fromObj.isNull("id"))
					idString = fromObj.getString("id");

			}
			if (!jo.isNull("message"))
				msgString += jo.getString("message") + "\n";

			if (!jo.isNull("name"))
				msgString += jo.getString("name") + "\n";
			if (!jo.isNull("description"))
				msgString += jo.getString("description") + "\n";

			if (!jo.isNull("updated_time")) {
				// SimpleDateFormat sd = new SimpleDateFormat();
				// Date date = sd.parse(jo.getString("updated_time"));
				msgtime = jo.getString("updated_time");
			}
			break;

		// case WALL:
		// if (bean.getStatuses() == null || bean.getStatuses().size() == 0) {
		// return;
		// }
		// // modeString = "@"+twitter.getScreenName();
		// modeString = "@Mt";
		// screenNameString = bean.getStatuses().get(bean.getNo()).getUser()
		// .getScreenName();
		// msgString = bean.getStatuses().get(bean.getNo()).getText();
		// msgtime = bean.getStatuses().get(bean.getNo()).getCreatedAt()
		// .toLocaleString();
		// user = bean.getStatuses().get(bean.getNo()).getUser();
		// break;

		default:
			break;
		}

		// Msg String
		String msg = msgtime + "\n" + msgString;

		Log.d(this.getPackageName(),
				"mSharedPreferences.getString(FontSize, 12): "
						+ mSharedPreferences.getString("FontSize", "12"));

		// Set the text properties in the canvas
		TextPaint textPaint = new TextPaint();
		textPaint.setTextSize(new Integer(mSharedPreferences.getString(
				"FontSize", "10")));

		textPaint.setColor(Color.WHITE);

		// Create the text layout and draw it to the canvas
		StaticLayout textLayout = new StaticLayout(msg, textPaint, 128,
				Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

		Log.d(this.getPackageName(), "textLayout.getLineCount(): "
				+ textLayout.getLineCount());
		Log.d(this.getPackageName(), "textPaint.getFontSpacing(): "
				+ textPaint.getFontSpacing());

		BigDecimal bitmaphight = new BigDecimal("128");
		BigDecimal fonthight = new BigDecimal(new Double(textPaint
				.getFontSpacing()).toString()).setScale(0, BigDecimal.ROUND_UP);
		BigDecimal linehight = new BigDecimal(new Long(textLayout
				.getLineCount() + 2).toString()).multiply(fonthight);
		Log.d(this.getPackageName(),
				"textLayout.getLineCount() * textPaint.getFontSpacing() :"
						+ linehight.toString());
		BigDecimal bitmaprows = linehight.divide(bitmaphight, 0,
				BigDecimal.ROUND_UP);
		Log.d(this.getPackageName(), "bitmaprows :" + bitmaprows);

		Bitmap msgbitmap = null;

		try {
			msgbitmap = Bitmap.createBitmap(128, bitmaprows.multiply(
					new BigDecimal("128")).intValue(), Bitmap.Config.RGB_565);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}

		Canvas canvas1 = new Canvas(msgbitmap);
		textLayout.draw(canvas1);

		Log.d(this.getPackageName(), "bitmap.getScaledHeight(canvas): "
				+ msgbitmap.getScaledHeight(canvas1));

		// screen name

		String username = modeString + " "
				+ new Integer(bean.getNo()).toString() + " :"
				+ (isRt ? "RT" : "") + "\n" + screenNameString;

		StaticLayout textLayout2 = new StaticLayout(username, textPaint, 128,
				Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

		Bitmap usernamebitmap = null;
		try {
			usernamebitmap = Bitmap.createBitmap(128, fonthight.multiply(
					new BigDecimal("2")).intValue(), Bitmap.Config.RGB_565);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}

		Log.d(this.getPackageName(), "bitmaphights :" + msgbitmap.getHeight()
				+ usernamebitmap.getHeight());

		try {
			bean.setBitmap(Bitmap.createBitmap(128, msgbitmap.getHeight()
					+ usernamebitmap.getHeight(), Bitmap.Config.RGB_565));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}

		Canvas canvas = new Canvas(bean.getBitmap());

		Canvas canvas2 = new Canvas(usernamebitmap);
		textLayout2.draw(canvas2);

		BitmapDrawable drawable2 = new BitmapDrawable(usernamebitmap);
		drawable2
				.setBounds(
						fonthight.multiply(new BigDecimal("2")).intValue() + 1,
						0, usernamebitmap.getWidth()
								+ (fonthight.multiply(new BigDecimal("2"))
										.intValue() + 1), usernamebitmap
								.getHeight());
		drawable2.draw(canvas);

		BitmapDrawable drawable3 = new BitmapDrawable(msgbitmap);
		drawable3.setBounds(0, usernamebitmap.getHeight(),
				msgbitmap.getWidth(), msgbitmap.getHeight()
						+ usernamebitmap.getHeight());
		drawable3.draw(canvas);

		bean.setFonthight(fonthight);
		bean.setIdString(idString);

		msgbitmap.recycle();
		usernamebitmap.recycle();

		// icon
		if (bean.getProfileImg().containsKey(idString)) {

			BitmapDrawable drawable = new BitmapDrawable(bean.getProfileImg()
					.get(idString));
			drawable.setBounds(0, 0, bean.getFonthight().multiply(
					new BigDecimal("2")).intValue(), bean.getFonthight()
					.multiply(new BigDecimal("2")).intValue());
			drawable.draw(canvas);

		} else {

			mHandler.postDelayed(new Runnable() {
				public void run() {
					// First message to LiveView

					FaceBookBean bean = FaceBookBean.getInstance();

					Canvas canvas = new Canvas(bean.getBitmap());

					BitmapDrawable drawable = new BitmapDrawable(
							getProfileImage(bean.getIdString()));
					drawable.setBounds(0, 0, bean.getFonthight().multiply(
							new BigDecimal("2")).intValue(), bean
							.getFonthight().multiply(new BigDecimal("2"))
							.intValue());
					drawable.draw(canvas);

					if (bean.getMsgrow() == 0) {
						mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 0,
								Bitmap
										.createBitmap(bean.getBitmap(), 0, 0,
												bean.getFonthight().multiply(
														new BigDecimal("2"))
														.intValue(), bean
														.getFonthight()
														.multiply(
																new BigDecimal(
																		"2"))
														.intValue()));
					}

				}

				private Bitmap getProfileImage(String idString) {
					Bitmap icon = null;
					FaceBookBean bean = FaceBookBean.getInstance();

					if (bean.getProfileImg().containsKey(idString)) {
						return bean.getProfileImg().get(idString);

					}

					// get icon
					HttpURLConnection c = null;
					InputStream is = null;
					try {
						// HTTP接続のオープン
						// http://graph.facebook.com/dekuyou/picture
						URL url = new URL("http://graph.facebook.com/"
								+ idString + "/picture");
						c = (HttpURLConnection) url.openConnection();
						c.setRequestMethod("GET");
						c.setConnectTimeout(2 * 1000);
						c.setReadTimeout(5 * 1000);
						c.connect();
						is = c.getInputStream();

						icon = BitmapFactory.decodeStream(is);

						// HTTP接続のクローズ
						is.close();
						c.disconnect();

					} catch (Exception e) {
						try {
							if (c != null)
								c.disconnect();
							if (is != null)
								is.close();
						} catch (Exception e2) {
						}
					} finally {

					}

					if (bean.getProfileImg().size() >= 5 && icon != null) {
						Set<String> keySet = bean.getProfileImg().keySet(); // すべてのキー値を取得
						Iterator<String> keyIte = keySet.iterator();
						keyIte.hasNext();
						String key = (String) keyIte.next();

						bean.getProfileImg().remove(key);
					}

					if (icon != null) {

						bean.getProfileImg().put(idString, icon);
					}

					return icon;
				}

			}, Const.DRAW_ICON_DELAY);

		}

	}

	/**
	 * Must be implemented. Stops plugin work, if any.
	 */
	protected void stopWork() {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done connection and registering to the LiveView
	 * Service.
	 * 
	 * If needed, do additional actions here, e.g. starting any worker that is
	 * needed.
	 */
	protected void onServiceConnectedExtended(ComponentName className,
			IBinder service) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done disconnection from LiveView and service has been
	 * stopped.
	 * 
	 * Do any additional actions here.
	 */
	protected void onServiceDisconnectedExtended(ComponentName className) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has checked if plugin has been enabled/disabled.
	 * 
	 * The shared preferences has been changed. Take actions needed.
	 */
	protected void onSharedPreferenceChangedExtended(SharedPreferences prefs,
			String key) {

	}

	protected void startPlugin() {
		Log.d(PluginConstants.LOG_TAG, "startPlugin");

		sandbox = true;
		startWork();
	}

	protected void stopPlugin() {
		Log.d(PluginConstants.LOG_TAG, "stopPlugin");

		sandbox = false;

		stopWork();
	}

	protected void button(String buttonType, boolean doublepress,
			boolean longpress) {
		Log.d(PluginConstants.LOG_TAG, "button - type " + buttonType
				+ ", doublepress " + doublepress + ", longpress " + longpress);

		FaceBookBean bean = FaceBookBean.getInstance();
		// if (bean.getTwitter() == null) {
		// return;
		// }

		try {

			if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_UP)) {
				// UP Scroll
				if (bean.getMsgrow() > 0) {
					bean.setMsgrow(bean.getMsgrow() - 1);
					doDraw();
				}

			} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_DOWN)) {
				// DOWN Scroll

				bean.setMsgrow(bean.getMsgrow() + 1);
				doDraw();

			} else if (buttonType
					.equalsIgnoreCase(PluginConstants.BUTTON_RIGHT)) {
				// Msg OLD
				switch (bean.getNowMode()) {
				// case NEWSFEED:
				// if (bean.getDms().size() - 1 > bean.getNo()) {
				// bean.setNo(bean.getNo() + 1);
				// } else {
				// bean.setPage(bean.getPage() + 1);
				// bean.setPaging(new Paging(bean.getPage(), Const.ROWS));
				// // paging.setSinceId(dms.get(no).getId());
				// getTimeline(bean.getPaging());
				//
				// bean.setNo(bean.getNo() + 1);
				//
				// if (bean.getDms().size() - 1 <= bean.getNo()) {
				// bean.setNo(bean.getDms().size() - 1);
				//
				// }
				// // no = statuses.size() - 1;
				//
				// }
				// break;

				default:
					if (bean.getData().length() - 1 > bean.getNo()) {
						bean.setNo(bean.getNo() + 1);
					} else {
						// bean.setPage(bean.getPage() + 1);
						// bean.setPaging(new Paging(bean.getPage(),
						// Const.ROWS));
						// // paging.setMaxId(statuses.get(no).getId());
						// getTimeline(bean.getPaging());
						//
						// bean.setNo(bean.getNo() + 1);
						//
						// if (bean.getStatuses().size() - 1 <= bean.getNo()) {
						// bean.setNo(bean.getStatuses().size() - 1);
						//
						// }
						// // no = statuses.size() - 1;

					}
					break;
				}
				bean.setMsgrow(0);
				makeMsgBitmap();
				doDraw();

			} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_LEFT)) {
				// Msg NEW

				if (bean.getNo() > 0 && !longpress) {
					bean.setNo(bean.getNo() - 1);
				} else {
					bean.setNo(0);
					bean.setStatuses(null);
					bean.setPage(1);
					getTimeline();
				}
				bean.setMsgrow(0);
				makeMsgBitmap();

				doDraw();

				// } else if (buttonType
				// .equalsIgnoreCase(PluginConstants.BUTTON_SELECT)) {
				// // TLChange
				// mHandler.postDelayed(new Runnable() {
				// public void run() {
				// try {
				// mLiveViewAdapter.vibrateControl(mPluginId, 0, 50);
				// } catch (Exception e) {
				// Log.e(PluginConstants.LOG_TAG, "Failed to Change TL.");
				// }
				// }
				// }, 5);
				//				
				//				
				// bean.setStatuses(null);
				//
				// changeMode();
				//
				// bean.setPage(1);
				// bean.setPaging(new Paging(bean.getPage(), Const.ROWS));
				// bean.setNo(0);
				// getTimeline(bean.getPaging());
				// bean.setMsgrow(0);
				//
				// makeMsgBitmap();
				// doDraw();

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void changeMode() {
		FaceBookBean bean = FaceBookBean.getInstance();

		Log.d(this.getPackageName(), "changeMode");
	}

	protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
		Log.d(PluginConstants.LOG_TAG, "displayCaps - width " + displayWidthPx
				+ ", height " + displayHeigthPx);
	}

	protected void onUnregistered() throws RemoteException {
		Log.d(PluginConstants.LOG_TAG, "onUnregistered");
		stopWork();
	}

	protected void openInPhone(String openInPhoneAction) {
		Log.d(PluginConstants.LOG_TAG, "openInPhone: " + openInPhoneAction);
	}

	protected void screenMode(int mode) {
		Log.d(PluginConstants.LOG_TAG, "screenMode: screen is now "
				+ ((mode == 0) ? "OFF" : "ON"));

		if (mode == PluginConstants.LIVE_SCREEN_MODE_ON) {
		} else {
		}
	}

}