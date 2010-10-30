package com.sixfire;
/**
 * The MIT License
 *
 * Copyright (c) 2009 Adam MacBeth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * An implementation of a WebSocket protocol client.
 */
public class WebSocket {

	/** The url. */
	private URI mUrl;
	
	/** The socket. */
	private Socket mSocket;
	
	/** Whether the handshake is complete. */
	private boolean mHandshakeComplete;
	
	/** The socket input stream. */
	private InputStream mInput;
	
	/** The socket mOutput stream. */
	private OutputStream mOutput;
	
	/** The external headers. */
	private HashMap<String, String> mHeaders; 
	
	/**
	 * Creates a new WebSocket targeting the specified URL.
	 * @param url The URL for the socket.
	 */
	public WebSocket(URI url) {
		mUrl = url;
		
		String protocol = mUrl.getScheme();
		if (!protocol.equals("ws") && !protocol.equals("wss")) {
			throw new IllegalArgumentException("Unsupported protocol: " + protocol);
		}
	}
	
	/**
	 * Sets extra headers to be sent.
	 * @param headers A hash of header name-values.
	 */
	public void setHeaders(HashMap<String, String> headers) {
		mHeaders = headers;
	}

	/**
	 * Returns the underlying socket;
	 */
	public Socket getSocket() {
		return mSocket;
	}
	
	/**
	 * Establishes the connection.
	 */
	public void connect() throws java.io.IOException {
		String host = mUrl.getHost();
		String path = mUrl.getPath();
		if (path.equals("")) {
			path = "/";
		}
		
		String query = mUrl.getQuery();
		if (query != null) {
			path = path + "?" + query;
		}
		
		String origin = "http://" + host;

		mSocket = createSocket();
		int port = mSocket.getPort();
		if (port != 80) {
			host = host + ":" + port;
		}
		
		mOutput = mSocket.getOutputStream();
		StringBuffer extraHeaders = new StringBuffer();
		if (mHeaders != null) {
			for (Entry<String, String> entry : mHeaders.entrySet()) {
				extraHeaders.append(entry.getKey() + ": " + entry.getValue() + "\r\n");				
			}
		}
		
		String request = "GET "+path+" HTTP/1.1\r\n" +
		         	     "Upgrade: WebSocket\r\n" +
		         	     "Connection: Upgrade\r\n" +
		         	     "Host: "+host+"\r\n" +
		         	     "Origin: "+origin+"\r\n" +
		         	     extraHeaders.toString() +
		         	     "\r\n";
		mOutput.write(request.getBytes());
		mOutput.flush();
		
		mInput = mSocket.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(mInput));
		String header = reader.readLine();
		if (!header.equals("HTTP/1.1 101 Web Socket Protocol Handshake")) {
			throw new IOException("Invalid handshake response");
		}
		
		header = reader.readLine();
		if (!header.equals("Upgrade: WebSocket")) {
			throw new IOException("Invalid handshake response");
		}
		
		header = reader.readLine();
		if (!header.equals("Connection: Upgrade")) {
			throw new IOException("Invalid handshake response");
		}
		
		do {
			header = reader.readLine();
		} while (!header.equals(""));
		
		mHandshakeComplete = true;
	}
	
	private Socket createSocket() throws java.io.IOException {
		String scheme = mUrl.getScheme();
		String host = mUrl.getHost();
				
		int port = mUrl.getPort();
		if (port == -1) {
			if (scheme.equals("wss")) {
				port = 443;
			} else if (scheme.equals("ws")) {
				port = 80;
			} else {
				throw new IllegalArgumentException("Unsupported scheme");
			}
		}
		
		if (scheme.equals("wss")) {
			SocketFactory factory = SSLSocketFactory.getDefault();
			return factory.createSocket(host, port);
		 } else {
			 return new Socket(host, port);
		 }
	}
	
	/**
	 * Sends the specified string as a data frame.
	 * @param str The string to send.
	 * @throws java.io.IOException
	 */
	public void send(String str) throws java.io.IOException {
		if (!mHandshakeComplete) {
			throw new IllegalStateException("Handshake not complete");
		}
		
		mOutput.write(0x00);
		mOutput.write(str.getBytes("UTF-8"));
		mOutput.write(0xff);
		mOutput.flush();
	}

	/**
	 * Receives the next data frame.
	 * @return The received data.
	 * @throws java.io.IOException
	 */
	public String recv() throws java.io.IOException {
		if (!mHandshakeComplete) {
			throw new IllegalStateException("Handshake not complete");
		}
		
		StringBuffer buf = new StringBuffer();

		int b = mInput.read();
		if ((b & 0x80) == 0x80) {
			// Skip data frame
			int len = 0;
			do {
				b = mInput.read() & 0x7f;
				len = b * 128 + len;
			} while ((b & 0x80) != 0x80);
			
			for (int i = 0; i < len; i++) {
				mInput.read();
			}
		}
		
		while (true) {
			b = mInput.read();
			if (b == 0xff) {
				break;
			}

			buf.append((char)b);			
		}

		return new String(buf.toString().getBytes(), "UTF8");
	}
	
	/**
	 * Closes the socket.
	 * @throws java.io.IOException
	 */
	public void close() throws java.io.IOException {
		mInput.close();
		mOutput.close();
		mSocket.close();
	}
	
	/**
	 * Quick echo test code.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put("key1", "value1");
			headers.put("key2", "value2");
			
			WebSocket ws = new WebSocket(new URI("ws://localhost:8080/echo"));
			ws.setHeaders(headers);
			ws.connect();

			String request = "Hello";
			ws.send(request);
			String response = ws.recv(); 
			System.out.println(request);
			if (request.equals(response)) {
				System.out.print("Success!");
			} else {
				System.out.print("Failed!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
