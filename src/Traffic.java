import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class Traffic
{
	String APP_ID = "DemoAppId01082013GAL";
	String APP_CODE = "AJKnXv84fjrb0KIHawS0Tg";
	
	public ArrayList<mainRoadway> roadways = new ArrayList<>();
	
	public RouteInfo.maneuvar.latlon bbLatLon1 = null;
	public RouteInfo.maneuvar.latlon bbLatLon2 = null;
	
	public String toString()
	{
		StringBuilder ret_string = new StringBuilder();
		for(mainRoadway eachRW: roadways)
		{
			ret_string.append(eachRW + "\n");
		}
		return ret_string.toString();
	}
	
	public Traffic(RouteInfo.maneuvar.latlon bbLatLon1, RouteInfo.maneuvar.latlon bbLatlon2) throws URISyntaxException
	{
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
			String boundingBox = bbLatLon1.toString() + ";" + bbLatlon2.toString();
			String url = "http://traffic.cit.api.here.com/traffic/6.1/flow.json";
			
			URIBuilder builder = new URIBuilder(url)
				.addParameter("app_id", APP_ID)
				.addParameter("app_code", APP_CODE)
				.addParameter("bbox", boundingBox)
				.addParameter("metricSystem", "metric");
			System.out.println(builder.build());
			
            HttpGet request = new HttpGet(builder.build());
            
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);

            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            
            try
            {
            	StringReader reader = new StringReader(json);
                JsonReader jsonReader = Json.createReader(reader);
                JsonObject wholeObject = jsonReader.readObject();
                
                JsonArray RoadWays = (JsonArray) wholeObject.get("RWS");
                
                for(JsonValue eachRWS : RoadWays)
                {
                	JsonObject eachRWSObject = (JsonObject) eachRWS;
                	JsonArray Roads = eachRWSObject.getJsonArray("RW");
                	
                	for(JsonValue eachRoad : Roads)
                	{
                		JsonObject eachRoadObject = (JsonObject) eachRoad;
                		mainRoadway rw = new mainRoadway();
                		roadways.add(rw);
                		
                		String eachRoadName = eachRoadObject.getJsonString("DE").getString();
                		rw.description = eachRoadName;
                		
                		JsonArray flowItemElements = eachRoadObject.getJsonArray("FIS");
                		
                		for(JsonValue eachFlowItemElement : flowItemElements) //Generally run one time
                		{
                			JsonArray singleFlowItems = ((JsonObject)eachFlowItemElement).getJsonArray("FI");
                			
                			for(JsonValue eachSingleFlowItem: singleFlowItems)
                			{
                				mainRoadway.crossFlow cf = new mainRoadway.crossFlow();
                				rw.crossingRoads.add(cf);
                				
                				JsonObject singleFlowTMC = ((JsonObject)eachSingleFlowItem).getJsonObject("TMC");
                				
            					cf.description = singleFlowTMC.getJsonString("DE").getString();
            					cf.direction = singleFlowTMC.getJsonString("QD").getString();
            					
                				//System.out.print(singleFlowTMC.getString("DE") + "-" + singleFlowTMC.getJsonNumber("LE") + " meter");
                						                				
                				JsonArray currentFlows = ((JsonObject)eachSingleFlowItem).getJsonArray("CF");
                				
                				for(JsonValue eachCurrentFlow : currentFlows) //Run only one time generally
                				{
                					JsonObject eachCurrentFlowObject = ((JsonObject)eachCurrentFlow);
                					cf.jamFactor = eachCurrentFlowObject.getJsonNumber("JF").doubleValue();
                					cf.confidence = eachCurrentFlowObject.getJsonNumber("CN").doubleValue();
                					cf.freeFlowSpeed = eachCurrentFlowObject.getJsonNumber("FF").doubleValue();
                					cf.speedUncapped = eachCurrentFlowObject.getJsonNumber("SU").doubleValue();
                					cf.speedCapped = eachCurrentFlowObject.getJsonNumber("SP").doubleValue();
                					
//                					System.out.println(" --> FreeFlow: " + eachCurrentFlowObject.getJsonNumber("FF")
//                							+ ", Speed(by limit): " + eachCurrentFlowObject.getJsonNumber("SP")
//                							+ ", Speed(not limit): " + eachCurrentFlowObject.getJsonNumber("SU")
//                							+ ", Jam Factor: " + eachCurrentFlowObject.getJsonNumber("JF")
//                							+ ", Confidence: " + eachCurrentFlowObject.getJsonNumber("CN")
//                							);
                				}
                			}
                		}
//                		System.out.println();
                	}
                }
                this.bbLatLon1 = bbLatLon1;
        		this.bbLatLon2 = bbLatlon2;
            } 
            catch (Exception e)
            {
            	System.err.println("Error: " + e.getMessage());
            	e.printStackTrace();
            }	
		}
		catch (IOException e) 
        {
			System.err.println("Error: " + e.getMessage());
        	e.printStackTrace();
        }
	}
	
	public static class mainRoadway
	{
		String description;
		public ArrayList<crossFlow> crossingRoads = new ArrayList<>();
		
		public String toString()
		{
			StringBuilder ret_string = new StringBuilder();
			ret_string.append(description + "\n");
			for(crossFlow eachCrossRD: crossingRoads)
			{
				ret_string.append(eachCrossRD + "\n");
			}
			return ret_string.toString();
		}
		
		public static class crossFlow
		{
			public String description;
			public String direction;
			public double length; //In meters
			public double confidence;
			public double jamFactor;
			public double freeFlowSpeed; //FF - free flow speed
			public double speedCapped; //SP - Not used
			public double speedUncapped; //SU - In traffic speed
			
			public String toString()
			{
				return description + " --> FreeFlow: " + freeFlowSpeed + "km/h, Speed(In Traffic): " + speedUncapped
						+ ", Jam Factor: " + jamFactor + ", Confidence:" + confidence + ", Dir: " + direction;
			}
		}
		
	}
	
	public double getAverageSpeedInTraffic(String flowRoadName, String direction, String[] crossingRoads)
	{
		double totalSpd = 0;
		int crossRoadCount = 0;//counts the number of sections (/crossing roads) for which avergae speed is being counted
		for(mainRoadway eachRW: roadways)
		{
			if(getAvenueOrStreetNumber(eachRW.description) == getAvenueOrStreetNumber(flowRoadName)) //Found the main roadway on which average speed in traffic
				//is being calculated
			{
				for(mainRoadway.crossFlow eachCF: eachRW.crossingRoads)
				{
					//if the this crossFlow description matches with any of the queried crossing roads
					//and if the direction matches, then consider the speed
					//if(contains(crossingRoads, eachCF.description))
					//System.out.println(eachCF.description);
					int CFNumber = getAvenueOrStreetNumber(eachCF.description);
					if(getAvenueOrStreetNumber(crossingRoads[0]) < CFNumber)
					{
						crossRoadCount++;
						totalSpd += eachCF.speedUncapped; //Getting speed uncapped of the road to get average speed in traffic
						//System.out.println(getAvenueOrStreetNumber(eachCF.description));
						break;
					}
				}
				break;
			}
		}
		
		if(crossRoadCount > 0)
			return (totalSpd/crossRoadCount);
		else
			return -1;
	}
	
	public static int getAvenueOrStreetNumber(String roadName)
	{
		if(roadName.replaceAll("[\\D]", "").compareTo("") == 0)
			return -1;
		return Integer.parseInt(roadName.replaceAll("[\\D]", ""));
	}
	
	public double getFreeFlowSpeed(String flowRoadName, String direction, String[] crossingRoads)
	{
		double totalSpd = 0;
		int crossRoadCount = 0;//counts the number of sections (/crossing roads) for which avergae speed is being counted
		for(mainRoadway eachRW: roadways)
		{
			if(eachRW.description.contains(flowRoadName)) //Found the main roadway on which average speed in traffic
				//is being calculated
			{
				for(mainRoadway.crossFlow eachCF: eachRW.crossingRoads)
				{
					//if the this crossFlow description matches with any of the queried crossing roads
					//and if the direction matches, then consider the speed
					if(contains(crossingRoads, eachCF.description) && eachCF.direction.compareTo(direction) == 0)
					{
						crossRoadCount++;
						totalSpd += eachCF.freeFlowSpeed;
					}
				}
				break;
			}
		}
		
		if(crossRoadCount > 0)
			return (totalSpd/crossRoadCount);
		else
			return -1;
	}
	
	//Print all the crossing roads and the traffic information between them
	public void printCrossRoadInfo(String flowRoadName, String direction, String[] crossingRoads)
	{
		for(mainRoadway eachRW: roadways)
		{
			if(eachRW.description.contains(flowRoadName)) //Found the main roadway on which average speed in traffic
				//is being calculated
			{
				for(mainRoadway.crossFlow eachCF: eachRW.crossingRoads)
				{
					//if the this crossFlow description matches with any of the queried crossing roads
					//and if the direction matches, then consider the speed
					if(contains(crossingRoads, eachCF.description) && eachCF.direction.compareTo(direction) == 0)
					{
						System.out.println(eachCF);
					}
				}
				break;
			}
		}
	}
	
	//if the word contains any string in the list it returns true
	private boolean contains(String[] list, String word)
	{
		for(String eachString: list)
		{
			if(word.contains(eachString))
				return true;
		}
		return false;
	}
}