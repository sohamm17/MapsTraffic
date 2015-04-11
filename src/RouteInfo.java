import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class RouteInfo
{
	public ArrayList<maneuvar> maneuvars = new ArrayList<>();
	public long travelTime;
	public long length;
	
	public Date getReachingTime()
	{
		maneuvar lastMan = maneuvars.get(maneuvars.size() - 1);
		return lastMan.time;
	}
	
	public void modifyManeuvarTime(maneuvar man, long modifiedTime)
	{
		maneuvar targetMan = null; int i = 0;
		for(maneuvar eachMan : maneuvars)
		{
			if(eachMan.isEqual(man))
				break;
			i++;
		}
		modifyManeuvarTime(i, modifiedTime);
	}
	
	public void modifyManeuvarTime(int maneuvarIndex, long modifiedTime)
	{
		long delay = modifiedTime - this.maneuvars.get(maneuvarIndex).travelTime;
		this.maneuvars.get(maneuvarIndex).travelTime += delay;
		
		for(int i = maneuvarIndex + 1; i < maneuvars.size(); i++)
		{
			maneuvar thisManeuvar = this.maneuvars.get(i);
			
			Calendar modifiedCalendar = DateToCalendar(thisManeuvar.time);
			modifiedCalendar.add(Calendar.SECOND, (int) delay);
			thisManeuvar.time = modifiedCalendar.getTime();
		}
		this.travelTime += delay;
	}
	
	public String toString()
	{
		StringBuilder ret_string = new StringBuilder();
		for(RouteInfo.maneuvar eachMan: this.maneuvars)
        {
        	ret_string.append(eachMan.toString() + "\n") ;
        }
		ret_string.append(
				"Length of route: " + length + "m, time: " + travelTime + "sec\n"
				);
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
				return nextStopName + " " + length + "m";
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
		
		public boolean isEqual(maneuvar man)
		{
			return this.id == man.id;
		}
		
		public String toString()
		{
			return /*type + "\t" + */time + "\t" + length + "m\t" + travelTime + "sec\t" 
		+ (busNumber == null ? "" : (busNumber + "\t")) + (roadName == null ? stopName : "") + "\t"
		+ (type.compareTo("PrivateTransportManeuverType") == 0 ? "walking" : "by bus")+ "\n" + instruction + "\n"
		+ (interRoadAndStops.size() == 0 ? "" : (interRoadAndStops + "\n")) ;
		}
	}
	
	public static Calendar DateToCalendar(Date date)
	{ 
		  Calendar cal = Calendar.getInstance();
		  cal.setTime(date);
		  return cal;
	}
}
