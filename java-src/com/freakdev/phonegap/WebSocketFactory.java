package com.freakdev.phonegap;

import java.net.URISyntaxException;
import android.webkit.WebView;

public class WebSocketFactory {

	WebView mView;
	
	public WebSocketFactory(WebView view)
	{
		mView = view;
	}
	
	public WebSocket getNew(String url) throws URISyntaxException {
		return new GapWebSocket(mView, url);
	}
	
}
