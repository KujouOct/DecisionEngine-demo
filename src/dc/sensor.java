package dc;

public class sensor {
	String name="";
	double weight=60.0;//50.0~100.0; 0.0 for broken; 
	double worstweight = 50.0;
	double distance=0.0, detected=0.0,refTime=0.0,refreshCycle=0.0;//µ¥Î»ns	
	
	public sensor() {
	}
	
	public sensor(String iname, double dis) {
		name = iname;
		System.out.print("\n"+" Find sensor:  "+ name);
		distance = dis;
	}
	
	public void setDetected(double a) {
		distance = a;
	}
	
	public void setWeight(double a) {
		weight = a;
	}
	
	public void setSensor(double iweight,double iworstweight,double irefreshCycle) {
		weight = iweight;
		worstweight = iworstweight;
		refreshCycle = irefreshCycle;
	}
}
