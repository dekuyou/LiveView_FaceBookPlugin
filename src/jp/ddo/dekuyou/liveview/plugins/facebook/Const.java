package jp.ddo.dekuyou.liveview.plugins.facebook;

public class Const extends Key{
	
	public static final String[] PERMISSIONS = new String[] {"publish_stream", 
        "read_stream", "offline_access"};
	
	public static final int ROWS = 10;

	
//	graphPath
	public static final String PATH_FEED = "me/home";
	
	
	public static final int TITE_DELAY = 20;
	public static final int DRAW_MSG_DELAY = 5*100;
	public static final int DRAW_MSG_DELAY2 = 10*100; 
	public static final int DRAW_MSG_DELAY3 = 15*100; 
	public static final int DRAW_MSG_DELAY4 = 20*100; 
	public static final int DRAW_ICON_DELAY = 25*100;

}

enum TL {
	NEWSFEED;
}





