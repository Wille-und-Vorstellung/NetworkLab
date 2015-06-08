package mobileArena;

import java.io.*;
import java.util.*; 
import java.net.*;
 
/*
 *Description:
 *	the host incorped with select-retransmiting protocol
 *
 *
 *@Author Ruogu Gao
 */

public class SRHost implements Runnable{
	//Status Values
	private static final int DEFAULT_TIMEOUT = 5000;
	private static final int MAX_SEQ = 10;

	//Public Method
	public void run(){
		boolean flagS = false;
		boolean flagR = false;
		boolean timeout = false;;
		int state = 1;
		int temp = 0;
		int slideLength = 0;
		boolean temp_ = false;
		byte[] receiveBuffer = new byte[packetSize];
		DatagramPacket temPacket = new DatagramPacket(receiveBuffer, 0, receiveBuffer.length);
		do{
			try{
				switch(state){
				case 1:
					flagS = sender.transmit();
					state = 2;
					break;
				case 2:
					listener.setSoTimeout(DEFAULT_TIMEOUT);
					timeout = false;
					try{
						listener.receive( temPacket );
					}catch(Exception e){
						System.out.println("Host "+hostID+" > timeout");
						timeout = true;
					}
					
					if (timeout){
						sender.counterAddup();
						sender.timeoutCheckAndRetransmit();
						//...
						state = 2;
						timeout = false;
						break;
					}
					else {
						temp = sender.receivePacket( temPacket );
						//System.out.println("Host "+hostID+" > temp: "+temp);
						if (  temp == 1){
							slideLength = sender.windowSlide();
							state = 1;
							//System.out.println("Host "+hostID+" > window slide: "+slideLength);
							break;
						}
						System.out.println("Host "+hostID+" > receiving");
						temp_ = receiver.startReceive( temPacket );
						//System.out.println("Host "+hostID+" > temp_: "+temp_);
						state = 2;
						if (  receiver.isReady() ){
							flagR = true;
						}
					}
				default:
					break;
				}
			}catch( Exception e ){
					System.out.println("Host "+hostID+" > exception occurs:"+e.getLocalizedMessage());
					sender.terminate();
					receiver.terminate();
					return;
			}
		}while(flagS != true || flagR != true);
		
		receiver.writeToDisk();
		sender.terminate();
		receiver.terminate();
		System.out.println("Host "+hostID+" > Mission complete thus terminated");
		
	}


	//Constructor
	public SRHost(){

	}

	public SRHost( String id, String desA, int desP, int localP, int pSize, byte[] data, String path, int senderID, int desID, int receiverID ) throws UnknownHostException, SocketException{
		hostID = id;
		sender = new SRSender(desA, desP, data, pSize, senderID, MAX_SEQ);
		receiver = new SRReceiver(desA, desP,  pSize, path, desID, receiverID, MAX_SEQ, 4/*windowSize*/);
		dataReceived = new ArrayList<Character>();
		dataTosend = data;
		listener = new DatagramSocket( localP );
		System.out.println("SRHost "+id+" > listener port: "+localP);
		packetSize = pSize;
	}

	//Private  Method



	//Private Variable
	private String hostID;
	private SRSender sender;
	private SRReceiver receiver;
	private DatagramSocket listener; 
	private byte[] dataTosend;
	private ArrayList<Character> dataReceived;
	private int packetSize;
}
