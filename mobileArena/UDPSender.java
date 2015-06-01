package mobileArena;

import java.util.*; 
import java.net.*;
import java.io.*;

/*
 *Description:
 *	execute the basic sender side of the stop-wait protocol(RDT3.0 as regard of the book) based on UDP
 * 
 */

public class UDPSender implements Runnable{
	//Static Value
	private static final int DEFAULT_PACKET_SIZE = 20;
	private static final int DEFAULT_LOCAL_PORT = 2048;
	private static final int DEFAULT_DES_PORT = 2072;
	private static final int DEFAULT_TIMEOUT = 2000;
	private static final int HEADER_SIZE = 5;

	//Public Method
	public void setData( byte [] data ){
		if ( dataSent == null ){
			dataSent = data;
		}
		System.out.println("sender> The data has already be initialized, the attemp to reset is thus terminated.");
		return;
	}

	public void run(  ){
		DatagramPacket temPacket = null;
		byte [] receiveBuffer = new byte[DEFAULT_PACKET_SIZE+10];

		while( sendIndex <= dataSent.length ){
			try{
				switch( state ){
				case 1://state 1: just send it
					sendPacket( sendIndex, sequenceN );
					System.out.println("sender> packet sent, offset:" + sendIndex);
					state++;
					break;
				case 2://state 2: waiting for ACK 
					temPacket = new DatagramPacket( receiveBuffer, 0, receiveBuffer.length );
					//start timer
					receiver.setSoTimeout( DEFAULT_TIMEOUT );
					try{
						receiver.receive( temPacket );
					}catch( SocketTimeoutException e ){//time out intrigered
						System.out.println( "sender> Time out, initiating re-transmit." );
						state = 1;
						continue;
					}
					//check 
					if ( receiveBuffer != temPacket.getData() ){
						System.out.println("sender> Warning: receiveBuffer != temPacket.getData(), for some currently unknown reason.");
						receiveBuffer = temPacket.getData();
					}
					if ( false == checksumVerify( receiveBuffer ) || false == ackVerify( receiveBuffer, sequenceN )  
					      ){//packet corrupted or wrong ack \
						System.out.println("sender> Wrong ACK or packet corrupted.");
						state = 1;//re-send
						continue;
					}
					else {//ACK verified
						System.out.println("sender> ACK verified");
						sendIndex = sendIndex + packetSize - headerSize;//update what originally should been done as state one
						//reset and stop timer

						//update sequence number
						sequenceN = nextSequenceN( sequenceN );
					}
					state = 1;
					break;
				default:
					System.out.println("sender> Invalid state number detected, what hell is going on out there?");
					break;
				}
			}catch( Exception e ){
				System.out.println( "sender> Exception occured, current state: " + state + ", program tterminated.");
				break;
			}
		}
		System.out.println("sender> mission complete, terminating.");
		receiver.close();
		sender.close();
	}
	//Constructor
	public UDPSender() throws UnknownHostException, SocketException{
		desAddr = InetAddress.getByName("127.0.0.1");
		desPort = DEFAULT_DES_PORT;
		localPort = DEFAULT_LOCAL_PORT;
		dataSent = null;
		sender = new DatagramSocket();
		receiver = new DatagramSocket( localPort );
		sendIndex = 0;
		packetSize = DEFAULT_PACKET_SIZE;
		sequenceN = 0;
		state = 1;
		headerSize = HEADER_SIZE;
	}
	public UDPSender( String address, int desPortal, int localPortal, byte [] data, int size ) throws UnknownHostException, SocketException{
		desAddr = InetAddress.getByName(address);
		desPort = desPortal;
		localPort = localPortal;
		dataSent = data;
		sender = new DatagramSocket();
		receiver = new DatagramSocket( localPort );
		sendIndex = 0;
		packetSize = size;
		sequenceN = 0;
		state = 1;
		headerSize = HEADER_SIZE;
	}
	//Private Method
	private void sendPacket(  int index, int sequence ) throws IOException{//just send the packet, do not update sendIndex
		byte [] sendBuffer = new byte[DEFAULT_PACKET_SIZE+10];
		int range = 0;
		//set data
		range = ( dataSent.length - sendIndex < packetSize - headerSize )? dataSent.length - sendIndex : packetSize - headerSize; 
		for (int i=0; i<range; i++){
			sendBuffer[i+headerSize] = dataSent[index+i];
		}
		//set the header
		sendBuffer[0] = 'D';
		sendBuffer[4] = (byte) sequence;
		if ( range != packetSize - headerSize){//last packet to send
			sendBuffer[1] = 1;
			sendBuffer[2] = (byte) range;
		}
		else {
			sendBuffer[1] = 0;
			sendBuffer[2] = (byte) packetSize;
		}
		//transmit
		DatagramPacket toSend = new DatagramPacket( sendBuffer, 0,  sendBuffer.length,  desAddr, desPort);
		sender.send( toSend );
		return;
	}

	private boolean checksumVerify( byte[] buff ){
		//no checksum verify applied right now
		return true;
	}

	private boolean ackVerify( byte[] buff ,  int sequence ){
		if ( buff[0] == 'A' && buff[4] == sequence ){
			return true;
		}
		else if ( buff[0] != 'A' ){
			System.out.println( "sender> Not ACK packet." );
		}
		else if ( buff[4] != sequence ){
			System.out.println( "sender> Wrong sequence number." );
		}
		return false;
	}

	private int nextSequenceN( int a ){
		if ( a == 0 ){
			return 1;
		}
		else if ( a == 1 ){
			return 0;
		}
		else {
			System.out.println("sender> Invalid sequence number detected.");
			return 0;
		}

	}
	//Private Variable
	private byte [] dataSent;
	private DatagramSocket sender;
	private DatagramSocket receiver;
	private InetAddress desAddr;//des address
	private int desPort;//des port
	private int localPort;
	private int state;//only two state possible : 1 2 ;
	
	private int sendIndex;//points to the next bytes to send
	private int packetSize;//byte
	private int headerSize;//5 bytes
	private int sequenceN;
	
}
