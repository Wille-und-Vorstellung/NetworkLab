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
	private static final int DEFAULT_TIMEOUT = 2000;
	private static final int MAX_SEQ = 10;

	//Public Method
	public void run(){
		boolean flagS = false;
		boolean flagR = false;
		boolean timeout = false;;
		int state = 1;
		int temp = 0;
		boolean temp_ = false;
		DatagramPacket temPacket = null;
		do{
			try{
				switch(state){
				case 1:
					flagS = sender.transmit();
				case 2:
					listener.setSoTimeout(DEFAULT_TIMEOUT);
					temPacket = null;
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
						continue;
					}
					else {
						temp = sender.receivePacket( temPacket );
						if (  temp == 1){
							sender.windowSlide();
							state = 1;
							continue;
						}
						temp_ = receiver.startReceive( temPacket );
						state = 2;
						if (  temp_ == true ){
							receiver.terminate();
							flagR = true;
						}
					}
				default:
					break;
				}
			}catch( Exception e ){
					System.out.println("Host "+hostID+" > exception occurs");
					sender.terminate();
					receiver.terminate();
					return;
			}
		}while(flagS != true || flagR != true);
		
		sender.terminate();
		receiver.terminate();
		System.out.println("Host "+hostID+" > Mission complete thus terminated");
		
	}


	//Constructor
	public SRHost(){

	}

	public SRHost( String id, String desA, int desPs, int desPr, int localPs, int localPr, int pSize, byte[] data, String path ) throws UnknownHostException, SocketException{
		hostID = id;
		sender = new SRSender(desA, desPs, data, pSize, 1, MAX_SEQ);
		receiver = new SRReceiver(desA, desPr,  pSize, path,1, 2, MAX_SEQ, 4);
		dataReceived = new ArrayList<Character>();
		dataTosend = data;
		listener = new DatagramSocket();
	}

	//Private  Method



	//Private Variable
	private String hostID;
	private SRSender sender;
	private SRReceiver receiver;
	private DatagramSocket listener; 
	private byte[] dataTosend;
	private ArrayList<Character> dataReceived;
}
