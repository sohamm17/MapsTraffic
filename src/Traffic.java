import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Arrays;

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
	
	public double getAverageSpeed(double bb_left_lat, double bb_left_lon, double bb_right_lat, double bb_right_lon,
			String flowRoadName, String direction, String[] crossingRoads) throws URISyntaxException
	{
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
			double avgSpd = 0;
			int avgSpdCountSections = 0;//counts the number of sections (/crossing roads) for which avergae speed is being counted
			
			String boundingBox = bb_left_lat + "," + bb_left_lon + ";" + bb_right_lat + "," + bb_right_lon;
			String url = "http://traffic.cit.api.here.com/traffic/6.1/flow.json";
			
			URIBuilder builder = new URIBuilder(url)
				.addParameter("app_id", APP_ID)
				.addParameter("app_code", APP_CODE)
				.addParameter("bbox", boundingBox);
			
            HttpGet request = new HttpGet(builder.build());
            
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);

            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            System.out.println(json);
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
                		                		
                		String eachRoadName = eachRoadObject.getString("DE");
                		
                		//If query roadName matches the road description of this node
                		if(eachRoadName.contains(flowRoadName))
                		{
	                		JsonArray flowItemElements = eachRoadObject.getJsonArray("FIS");
	                		
	                		for(JsonValue eachFlowItemElement : flowItemElements)
	                		{
	                			JsonArray singleFlowItems = ((JsonObject)eachFlowItemElement).getJsonArray("FI");
	                			
	                			for(JsonValue eachSingleFlowItem: singleFlowItems)
	                			{
	                				JsonObject singleFlowTMC = ((JsonObject)eachSingleFlowItem).getJsonObject("TMC");
	                				
	                				String FIDirection = singleFlowTMC.getString("QD");
	                				if (FIDirection.contains(direction))
	                				{
	                					String crossRoad = singleFlowTMC.getString("DE");
	                					
	                					if (contains(crossingRoads, crossRoad))
	                					{
			                				System.out.print(singleFlowTMC.getString("DE") + "-" + singleFlowTMC.getJsonNumber("LE") + " miles");
			                						                				
			                				JsonArray currentFlows = ((JsonObject)eachSingleFlowItem).getJsonArray("CF");
			                				
			                				for(JsonValue eachCurrentFlow : currentFlows) //Run only one time generally
			                				{
			                					JsonObject eachCurrentFlowObject = ((JsonObject)eachCurrentFlow);
			                					System.out.println(" --> FreeFlow: " + eachCurrentFlowObject.getJsonNumber("FF")
			                							+ ", Speed(by limit): " + eachCurrentFlowObject.getJsonNumber("SP")
			                							+ ", Speed(not limit): " + eachCurrentFlowObject.getJsonNumber("SU")
			                							+ ", Jam Factor: " + eachCurrentFlowObject.getJsonNumber("JF")
			                							+ ", Confidence: " + eachCurrentFlowObject.getJsonNumber("CN")
			                							);
			                					avgSpd += eachCurrentFlowObject.getJsonNumber("SP").doubleValue();
			                					avgSpdCountSections++;
			                				}
	                					}
	                				}
	                			}
	                		}
	                		System.out.println();
                		}
                	}
                }
                
                return avgSpd / avgSpdCountSections;
            } 
            catch (Exception e)
            {
            	System.err.println("Error: " + e.getMessage());
            	e.printStackTrace();
            	return -1;
            }

        }
		catch (IOException ex) 
        {
			return -1;
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
