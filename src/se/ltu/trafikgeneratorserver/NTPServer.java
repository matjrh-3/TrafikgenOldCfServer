package se.ltu.trafikgeneratorserver;

import java.net.*;
import java.io.*;

public class NTPServer {

	private int NTPPort = 123;
	private byte[] NTPData = new byte[48];
	// byte[] NTPData2;
	private long seventyOffset; // offset (in ms) between 1900 and 1970

	private DatagramPacket NTPPacket;
	private DatagramSocket NTPSocket;

	public NTPServer() {
		try {
			System.out.println("Server started!");

			seventyOffset = 70 * 365; // days in 70 years
			seventyOffset += 17; // add days for leap years between 1900 and
									// 1970
			seventyOffset *= 24; // hours in a day
			seventyOffset *= 60; // minutes in an hour
			seventyOffset *= 60; // seconds in a minute
			seventyOffset *= 1000; // milliseconds in a second

			NTPPacket = new DatagramPacket(NTPData, NTPData.length);
			NTPSocket = new DatagramSocket(NTPPort);

			while (true) {
				NTPSocket.receive(NTPPacket);
				new NTPServerThread(NTPPacket, NTPSocket, seventyOffset).start();
			}
		}

		catch (SocketException e) {
			System.err.println("Can't open socket: " + e.getMessage());
			System.exit(1);
		}

		catch (IOException e) {
			System.err.println("Communication error! " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error!" + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new NTPServer();
	}
}