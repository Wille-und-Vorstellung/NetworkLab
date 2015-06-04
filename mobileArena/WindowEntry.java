package mobileArena;



public class WindowEntry {
	//Constructor
	public WindowEntry(){
		indexS = 0;
		indexE = 0;
		counter = 0;
		acked = false;
	}

	public WindowEntry(int is, int ie, int se){
		indexS = is; 
		indexE = ie;
		seq = se;
		counter = 0;
		acked = false;
	}

	//Public Variable
	public int indexS;
	public int indexE;
	public int counter;
	public boolean acked;
	public int seq;
}
