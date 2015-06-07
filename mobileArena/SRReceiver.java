package mobileArena;

import java.io.*;
import java.util.*; 
import java.net.*;

/*
 *Description:
 *	execute the basic receiver side of the selective-repeat protocol
 *
 *@Author Ruogu Gao
 */

public class SRReceiver {
	//Static Value
	private static final int DEFAULT_DES_PORT = 2048;
	private static final int DEFAULT_PACKET_SIZE = 20;
	private static final int HEADER_SIZE = 5;
	private static final int DEFAULT_WINDOW_SIZE = 4;
	private static final int MAX_SEQ = 10;
	private static final String DEFAULT_PATH = "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\testSR.txt";
	private static final int DEFAULT_SENDER_ID = 1;
	private static final int DEFAULT_ID = 2;
	
	//Public Method
	public boolean startReceive( DatagramPacket receivedPacket ) throws IOException{
		//check
		if ( receivedPacket == null ){
			return false;
		}
		
		byte[] tempBuffer = new byte[packetSize];
		int ack = 0;
		int dataSize = 0;
		tempBuffer = receivedPacket.getData();
		//check whether the incoming packet is a data packet and whether have the correct senderID
		if ( dataReady ){
			return false;
		}
		else if ( tempBuffer[3] != senderID || tempBuffer[0] != 'D' ){
			return false;
		}
		ack = tempBuffer[4];

		//sequence number check
		if (ack < expectedSEQ){
			sendACK( ack );
			return false;
		}
		else if ( windowSpareSpace() <= 0){
			///do nothing
			return false;
		}
		else if ( matchSEQ( ack ) == -1 ){
			//do nothing
			return false;
		}

		//verify checksum
		if ( !checksumVerify( tempBuffer ) ){
			return false;
		}

		//check whether it is the last packet
		dataSize = packetSize - HEADER_SIZE;
		if ( tempBuffer[1] == 1 ){
			dataReady = true;
			dataSize = tempBuffer[2];
		}

		//push to window
		byte [] temp = new byte[dataSize];
		for (int i=0; i<dataSize; i++){
			temp[i] = tempBuffer[HEADER_SIZE + i];
		}
		pushToWindow( 0, 0, ack, temp );
		//reorganize window
		reorganizeWindow();

		//sendACK
		sendACK( ack );
		return true;
	}
	
	public boolean isReady(){
		return dataReady;
	}

	public void terminate(){
		sender.close();
	}

