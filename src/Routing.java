import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class Routing 
{
	static String APP_ID = "DemoAppId01082013GAL";
	static String APP_CODE = "AJKnXv84fjrb0KIHawS0Tg";
	static String PUBLICManevar = "PublicTransportManeuverType";
	static String PRIVATEManeuvar = "PrivateTransportManeuverType";
	static String PUBLICLink = "PublicTransportLinkType";
	static String PRIVATELink = "PrivateTransportLinkType";
	
	public void getRoute(String source_string, String destination_string, double walkSpeed) throws URISyntaxException
	{
		double[] src_latlon = new double[2], dst_latlon = new double[2];
		
		//Getting latitude and longitude of source and destination
		getLatLong(source_string, src_latlon);
		getLatLong(destination_string, dst_latlon);
		
		System.out.println(src_latlon[0] + "\t" + src_latlon[1]);
		System.out.println(dst_latlon[0] + "\t" + dst_latlon[1]);
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		@SuppressWarnings("deprecation")
		Date date = new Date(115, 02, 26, 17, 30, 00);
		System.out.println(dateFormat.format(date));
		
		//Calculating the route
		String url = "http://route.cit.api.here.com/routing/7.2/calculateroute.json";
		
	
		URIBuilder builder = new URIBuilder(url)
		.addParameter("app_id", APP_ID)
		.addParameter("app_code", APP_CODE)
		.addParameter("waypoint0", "geo!" + getAngleAsString(src_latlon))
		.addParameter("waypoint1", "geo!" + getAngleAsString(dst_latlon))
		.addParameter("mode", "fastest;publicTransportTimeTable") //Getting public routes with the fastest options
		.addParameter("combineChange", "true")
		.addParameter("alternatives", "5") //getting 2 alternative routes
		.addParameter("walkSpeed", String.valueOf(walkSpeed)) //allowed value between 0.5 to 2
		.addParameter("departure", dateFormat.format(date))
		.addParameter("instructionformat", "text")
		.addParameter("maneuverAttributes", "trafficTime,waitTime,publicTransportLine,time,roadName,length,link,direction")
		.addParameter("legAttributes", "links,trafficTime,waypoint")
		.addParameter("linkAttributes", "roadName,maneuver,length,nextStopName,dynamicSpeedInfo,speedLimit,nextLink")
		.addParameter("routeAttributes", "lines")
		.addParameter("metricSystem", "metric")
		;
		
		System.out.println(builder.build());
		parseRoutingResponse(builder);
	}
	
	public void parseRoutingResponse(URIBuilder uri) throws URISyntaxException
	{
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
		
			HttpGet request = new HttpGet(uri.build());
	        
	        request.addHeader("content-type", "application/json");
	        HttpResponse result = httpClient.execute(request);
	
	        String json = EntityUtils.toString(result.getEntity(), "UTF-8");
	        //System.out.println(json);
	        try
	        {
	        	StringReader reader = new StringReader(json);
	            JsonReader jsonReader = Json.createReader(reader);
	            JsonObject wholeObject = jsonReader.readObject();
	            
	            //Getting only the first route option
	            JsonObject firstRoute = (JsonObject) wholeObject.getJsonObject("response").getJsonArray("route").get(0);
	            
	            //Getting only the first leg because legs are between way-points and we only two way-points
	            JsonObject leg = (JsonObject) firstRoute.getJsonArray("leg").get(0);
	            JsonArray publicTransportLines = firstRoute.getJsonArray("publicTransportLine");
	            JsonArray links = leg.getJsonArray("link");
	            JsonArray maneuvers = leg.getJsonArray("maneuver");
	            long length = 0, travelTime = 0;
	            
	            RouteInfo route = new RouteInfo();
	            
	            for(JsonValue eachmanvr: maneuvers)
	            {
	            	JsonObject manvrObj = (JsonObject) eachmanvr;
	            	
	            	RouteInfo.maneuvar eachManeuvar = new RouteInfo.maneuvar();
	            	route.maneuvars.add(eachManeuvar);
	            	
	            	eachManeuvar.id = manvrObj.getJsonString("id").getString();
	            	eachManeuvar.length = manvrObj.getJsonNumber("length").longValue();
	            	eachManeuvar.travelTime = manvrObj.getJsonNumber("travelTime").longValue();
	            	eachManeuvar.position = new RouteInfo.maneuvar.latlon(
	            			manvrObj.getJsonObject("position").getJsonNumber("latitude").longValue(),
	            			manvrObj.getJsonObject("position").getJsonNumber("longitude").longValue()
	            			);
	            	eachManeuvar.instruction = manvrObj.getJsonString("instruction").getString();
	            	eachManeuvar.type = manvrObj.getJsonString("_type").getString();
	            	
	            	if(eachManeuvar.type.compareTo(PUBLICManevar) == 0)//Found a public maneuvar
	            	{
	            		JsonString temp = manvrObj.getJsonString("stopName");
	            		if(temp != null)
	            			eachManeuvar.stopName = temp.getString();
	            		
	            		//Getting the busNumber of the public transport
	            		temp = manvrObj.getJsonString("line");
	            		if(temp != null)
	            		{
	            			String tempLineID = temp.getString();
	            			for(JsonValue eachPublicLine: publicTransportLines)
	            			{
	            				JsonString tempID = ((JsonObject)eachPublicLine).getJsonString("id");
	            				if(tempID != null && tempLineID.compareTo(tempID.getString()) == 0)
	            				{
	            					JsonString tempBNumber = ((JsonObject)eachPublicLine).getJsonString("lineName");
	            					if(tempBNumber != null)
	            						eachManeuvar.busNumber = tempBNumber.getString();
	            				}
	            			}
	            		}
	            		
	            		//Getting the links associated with a public route
	            		for(JsonValue eachLink: links)
	            		{
	            			JsonObject eachLinkObj = (JsonObject) eachLink;
	            			//If the maneuvar field in link matches with the id of this maneuvar, then we want to get this link
	            			if(eachLinkObj.getJsonString("maneuver") != null 
	            					&& eachLinkObj.getJsonString("maneuver").getString().compareTo(eachManeuvar.id) == 0)
	            			{
	            				RouteInfo.maneuvar.link roadBetween = new RouteInfo.maneuvar.link();
	            				eachManeuvar.interRoadAndStops.add(roadBetween);
	            				
	            				roadBetween.length = eachLinkObj.getJsonNumber("length").longValue();
	            				roadBetween.nextStopName = eachLinkObj.getJsonString("nextStopName").getString();
	            			}
            			}
	            	}
	            	else //assumed as private maneuvar
	            	{
	            		JsonString temp = manvrObj.getJsonString("direction");
	            		if(temp != null)
	            			eachManeuvar.direction = temp.getString();
	            		
	            		temp = manvrObj.getJsonString("roadName");
	            		if(temp != null)
	            			eachManeuvar.roadName = temp.getString();
	            	}
	            	
//	            	length += manvrObj.getJsonNumber("length").longValue();
//	            	travelTime += manvrObj.getJsonNumber("travelTime").longValue();
//	            	System.out.println(manvrObj.getString("id") + " : " + manvrObj.getJsonNumber("travelTime") + " , " 
//	            	+ manvrObj.getJsonNumber("length"));
	            }
	            
	            for(RouteInfo.maneuvar eachMan: route.maneuvars)
	            {
	            	System.out.println(eachMan);
	            }
	            
//	            String tempManvrID = "";
//	            length = 0; travelTime = 0;
//	            for(JsonValue eachLink: links)
//	            {
//	            	JsonObject eachLinkObj = (JsonObject) eachLink;
//	            	//System.out.println(eachLinkObj);
//	            	if(tempManvrID.compareTo(eachLinkObj.getJsonString("maneuver").getString()) != 0)
//	            	{
//	            		System.out.println(tempManvrID + " : " + travelTime + " , " + length);
//	            		tempManvrID = eachLinkObj.getJsonString("maneuver").getString();
//	            		length = 0;
//	            		travelTime = 0;
//	            	}
//	            	length += eachLinkObj.getJsonNumber("length").longValue();
//	            	switch(eachLinkObj.getJsonString("_type").getString())
//	            	{
//		            	case "PrivateTransportLinkType":
//		            		travelTime += eachLinkObj.getJsonObject("dynamicSpeedInfo").getJsonNumber("baseTime").longValue();
//		            		break;
//		            	case "PublicTransportLinkType":
//		            		break;
//		            	default:
//		            		;
//	            	}
//	            }
//	            System.out.println(tempManvrID + " : " + travelTime + " , " + length);
	            
	            System.out.println("Length of route: " + leg.getJsonNumber("length") + "m, time: " + leg.getJsonNumber("travelTime"));
	            
	        }
	        catch (Exception e)
	        {
	        	System.err.println("Error: " + e.getMessage());
	        	e.printStackTrace();
	        }
		}
		catch (IOException ex) 
        {
			System.err.println("Error: " + ex.getMessage());
        }
	}
	
	//Given a location it gets the latitude and longitude and returns them through the array latlong
	//lat is at 0th index, lon is at 1st.
	public static void getLatLong(String location, double[] latlon) throws URISyntaxException
	{
		String url = "http://geocoder.cit.api.here.com/6.2/geocode.json";
		
		URIBuilder builder = new URIBuilder(url)
			.addParameter("app_id", APP_ID)
			.addParameter("app_code", APP_CODE)
			.addParameter("gen", "8")
			.addParameter("searchtext", location)
			;
		
		System.out.println(builder.build());
		
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build())
		{
		
			HttpGet request = new HttpGet(builder.build());
	        
	        request.addHeader("content-type", "application/json");
	        HttpResponse result = httpClient.execute(request);
	
	        String json = EntityUtils.toString(result.getEntity(), "UTF-8");
	        //System.out.println(json);
	        try
	        {
	        	StringReader reader = new StringReader(json);
	            JsonReader jsonReader = Json.createReader(reader);
	            JsonObject wholeObject = jsonReader.readObject();
	            
	            JsonObject firstView = (JsonObject) wholeObject.getJsonObject("Response").getJsonArray("View").get(0);
	            
	            JsonObject firstResult = (JsonObject) firstView.getJsonArray("Result").get(0);
	            
	            JsonObject displayPosition = firstResult.getJsonObject("Location").getJsonObject("DisplayPosition");
	            
	            //System.out.println(displayPosition);
	            latlon[0] = displayPosition.getJsonNumber("Latitude").doubleValue();
	            latlon[1] = displayPosition.getJsonNumber("Longitude").doubleValue();
	        }
	        catch (Exception e)
	        {
	        	System.err.println("Error: " + e.getMessage());
	        	e.printStackTrace();
	        }
		}
		catch (IOException ex) 
        {
			System.err.println("Error: " + ex.getMessage());
        }
	}
	
	//Getting latitude or longitude as string form i.e separated by commas
	private static String getAngleAsString(double[] angle)
	{
		return angle[0] + "," + angle[1];
	}
}
