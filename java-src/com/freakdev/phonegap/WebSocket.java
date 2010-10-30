package com.freakdev.phonegap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * An implementation of a WebSocket protocol client. designed for being used in Android platform.
 * @author freakdev
 */
public class WebSocket extends com.sixfire.WebSocket {

	public final static int CONNECTING = 0; // The connection has not yet been established.
	public final static int OPEN = 1; // The WebSocket connection is established and communication is possible.
    public final static int CLOSING = 2; // The connection is going through the closing handshake.
    public final static int CLOSED = 3; // The connection has been closed or could not be opened.
    
    public int readyState = 0; // according to w3c specifications, should be "read-only"
    
    Thread connectThread = new Thread(new ConnectRunnable());
    
    protected Handler _messageHandler = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		onmessage((String) msg.obj);
    	}
    	
    };
	
    /**
     * as defined in the specification new object will automatically try to connect
     * @param url
     * @throws URISyntaxException
     */
	public WebSocket(String url) throws URISyntaxException {
		super(new URI(url));
		
		connectThread.start();
	}
	
	
	// event methods
	// these methods are called when an event is raised you should overrides their behavior to match your need
	
	protected void onopen() {
	}
	
	protected void onmessage(String data) {
	}
	
	protected void onerror() {
	}
	
	protected void onclose() {
	}
	
	
	@Override
	public void close() throws IOException
	{
		if (WebSocket.CLOSING == this.readyState || WebSocket.CLOSED == this.readyState) {
			this.readyState = WebSocket.CLOSING;
			super.close();
			this.readyState = WebSocket.CLOSED;
		}
	}
	
	@Override
	public void send(String data)
	{
		if (WebSocket.OPEN == this.readyState) {
			try {
				super.send(data);
			} catch (IOException e) {
				Log.w("WebSocket", "[send] " + e.getMessage());
				this.onerror();
			}
		} else {
			// throw invalid state exception
		}
	}

	
	protected void WaitForDataLoop () {
		
		Log.i("Thread Info", Thread.currentThread().getName()); 
		try {
			Log.i("WebSocket", "waiting for data");
			while (WebSocket.CLOSING > readyState) {
				String response = recv();
				_messageHandler.sendMessage(_messageHandler.obtainMessage(1, response));
			}
		} catch (IOException e) {
			if (WebSocket.CLOSING > readyState) {
				Log.w("WebSocket", "[WaitForDataLoop] " + e.getMessage());
				onerror();
			}
		}
		
	}		
	
	private class ConnectRunnable implements Runnable {

		@Override
		public void run() {
			Log.i("Thread Info", Thread.currentThread().getName());
			try {
				if (WebSocket.OPEN != readyState) {
					readyState = WebSocket.CONNECTING; 
					connect();
					readyState = WebSocket.OPEN;
					onopen();
					Log.i("WebSocket", "status Connecté");
					
					WaitForDataLoop();
				}
				
			} catch (IOException e) {
				Log.w("WebSocket", "[Connect.run] " + e.getMessage());
				
				try {
					close();
				} catch (IOException e1) {
					Log.w("WebSocket", "[Connect.run | connection fallback] " + e.getMessage());
				}
			}
			
		}
		
	}

}
