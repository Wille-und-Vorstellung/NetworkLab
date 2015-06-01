package mobileArena;

import java.util.*; 
import java.net.*;
import java.io.*;

/*
 *Description:
 *	a combined with the sender and the receiver by having a UDPSender & UDPReceiver (respectively)running on *    separate threads.
 * 
 */

public class UDPHost implements Runnable{
	//Static Value

	//Public Method
	public void run(){
		
		Thread senderThread = new Thread( sender );
		Thread receiverThread = new Thread( receiver );
		senderThread.start();
		receiverThread.start();
		System.out.println("HOST: " + hostID + "> " + "bidirectional data transfer initiated.");
		while( !receiver.dataReady() );
		System.out.println("HOST: " + hostID + "> " + "transference complete, received data :");
		getReceivedData();
		System.out.println("HOST: " + hostID + "> ");
		for (int i=0; i<dataReceived.size(); i++){
			System.out.print(dataReceived.get(i));
		}
		System.out.println();
		System.out.println("HOST: " + hostID + "> " + "terminating");
	}

	public void setSendingData( byte[] incomingData ){
		dataTosend = incomingData;
	}

	public ArrayList<Character> getReceivedData(){
		if (!receiver.dataReady() ){
			return null;
		}
		dataReceived = receiver.getData();
		return new ArrayList<Character>(dataReceived);
	}

	public boolean isReady(){
		return receiver.dataReady();
	}

	//Constructor
	public UDPHost( String id, String desA, int desPs, int desPr, int localPs, int localPr, int pSize, byte[] data, String path ) throws UnknownHostException, SocketException{
		hostID = id;
		sender = new UDPSender(desA, desPs, localPs, data, pSize);
		receiver = new UDPReceiver(desA, desPr, localPr, pSize, path);
		dataReceived = new ArrayList<Character>();
		dataTosend = data;
	}
	//Private Variable
	private String hostID;
	private UDPSender sender;
	private UDPReceiver receiver;
	private byte[] dataTosend;
	private ArrayList<Character> dataReceived;

	private int localPort;
	private int desPort;
	private int packetSize;
	private String desAddr;
}
