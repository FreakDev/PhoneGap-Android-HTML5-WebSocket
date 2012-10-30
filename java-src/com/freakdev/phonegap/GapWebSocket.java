package com.freakdev.phonegap;

import java.net.URISyntaxException;

import android.util.Log;
import android.webkit.WebView;

 public class GapWebSocket extends WebSocket{

	WebView mView;
	private final WebSocket instance;
	
	public GapWebSocket(WebView v, String url) throws URISyntaxException {
		super(url);
		mView = v;
		this.instance = this;
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
	protected void onmessage(final String data) {
		mView.post(new Runnable(){
			public void run() {
				mView.loadUrl("javascript:WebSocket.triggerEvent(" + JSEvent.buildJSON("message", instance.toString(), data) + ")");
			}
		});
	}

	@Override	
	protected void onopen() {
		try {
			mView.loadUrl("javascript:WebSocket.triggerEvent(" + JSEvent.buildJSON("open", this.toString()) + ")");
		} catch (Exception e) {
			Log.i("WebSocketException", e.toString());
			// TODO: handle exception
		}
	}
	
	@Override	
	protected void onerror() {
		try {
			mView.loadUrl("javascript:WebSocket.triggerEvent(" + JSEvent.buildJSON("error", this.toString()) + ")");
		} catch (Exception e) {
			Log.i("WebSocketException", e.toString());
			// TODO: handle exception
		}
	}

	@Override
	protected void onclose() {
		try {
			mView.loadUrl("javascript:WebSocket.triggerEvent(" + JSEvent.buildJSON("close", this.toString()) + ")");
		} catch (Exception e) {
			Log.i("WebSocketException", e.toString());
			// TODO: handle exception
		}
	}
	
	public String getIdentifier() {
		return this.toString();
	}

	public int getReadyState() {
		return this.readyState;
	}	
	
}
