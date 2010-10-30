package com.freakdev.phonegap;

import java.net.URISyntaxException;

import android.util.Log;
import android.webkit.WebView;

 public class GapWebSocket extends WebSocket{

	WebView mView;
	
	public GapWebSocket(WebView v, String url) throws URISyntaxException {
		super(url);
		mView = v;
	}
	
	protected static class JSEvent {
		static String buildJSON(String type, String target, String data) {
			Log.i("JSEvent", "{\"type\":\"" + type + "\",\"target\":\"" + target + "\",\"data\":'"+ data +"'}");
			return "{\"type\":\"" + type + "\",\"target\":\"" + target + "\",\"data\":'"+ data +"'}";
		}
		
		static String buildJSON(String type, String target) {
			return "{\"type\":\"" + type + "\",\"target\":\"" + target + "\",\"data\":\"\"}";
		}		
	}
	
	@Override
	protected void onmessage(String data) {
		mView.loadUrl("javascript:WebSocket.triggerEvent(" + JSEvent.buildJSON("message", this.toString(), data) + ")");
	}

	@Override	
	protected void onopen() {
		mView.loadUrl("javascript:WebSocket.triggerEvent(" + JSEvent.buildJSON("open", this.toString()) + ")");
	}
	
	@Override	
	protected void onerror() {
		mView.loadUrl("javascript:WebSocket.triggerEvent(" + JSEvent.buildJSON("error", this.toString()) + ")");
	}

	@Override
	protected void onclose() {
		mView.loadUrl("javascript:WebSocket.triggerEvent(" + JSEvent.buildJSON("close", this.toString()) + ")");		
	}
	
	public String getIdentifier() {
		return this.toString();
	}

	public int getReadyState() {
		return this.readyState;
	}	
	
}
