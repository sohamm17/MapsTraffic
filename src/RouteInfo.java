import java.util.ArrayList;
import java.util.Date;


public class RouteInfo
{
	public ArrayList<maneuvar> maneuvars = new ArrayList<>();
	
	public String toString()
	{
		StringBuilder ret_string = new StringBuilder();
		for(RouteInfo.maneuvar eachMan: this.maneuvars)
        {
        	ret_string.append(eachMan.toString() + "\n") ;
        }
		return ret_string.toString();
	}
	
	public static class maneuvar
	{		
		public static class latlon
		{
			public double lat, lon;
			public latlon(double lat, double lon)
			{
				this.lat = lat;
				this.lon = lon;
			}
			public String toString()
			{
				return lat + "," + lon;
			}
		}
		
		public static class link
		{
			public String nextStopName; //From the previous stop upto this stop, the length and link information are stored
			public long length;
			public long travelTime = 0; //although hereMaps doesn't give any travel time for links, this is kept for future use
			public String type = null; //Currently doing for only public routes so this is null
			
			public String toString()
			{
				return nextStopName + "\t" + length;
			}
		}
		
		public latlon position;
		public String instruction;
		public long travelTime;
		public long length;
		public Date time; //Not taking the time as of now
		public String direction;
		public String roadName = null; //In case of private maneuvars, this is the road-name on which the public walks
		public String stopName = null; //In case of public maneuvars, this is the stop-name from which a bus starts
		public String id = null;
		public String type;
		public String busNumber = null; //only in case of public transport
		public ArrayList<link> interRoadAndStops  = new ArrayList<>();
		
		public String toString()
		{
			return type + "\t" + time + "\t" + length + "m\t" + travelTime + "sec\t" + 
		(busNumber == null ? "" : (busNumber + "\t")) + (roadName == null ? stopName : roadName) + "\t" + interRoadAndStops + "\n" + instruction +"\n";
		}
	}
}
