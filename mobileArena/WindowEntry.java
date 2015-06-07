package mobileArena;

import java.util.*; 
import java.net.*;
import java.io.*;

public class WindowEntry {
	//Static Value
	
	//Public Method
	public void setBuffer( ArrayList<Character> incoming ){
		buffer.clear();
		for (int i=0; i<incoming.size(); i++){
		buffer.add( new Character( incoming.get(i) ) );
		}
	}	
	
	//constructor
	public WindowEntry(){
		acked = false;
		counter = 0;
		buffer = new ArrayList<Character>();
	}
	
	public WindowEntry(  int is, int ie, int iseq){
		this();
		indexS = is;
		indexE = ie;
		seq = iseq;
	}
	
	//Public Variable
	public int indexS;
	public int indexE;
	public int counter;
	public boolean acked;
	public int seq;
	public ArrayList<Character> buffer;
}
