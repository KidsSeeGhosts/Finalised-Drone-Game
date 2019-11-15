package uk.ac.ed.inf.powergrab;

public class StatefulDrone extends Drone{
	
	
	public StatefulDrone(Position startposition, int userSeed, String mapSource,String newFileName) {
		this.startposition=startposition;
		this.userSeed=userSeed;
		this.power=250;
		this.mapSource=mapSource;
		this.newFileName=newFileName;
	}
	
	public void run() {
		setUpDrone();
		while (power>0 && moves<=250) {
			
		}
		
		
		
		
		
		
		writer.close();//at this point the stateless drone's journey is complete
		makeNewGeoJson();
	}
	
	
}
	
	
	
	
	
	
	
	