	public void writeToDisk(){
		if ( !isReady() ){
			return;
		}
		//write receivedData to disk
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
			out.print(recovered.get(j));
		}
		out.close();
		System.out.println("SRReceiver"+ID+" > data has already written to " + filePath);
	}

	public ArrayList<Character> getData(){
		return receivedData;
	}

	//Constructor
	public SRReceiver(  ) throws UnknownHostException, SocketException{
		desPort = DEFAULT_DES_PORT;
		desAddr = InetAddress.getByName("127.0.0.1");
		sender = new DatagramSocket();
		writeIndex = 0;
		packetSize = DEFAULT_PACKET_SIZE;
		receivedData = new ArrayList<Character>();
		dataReady = false;
		filePath  =DEFAULT_PATH;
		senderID = DEFAULT_SENDER_ID;
		expectedSEQ = 0;
		maxSeq = MAX_SEQ;
		window = new WindowEntry[windowSize];
		windowAvailbility = new boolean[windowSize];
		windowSEQ = new int[windowSize];
		ID = DEFAULT_ID;
		windowSize = DEFAULT_WINDOW_SIZE;
		
		for (int i=0; i<windowAvailbility.length;i++){
			windowAvailbility[i] = false;
		}
		initializeWSEQ(0);
	}

	public SRReceiver( String da, int dp, int ps, String path, int sid, int id, int seq, int ws ) throws UnknownHostException, SocketException{
		desAddr = InetAddress.getByName(da);
		sender = new DatagramSocket();
		desPort = dp;
		packetSize = ps;
		receivedData = new ArrayList<Character>();
		dataReady = false;
		filePath = path;
		senderID = sid;
		ID = id;
		writeIndex = 0;
		expectedSEQ = 0;
		maxSeq = seq;
		window = new WindowEntry[windowSize];
		windowAvailbility = new boolean[windowSize];
		windowSEQ = new int[windowSize];
		windowSize = ws;

		for (int i=0; i<windowAvailbility.length;i++){
			windowAvailbility[i] = false;
		}
		initializeWSEQ(0);
	}

	//Private Method
	private void pushToWindow( int left, int right, int seq, byte[] incoming ){//may involve an insert-sort on sequence number
		//window size check
		if ( window.length >= windowSize){
			return;
		}
		
		WindowEntry tempWE = new WindowEntry(left, right, seq);
		ArrayList<Character> tempC = new ArrayList<Character>();
		int index = 0;
		//copy the data
		tempC.clear();
		for ( int i=0; i<incoming.length; i++ ){
			tempC.add( (char) incoming[i] );
		}
		tempWE.setBuffer( tempC );
		index = getNoneOccupiedIndex(seq);

		//push to window (sort on sequence number)
		window[index] = tempWE;
		windowAvailbility[index] = true;
	}
	
	private int getNoneOccupiedIndex( int incoming ){
		for (int i=0; i<window.length; i++){
			if ( windowAvailbility[i] == false && window[i].seq == incoming){
				return i;
			}
		}
		System.out.println("SRReceiver "+ID+" > error(getNonegetNoneOccupiedIndex): find no free entry corespond to seq: "+incoming);
		return -1;
	}

	private int windowSpareSpace(){
		int cnt =0;
		for (int i=0; i<windowAvailbility.length; i++){
			if ( windowAvailbility[i] == false )
				cnt++;
		}
		return cnt;
	}

	private int matchSEQ(int seq){
		for ( int i=0; i<windowSEQ.length; i++ ) {
			if (  windowSEQ[i] == seq)
				return i;
		}
		return -1;
	}

	private void initializeWSEQ(int startSeq){
		int temp = startSeq;
		for ( int  i=0; i<windowSEQ.length; i++){
			windowSEQ[i] = temp;
			temp = nextSeqN(temp);
		}
	}

	private boolean checksumVerify( byte[] incoming ){//check whether the packet is corrupted
		//currently just nothing to do
		return true;
	}

	private void sendACK( int seq ) throws IOException {//send an ACK with given sequence number
		byte [] sendBuffer = new byte[packetSize];
		int range = packetSize;
		//set the header
		sendBuffer[0] = 'A';
		sendBuffer[4] = (byte) seq;
		sendBuffer[1] = 0;
		sendBuffer[2] = (byte) packetSize;
		sendBuffer[3] =(byte) senderID;
		//transmit
		DatagramPacket toSend = new DatagramPacket( sendBuffer, 0,  sendBuffer.length,  desAddr, desPort);
		sender.send( toSend );
		return;		
	}

	private boolean reorganizeWindow(){//check and write those continuous data to buffer(should there be any )
						  //thus window sliding may occur
		//find continuous data entry
		int range = 0;
		for ( int i=0; i<windowSEQ.length; i++ ){
			if ( windowAvailbility[i] == true){
				range++;
			}
		}
		if( range == 0 ){//no packet ready
			return false;
		}

		//read those entry
		for (int i=0; i<range; i++){
			for (int j=0; j<window[i].buffer.size(); j++){
				receivedData.add( window[i].buffer.get(j) );
			}
			windowAvailbility[i] = false;
		}

		//slide window(update expected sequence number)
		expectedSEQ = windowSEQ[range];
		for ( int i=0; i< window.length - range; i++){
			window[i] = window[range+i];
			windowAvailbility[i] = true;
		}
		for ( int j=range; j<window.length; j++ ){
			windowAvailbility[j] = false;
		}
		initializeWSEQ( expectedSEQ );
		
		return true;
	}

	private int nextSeqN(int i) {
		if ( i<maxSeq && i>=0){
			return ++i;
		}
		else if ( i == maxSeq ){
			return 0;
		}
		else  {
			System.out.println("SRReceiver "+ID+" > "+"error(nextSeqN): invalid input sequence number: " + i);
			return -1;
		}
	}

	//Private Variable
	private int desPort;
	private InetAddress desAddr;
	private DatagramSocket sender;
	private int packetSize;
	private ArrayList<Character> receivedData;
	private boolean dataReady;
	private String filePath;
	private int maxSeq;

	private WindowEntry[] window; 
	private boolean [] windowAvailbility;
	private int [] windowSEQ;
	private int expectedSEQ;//the squence number of the left-most entry in the window
	private int writeIndex;//the next byte to write in receivedData
	private int ID;
	private int senderID;
	private int windowSize;
}
