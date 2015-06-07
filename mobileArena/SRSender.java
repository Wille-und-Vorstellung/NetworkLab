package mobileArena;

import java.util.*; 
import java.net.*;
import java.io.*;

/*
 *Description:
 *	execute the basic sender side of the selective-repeat protocol
 * 
 */

public class SRSender {
	//Static Value
	private static final int DEFAULT_PACKET_SIZE = 20;
	private static final int DEFAULT_DES_PORT = 2072;
	private static final int DEFAULT_COUNTER_BOUND = 2;//2 time unit
	private static final int HEADER_SIZE = 5;
	private static final int WINDOW_SIZE = 4;
	private static final int MAX_SEQ = 10;
	private static final int DEFAULT_ID = 1;

	//Public Method
	public boolean transmit() throws IOException{//transmit as much packet as possible
		//check if data is all sent and acked
		if ( sendIndex >= dataSent.length && window.size() ==0 ){
			return true;
		}

		//check remaining window entry
		int range = WINDOW_SIZE - window.size();
		if ( range == 0 ){
			return false;
		}
		else if ( sendIndex >= dataSent.length ){
			return false;
		}

		//transmit and push to window entry
		boolean done = false;
		int tempRange = 0;
		for (int i=0; i<range; i++){
			if ( sendIndex >= dataSent.length ){
				done = true;
				break;
			}
			tempRange = (( dataSent.length - sendIndex ) > ( packetSize - headerSize ) )? ( packetSize - headerSize ): ( dataSent.length - sendIndex );
			//transmit
			transmitPacket( sendIndex, sendIndex+tempRange );
			//push to window
			pushEntry(sendIndex, sendIndex+tempRange);

			sendIndex += tempRange;
		}

		if (done){
			System.out.println("SRSender "+senderID+" > "+" Transmission complete.");
			return false;
		}
		return false;
	}

	public int receivePacket( DatagramPacket receivedPacket ){//receive one data packet(ack)
		//check 
		if ( receivedPacket == null ){
			return 0;
		}
		
		byte [] receiveBuffer = new byte[packetSize];
		receiveBuffer = receivedPacket.getData();
		int incomingSeq  =0, i=0;
		//check if is coresponding(ie with the correct senderID) ACK
		if (receiveBuffer[0] != 'A' || receiveBuffer[3] != senderID){
			return 0;
		}
		//set coresponding window entry
		incomingSeq = receiveBuffer[4];
		for ( i=0; i<window.size(); i++ ){
			if ( window.get(i).seq == incomingSeq ){
				window.get(i).acked = true;
				break;
			}
		}
		if ( i == window.size() ){//seq not found
			System.out.println("SRSender "+senderID+" > "+"error(receivePacket):invalid sequence number:" + incomingSeq);
			return 0;
		}

		return 1;
	}

	public int windowSlide(){
		int slideLength = 0;
		//locate slide length(those in the window that has been acked)
		for (int i=0; i<window.size(); i++){
			if ( window.get(i).acked == false ){
				break;
			}
			slideLength++;
		}
		if ( slideLength == 0 ){
			return slideLength;
		}

		//slide window
		window = (ArrayList<WindowEntry>) window.subList(slideLength, window.size()-1);
		
		return slideLength;
	}

	public void counterAddup(){//add every entry in the window 
		for (int i=0; i<window.size(); i++){
			if ( window.get(i).acked == false ){
				window.get(i).counter = window.get(i).counter+1;
			}
			
		}
		return ;
	}

	public void timeoutCheckAndRetransmit() throws IOException{//initiating retransmit if necessary
		//check time out
		for ( int j=0; j<window.size(); j++ ){
			if (window.get(j).counter > DEFAULT_COUNTER_BOUND && window.get(j).acked == false){//retransmit this packet
				reTransmit( j );
				System.out.println("SRSender "+senderID+" > "+"retransmit< "+window.get(j).indexS+", "+window.get(j).indexE+">");
			}
		}
		return;
	}

	public void terminate(){
		sender.close();
	}	

	//Constructor
	public SRSender() throws UnknownHostException, SocketException {
		dataSent = null;
		sender = new DatagramSocket();
		desAddr = InetAddress.getByName("127.0.0.1");
		desPort = DEFAULT_DES_PORT;
		senderID = DEFAULT_ID;
		sendIndex = 0;
		packetSize = DEFAULT_PACKET_SIZE;
		maxSeqN = MAX_SEQ;
		sequenceN = 0;
		windowNext = 0;
		window = new ArrayList<WindowEntry>();
		headerSize = HEADER_SIZE;
	}

