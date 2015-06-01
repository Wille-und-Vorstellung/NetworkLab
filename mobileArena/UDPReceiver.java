package mobileArena;

import java.util.*; 
import java.net.*;
import java.io.*;

/*
 *Description:
 *	execute the basic receiver side of the stop-wait protocol based on UDP
 * 
 */

public class UDPReceiver implements Runnable{
	//Static Value
	private static final int DEFAULT_LOCAL_PORT = 2072;
	private static final int DEFAULT_DES_PORT = 2048;
	private static final int DEFAULT_PACKET_SIZE = 20;
	private static final int DEFAULT_TIMEOUT = 3000;
	private static final int HEADER_SIZE = 5;
	private static final String DEFAULT_PATH = "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\test.txt";
	//Public Method
	public void run(){
		byte[] receiveBuffer = new byte[packetSize+10];
		boolean endOfLine = false;
		DatagramPacket temPacket = new DatagramPacket( receiveBuffer, 0, receiveBuffer.length ); 
		int temp;
		int sequence = 0;
		int packetCnt =0; 
		while( !endOfLine ){
			try{
				switch( state ){
				case 1:
					receiver.setSoTimeout(DEFAULT_TIMEOUT);
					try{
						receiver.receive( temPacket );
					}catch( SocketTimeoutException e ){//timeout 
						System.out.println( "receiver> Time out, terminating." );
						endOfLine = true;
						continue;
					}
					System.out.println("receiver> packet received");
					//simulates that the 2th packet is loss.
					if ( packetCnt == 1 ){
						System.out.println("receiver> packet corrupted.");
						packetCnt++;
						state = 1;
						continue;
					}
					temp = readData( temPacket );
					
					if ( temp == -1 ){//packet received is not a data packet.
						state = 1;						
						continue;
					}
					sequence = temp;
					packetCnt++;
					if ( state != 3 ){//state == 3 indicates that current received packet is the last one
						state++;
					}
					break;
				case 3:
					System.out.println("receiver> last ACK");
					endOfLine  = true;
				case 2:
					sendACK( sequence );
					System.out.println("receiver> ACK sent");
					state = 1;
					break;
				default:
					//System.out.println("Wrong state(receiver) detected.");
					break;
				}
			}catch( Exception e ){
				System.out.println( "receiver> Exception occured(receiver), current state: " + state + ", program tterminated.");
				break;
			}

		}
		System.out.println("receiver> Reassembled Data:");
		PrintWriter out = null;
		try {
			out = new PrintWriter(filePath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<Character> recovered = new ArrayList<Character>();
		recovered = getData();
		for (int j=0; j<recovered.size(); j++){
			System.out.print(recovered.get(j));
			out.print(recovered.get(j));
		}
		out.close();
		System.out.println();
		System.out.println("receiver> data has already written to " + filePath);
		System.out.println("receiver> mission complete, terminating.");
		dataReady = true;
		sender.close();
		receiver.close();
	}

	public boolean dataReady(){
		return dataReady;
	}


	public ArrayList<Character> getData(){
		return new ArrayList<Character>(receivedData);
	}
	//Constructor
	public UDPReceiver() throws UnknownHostException, SocketException{
		desPort = DEFAULT_DES_PORT;
		desAddr = InetAddress.getByName("127.0.0.1");
		localPort = DEFAULT_LOCAL_PORT;
		sender = new DatagramSocket();
		receiver = new DatagramSocket( localPort );
		receiveIndex = 0;
		packetSize = DEFAULT_PACKET_SIZE;
		receivedData = new ArrayList<Character>();
		state = 1;
		dataReady = false;
		filePath  =DEFAULT_PATH;
	}
	public UDPReceiver( String da, int dp, int lp, int ps, String path ) throws UnknownHostException, SocketException{
		desPort = dp;
		desAddr = InetAddress.getByName(da);
		localPort = lp;
		sender = new DatagramSocket();
		receiver = new DatagramSocket( localPort );
		receiveIndex = 0;
		packetSize = ps;
		receivedData = new ArrayList<Character>();
		state = 1;
		dataReady = false;
		filePath = path;
	}

	//Private Method
	private int readData(  DatagramPacket packet ){
		byte[] readBuffer = packet.getData();
		int temp ;
		int range;
		//analysis the header
		if ( readBuffer[0] != 'D' ){
			return -1;
		}
		temp = ( int )readBuffer[4];
		if ( readBuffer[1] == 1 ){//last packet
			range = readBuffer[2];
			state = 3;
		}
		else if ( readBuffer[1] == 0 ){
			range = packetSize - HEADER_SIZE;
		}
		else {
			System.out.println("receiver> Invalid readBuffer[1], take it as 1");
			range = readBuffer[2];
			state = 3;
		}

		//read date 
		
		System.out.println("receiver> packet content:");
		for ( int j=0; j<range; j++ ){
			receivedData.add( (char)readBuffer[ j + 5 ] );
			System.out.print((char)readBuffer[ j + 5 ]);
			
		}
		
		
		System.out.println();
		return temp;
	}

	private void sendACK( int sequence ) throws IOException{
		byte [] sendBuffer = new byte[DEFAULT_PACKET_SIZE+10];
		int range = packetSize;
		//set the header
		sendBuffer[0] = 'A';
		sendBuffer[4] = (byte) sequence;
		sendBuffer[1] = 0;
		sendBuffer[2] = (byte) packetSize;
		//transmit
		DatagramPacket toSend = new DatagramPacket( sendBuffer, 0,  sendBuffer.length,  desAddr, desPort);
		sender.send( toSend );
		return;		
	}

	//Private Variable
	private int desPort;
	private InetAddress desAddr;
	private int localPort;
	private DatagramSocket sender;
	private DatagramSocket receiver;
	private int receiveIndex;
	private int packetSize;
	private ArrayList<Character> receivedData;
	private int state; //{1,2,3}
	private boolean dataReady;

	private String filePath;
}
