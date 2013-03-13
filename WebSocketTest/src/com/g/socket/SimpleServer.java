package com.g.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.BASE64Encoder;

public class SimpleServer {
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
		new SimpleServer();
	}

	public SimpleServer() throws IOException, NoSuchAlgorithmException {
		ServerSocket ss = new ServerSocket(30000);
		Socket socket = ss.accept();
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		
		byte[] buff = new byte[1024];
		int count = -1;
		String req = "";
		
		// read data, at this time establish handshake with websocket.
		count = in.read(buff);
		req = new String(buff, 0, count);
		System.out.println("handshake request : " + req);
		
		// acquire key of web socket.
		String secKey = getSecWebSocketKey(req);
		System.out.println("secKey = " + secKey);
		String response = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: "
			+ "websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "
			+ getSecWebSocketAccept(secKey) + "\r\n\r\n";
		System.out.println("secAccept = " + getSecWebSocketAccept(secKey));
		out.write(response.getBytes());
		
		// again, read data from web socket.
		count = in.read(buff);
		System.out.println("bytes amount received : " + count);
		
		/*
		 * because some protocol abeyed by web socket.
		 * 3 ~ 6 bytes
		 * the 7th byte is real 
		 */
		for (int i = 0; i < count - 6; i++) {
			buff[i + 6] = (byte) (buff[i%4 + 2] ^ buff[i + 6]);
		}
		// display data read.
		System.out.println("content received : " + new String(buff, 6, count -6, "UTF-8"));
		
		// the 1st letter must be identical with the 1st letter read form 
		// data, when sending data.
		byte[] pushHead = new byte[2];
		pushHead[0] = buff[0];
		String pushMsg = "Received! Received! Welcome to the world of WEB SOCKET!";
		
		// the 2ed byte record the length of the sending data.
		pushHead[1] = (byte) pushMsg.getBytes("UTF-8").length;
		
		// send the first 2 bytes
		out.write(pushHead);
		// send contents data
		out.write(pushMsg.getBytes());
		
		// close socket
		socket.close();
		ss.close();
	}

	private String getSecWebSocketAccept(String secKey) 
		throws UnsupportedEncodingException, NoSuchAlgorithmException {
		String guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		secKey += guid;
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(secKey.getBytes("ISO-8859-1"), 0, secKey.length());
		byte[] shalHash = md.digest();
		BASE64Encoder encoder = new BASE64Encoder();
		return encoder.encode(shalHash);
	}

	private String getSecWebSocketKey(String req) {
		Pattern p = Pattern.compile("^(Sec-WebSocket-Key:).+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Matcher m = p.matcher(req);
		if (m.find()) {
			String foundstring = m.group();
			return foundstring.split(":")[1].trim();
		} else {
			return null;
		}
	}
	
}
