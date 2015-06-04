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
	

	//Public Method
	public void run(){



		/*sender part
			do{
			switch(state){
			case 1:
				flagS = sender.transmit();
			case 2:
				try{
					//wait 
				}catch(){
					//
				}
				
				if (timeout){
					sender.counterAddup();
					sender.timeoutCheckAndRetrans();
					//...
					state = 2;
					continue;
				}
				else if (incoming Datagram ){
					temp = sender.receivePacket();
					if (  temp == 1){
						sender.windowSlide();
						state = 1;
						continue;
					}
					temp_ = receiver.//....
					//....
				}
			default:
				break;
			}
			}while(flagS != true || flagR != true);
		*/
	}


	//Constructor
	public SRHost(){

	}

	public SRHost( String id, String desA, int desPs, int desPr, int localPs, int localPr, int pSize, byte[] data, String path ) throws UnknownHostException, SocketException{
		hostID = id;
		sender = new SRSender(desA, desPs, localPs, data, pSize);
		receiver = new SRReceiver(desA, desPr, localPr, pSize, path);
		dataReceived = new ArrayList<Character>();
		dataTosend = data;
	}

	//Private  Method



	//Private Variable
	private String hostID;
	private SRSender sender;
	private SRReceiver receiver;
	private byte[] dataTosend;
	private ArrayList<Character> dataReceived;
}