	public SRSender( String address, int desPortal, byte [] data, int size, int id, int seqN ) throws UnknownHostException, SocketException {
		dataSent = data;
		packetSize = size;
		senderID = id;
		desPort = desPortal;
		desAddr = InetAddress.getByName( address );
		sequenceN = 0;
		maxSeqN = seqN;
		sendIndex = 0;
		windowNext = 0;
		window = new ArrayList<WindowEntry>();
		headerSize = HEADER_SIZE;
	}

	//Private Method
	private boolean transmitPacket( int indexS,  int indexE ) throws IOException{//transmit the data in dataSent from indexS to indexE in one packet
		//boundary-check
		if (  indexS >= indexE || indexS >=  dataSent.length || indexE >= dataSent.length){
			System.out.println("SRSender "+senderID+" > "+"error(transmitPacket): invalid input range( "+indexS+", "+indexE+" )");
			return false;
		}
		else if (indexE - indexS > packetSize - headerSize ){//exceeds the packet length
			System.out.println("SRSender "+senderID+" > "+"error(transmitPacket): invalid(exceeds packetSize) input range( "+indexS+", "+indexE+" )");
			return false;	
		}
		//construct the packet
		byte [] sendBuffer = new byte[packetSize];
		int range=0;
		////construct head
		sendBuffer[0] = 'D';
		sendBuffer[4] = (byte) nextSeqN();
		sendBuffer[3] = (byte) senderID;
		if ( indexE == dataSent.length ){//last packet to send
			sendBuffer[1] = 1;
			sendBuffer[2] = (byte) (indexE - indexS);
			range = indexE - indexS;
		}
		else {
			sendBuffer[1] = 0;
			sendBuffer[2] = (byte) (packetSize - headerSize);
			range = packetSize - headerSize;
		}
		
		////copy data
		for (int i=0; i<range; i++){
			sendBuffer[headerSize+i] = dataSent[indexS+i];
		}
		
		//send
		DatagramPacket toSend = new DatagramPacket( sendBuffer, 0,  sendBuffer.length,  desAddr, desPort);
		sender.send( toSend );
		return true;
	}
	
	private void pushEntry(int a, int b){//push a new entry to window
		window.add(new WindowEntry(a, b, currentSeqN()));
	}

	private int reTransmit( int index ) throws IOException{//index indicates the entry in current window that needs to be retransmitted
		//check
		if ( index < 0 || index>=window.size() ){
			System.out.println("SRSender "+senderID+" > "+"error(retrnamit): invalid index: " + index);
			return 0;
		}
		else if ( window.get(index).acked ){
			System.out.println("SRSender "+senderID+" > "+"error(retrnamit): invalid index: " + index+"already ACKed");
			return 0;
		}	
		
		//retransmit
		boolean flag = false;
		flag = transmitPacket( window.get(index).indexS, window.get(index).indexE );
		//reset counter
		window.get(index).counter = 0;
		window.get(index).acked = false;
		
		if (flag){
			return 1;
		}
		System.out.println("SRSender "+senderID+" > "+"error(retrnamit): fail to retransmit packet at:" + index);
		return 0;
	}

	private int nextSeqN(){
		if ( sequenceN<maxSeqN && sequenceN>=0){
			return sequenceN++;
		}
		else if ( sequenceN == maxSeqN ){
			sequenceN = 0;
			return maxSeqN;
		}
		else  {
			System.out.println("SRSender "+senderID+" > "+"error(nextSeqN): invalid input sequence number: " + sequenceN);
			return -1;
		}
	}

	private int currentSeqN(){
		
	}

	//Private Variable
	private byte [] dataSent;
	private DatagramSocket sender;
	private InetAddress desAddr;//des address
	private int desPort;//des port
	//private int state;//only two state possible : 1 2 ;
	private int senderID;

	private int sendIndex;//points to the next bytes to send
	private int packetSize;//byte
	private int headerSize;//byte
	private int sequenceN;//next available sequence number
	private int maxSeqN;//the maximum sequence number possible
	private int windowNext;
	private ArrayList<WindowEntry> window;
}
