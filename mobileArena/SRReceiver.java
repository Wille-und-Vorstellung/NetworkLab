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
			System.out.println("SRReceiver "+ID+" > "+"error(startReceive): null packet.");
			return false;
		}
		
		
		byte[] tempBuffer = new byte[packetSize];
		int ack = 0;
		int dataSize = 0;
		tempBuffer = receivedPacket.getData();
		//check whether the incoming packet is a data packet and whether have the correct senderID
		if ( isReady() ){
			System.out.println("SRReceiver "+ID+" > "+"warning(startReceive): receive has complete.");
			return false;
		}
		else if ( tempBuffer[3] != senderID || tempBuffer[0] != 'D' ){
			System.out.println("SRReceiver "+ID+" > "+"warning(startReceive): not data packet or wrong senderID: "+tempBuffer[3]);
			return false;
		}
		ack = tempBuffer[4];

		//sequence number check
		if (ack < expectedSEQ){
			sendACK( ack );
			System.out.println("SRReceiver "+ID+" > "+"warning(startReceive):incomingSEQ < expectedSEQ: "+ack+" < "+expectedSEQ);
			return false;
		}
		else if ( windowSpareSpace() <= 0){
			///do nothing
			System.out.println("SRReceiver "+ID+" > "+"warning(startReceive): no spare entry left.");
			return false;
		}
		else if ( matchSEQ( ack ) == -1 ){
			//do nothing
			System.out.println("SRReceiver "+ID+" > "+"error(startReceive): unexpected seq: "+ack+", expect: "+expectedSEQ);
			return false;
		}

		//verify checksum
		if ( !checksumVerify( tempBuffer ) ){
			System.out.println("SRReceiver "+ID+" > "+"error(startReceive): packet corrupted.");
			return false;
		}

		//simulate packet loss
		if ( lossCnt == ack && lossSign ){
			lossSign = false;
			return false;
		}
		
		//check whether it is the last packet
		dataSize = packetSize - HEADER_SIZE;
		if ( tempBuffer[1] == 1 ){
			dataReady = true;
			lastPacketSEQ = ack;
			dataSize = tempBuffer[2];
		}

		//push to window
		byte [] temp = new byte[dataSize];
		for (int i=0; i<dataSize; i++){
			temp[i] = tempBuffer[HEADER_SIZE + i];
		}
		System.out.println("SRReceiver "+ID+" > window seq status(before): "+windowSEQ[0]+" "+windowSEQ[1]+" "+windowSEQ[2]+" "+windowSEQ[3]);
		pushToWindow( 0, 0, ack, temp );
		//reorganize window
		reorganizeWindow();
		System.out.println("SRReceiver "+ID+" > window seq status(after): "+windowSEQ[0]+" "+windowSEQ[1]+" "+windowSEQ[2]+" "+windowSEQ[3]);
		//sendACK
		sendACK( ack );
		System.out.println("SRReceiver "+ID+" > packet received, ACK sent, sequence number: "+ack);
		return true;
	}
	
	public boolean isReady(){
		return dataReady && notUnReceived();
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
		System.out.println("SRReceiver "+ID+" > data has already written to " + filePath);
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
		windowSize = DEFAULT_WINDOW_SIZE;
		window = new WindowEntry[windowSize];
		windowAvailbility = new boolean[windowSize];
		windowSEQ = new int[windowSize];
		ID = DEFAULT_ID;
		lastPacketSEQ = -1;
		lossCnt = 1;
		lossSign = true;
		
		for (int i=0; i<window.length; i++){
			window[i] = new WindowEntry();
		}
		
		for (int i=0; i<windowAvailbility.length;i++){
			windowAvailbility[i] = true;
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
		windowSize = ws;
		window = new WindowEntry[windowSize];
		windowAvailbility = new boolean[windowSize];
		windowSEQ = new int[windowSize];
		lastPacketSEQ = -1;
		lossCnt = 1;
		lossSign = true;
		
		for (int i=0; i<window.length; i++){
			window[i] = new WindowEntry();
		}
		
		for (int i=0; i<windowAvailbility.length;i++){
			windowAvailbility[i] = true;
		}
		initializeWSEQ(0);
	}

	//Private Method
	private void pushToWindow( int left, int right, int seq, byte[] incoming ){//may involve an insert-sort on sequence number
		/*
		if (window.length>=windowSize){
			return;
		}
		*/
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
		System.out.println("> "+index);
		//push to window (sort on sequence number)
		window[index].set(tempWE);
		windowAvailbility[index] = false;
		System.out.println("SRReceiver "+ID+" > "+"(pushToWindow): packet: "+seq+" pushed to entry:"+index);
	}
	
	private int getNoneOccupiedIndex( int incoming ){
		//System.out.println("SRReceiver "+ID+" > "+"check point getNOIndex");
		for (int i=0; i<window.length; i++){
			if ( windowAvailbility[i] == true && windowSEQ[i] == incoming){
				return i;
			}
		}
		System.out.println("SRReceiver "+ID+" > error(getNonegetNoneOccupiedIndex): find no free entry corespond to seq: "+incoming);
		return -1;
	}
	
	private int getOccupiedIndex( int incoming ){
		for ( int i=0; i< window.length; i++ ){
			if ( windowAvailbility[i] == false && windowSEQ[i] == incoming ){
				return i;
			}
		}
		System.out.println("SRReceiver "+ID+" > error(getOccupiedIndex): find no free entry corespond to seq: "+incoming);
		return -1;
	}
	
	private boolean notUnReceived(){
		if ( lastPacketSEQ == -1 ){
			return false;
		}
		int lastPacketIndex = getOccupiedIndex( lastPacketSEQ );
		for (int i=0; i<lastPacketIndex; i++){
			if(windowAvailbility[i] == true){
				return false;
			}
		}
		return true;
	}
	
	private int windowSpareSpace(){
		int cnt =0;
		for (int i=0; i<windowAvailbility.length; i++){
			if ( windowAvailbility[i] == true )
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
				break;
			}
			range++;
		}
		if( range == 0 ){//no packet ready
			System.out.println("SRReceiver "+ID+" > "+"warning(reorganizeWindow): no continuous packet ready.");
			return false;
		}
		System.out.println("SRReceiver "+ID+" > "+"check point reorag 1");
		//read those entry
		for (int i=0; i<range; i++){
			for (int j=0; j<window[i].buffer.size(); j++){
				receivedData.add( window[i].buffer.get(j) );
			}
			//windowAvailbility[i] = true;
		}
		System.out.println("SRReceiver "+ID+" > "+"check point reorag 2, range: "+range);
		
		//slide window(update expected sequence number)
		if ( range < window.length ){
			expectedSEQ = windowSEQ[range];
		}
		else {///range = window.length
			expectedSEQ = nextSeqN(windowSEQ[window.length-1]);
		}
		System.out.println("SRReceiver "+ID+" > "+"check point reorag 3, new expectedSEQ: "+expectedSEQ+" window.Length: "+window.length);
		//System.out.println("SRReceiver "+ID+" > checking window.");
		/*
		for(int j=0; j<window.length; j++){
			System.out.print(j+"th ");
			System.out.print(window[j].seq+" ");
		}
		*/
		//System.out.println("SRReceiver "+ID+" > checking window complete.");
		for ( int i=0; i< window.length - range; i++){
			window[i].set(window[range+i].clone());
			windowAvailbility[i] = windowAvailbility[range+i];
			///System.out.println("SRReceiver "+ID+" > "+"check point reorag 3+, i: "+i);
		}
		System.out.println("SRReceiver "+ID+" > "+"check point reorag 4");
		for ( int j=range; j<window.length; j++ ){
			windowAvailbility[j] = true;
		}
		System.out.println("SRReceiver "+ID+" > "+"check point reorag 5");
		initializeWSEQ( expectedSEQ );
		System.out.println("SRReceiver "+ID+" > "+"check point reorag 6");
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
	
	//Public Variable
	public int lossCnt;
	public boolean lossSign;
	
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
	private int lastPacketSEQ;
}
