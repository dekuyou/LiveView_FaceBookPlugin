package jp.ddo.dekuyou.liveview.plugins.facebook;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.facebook.android.Facebook;

public class FaceBookBean {
	
	
	  private static FaceBookBean instance = new FaceBookBean();

	  private FaceBookBean() {}

	  public static FaceBookBean getInstance() {
	    return instance;
	  }
	
	private int no = 0;
	private int page = 1;
	private Bitmap bitmap = null;
	private int msgrow = 0;
	private TL nowMode = TL.NEWSFEED;
	
	
	private String modeString = "";
	private String screenNameString = "";
	private String msgString = "";
	private String msgtime = "";
	
	private String idString = "";
	
	public String getIdString() {
		return idString;
	}

	public void setIdString(String idString) {
		this.idString = idString;
	}

	private Canvas canvas;
	
	private LinkedHashMap<String, Bitmap > profileImg = new LinkedHashMap<String, Bitmap >();
	
	
	private Bitmap drawBitmap;

	
	private Facebook fb ;
	private String statuses = "";
	private JSONArray data ;
	private JSONObject paging ;
	
	


	public JSONArray getData() {
		return data;
	}

	public void setData(JSONArray data) {
		this.data = data;
	}

	public JSONObject getPaging() {
		return paging;
	}

	public void setPaging(JSONObject paging) {
		this.paging = paging;
	}

	public String getStatuses() {
		return statuses;
	}

	public void setStatuses(String statuses) throws JSONException {
		this.statuses = statuses;
		
		
		if (statuses != null) {
			JSONObject jo1 = new JSONObject(statuses);

			this.data = jo1.getJSONArray("data");
			this.paging = jo1.getJSONObject("paging");
		}
	}

	public Facebook getFb() {
		return fb;
	}

	public void setFb(Facebook fb) {
		this.fb = fb;
	}

	public LinkedHashMap<String, Bitmap> getProfileImg() {
		return profileImg;
	}

	public void setProfileImg(LinkedHashMap<String, Bitmap> profileImg) {
		this.profileImg = profileImg;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	private 		BigDecimal fonthight;

	public BigDecimal getFonthight() {
		return fonthight;
	}

	public void setFonthight(BigDecimal fonthight) {
		this.fonthight = fonthight;
	}

	public String getModeString() {
		return modeString;
	}

	public void setModeString(String modeString) {
		this.modeString = modeString;
	}

	public String getScreenNameString() {
		return screenNameString;
	}

	public void setScreenNameString(String screenNameString) {
		this.screenNameString = screenNameString;
	}

	public String getMsgString() {
		return msgString;
	}

	public void setMsgString(String msgString) {
		this.msgString = msgString;
	}

	public String getMsgtime() {
		return msgtime;
	}

	public void setMsgtime(String msgtime) {
		this.msgtime = msgtime;
	}


	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}


	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public int getMsgrow() {
		return msgrow;
	}

	public void setMsgrow(int msgrow) {
		this.msgrow = msgrow;
	}

	public TL getNowMode() {
		return nowMode;
	}

	public void setNowMode(TL nowMode) {
		this.nowMode = nowMode;
	}

	public Bitmap getDrawBitmap() {
		return drawBitmap;
	}

	public void setDrawBitmap(Bitmap drawBitmap) {
		this.drawBitmap = drawBitmap;
	}






	
	
}
