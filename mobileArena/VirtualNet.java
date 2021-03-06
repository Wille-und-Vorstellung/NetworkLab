package mobileArena;

import java.util.*;
import java.io.*;
import java.net.*;

/*
 *Description:
 *	the over-all starter of two UDP host(the sender and the receiver separately )
 *	multi-thread applied 
 *
 */

public class VirtualNet {
	//main entrance
	public static void main( String arg [] ){
		VirtualNet test01 = new VirtualNet();
		int mod =3;
		try {
			test01.start(mod);
		} catch (UnknownHostException | SocketException | FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}

	//Method
	public int start( int mod ) throws UnknownHostException, SocketException, FileNotFoundException{
		//data for mod 2 and 3 
		byte[] aTob = new byte[63];
		byte[] bToa = new byte[63];
		
		System.out.println("Reading input file on disk.");
		Scanner alice = new Scanner( new File(  "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\AliceToBob.txt" ) );
		Scanner bob = new Scanner( new File(  "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\BobToAlice.txt" ) );
		String temp_=null, temp=null;
		temp = alice.nextLine();
		temp_ = bob.nextLine();
		for (int i=0; i<63; i++){
			aTob[i] = (byte)temp.charAt(i);
			bToa[i] = (byte)temp_.charAt(i);
		}
		
		switch(mod){
		case 1://stage one : unidirectional transfer
			//construct data 
			byte[] testData = new byte[63];
			System.out.println("Original Data:");
			for (int i=0; i<20; i++){
				testData[i] = (byte) ('a'+i);
				System.out.print((char)testData[i]);
			}
			for ( int j=0; j<20; j++ ){
				testData[j+20] = (byte) ('a'+j);
				System.out.print((char)testData[j+20]);
			}
			for (int k=0; k<23; k++){
				testData[k+40] = (byte) ('a'+k);
				System.out.print((char)testData[k+40]);
			}
			System.out.println();
			System.out.println("--------------------------------------");

			//stage one : unidirectional transfer
			UDPSender sender = new UDPSender("127.0.0.1", 2048, 2072, testData, 20);
			UDPReceiver receiver = new UDPReceiver("127.0.0.1", 2072, 2048, 20, "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\test.txt");
			Thread senderThread = new Thread( sender );
			Thread receiverThread = new Thread( receiver );

			senderThread.start();
			receiverThread.start();
			break;
		case 2:
			//stage two :bidirectional transfer
			UDPHost hostAlice = new UDPHost( "Alice", "127.0.0.1", 2048, 2049, 2072, 2073, 20, aTob, "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\fromBob.txt" );
			UDPHost hostBob = new UDPHost( "Bob", "127.0.0.1", 2073, 2072, 2049, 2048, 20, bToa,  "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\fromAlice.txt");
			Thread threadAlice = new Thread( hostAlice );
			Thread threadBob = new Thread( hostBob );

			threadAlice.start();
			threadBob.start();
			break;
			
		case 3://SR test
		//public SRHost( String id, String desA, int desPs, int desPr, int localP, int pSize, byte[] data, String path int senderID, int desID, int receiverID)
			SRHost srHostAlice = new SRHost("Alice", "127.0.0.1", 2048, 2072, 20, aTob, "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\fromBobSR.txt", 1, 2, 1);
			SRHost srHostBob = new SRHost("Bob", "127.0.0.1", 2072, 2048, 20, bToa, "D:\\ProgrammingProjects\\Eclipse_Java\\NetworkLab2\\fromAliceSR.txt", 2, 1, 2);
			Thread threadAliceSR = new Thread( srHostAlice );
			Thread threadBobSR = new Thread( srHostBob );
			
			threadAliceSR.start();
			threadBobSR.start();
			
			break;
		default:
			System.out.println("Wrong Mode number.");
			break;
			
		}

		return 117;
	}
	//Constructor
	public VirtualNet(){
		//do nothing 
	}

	//Variable

}
