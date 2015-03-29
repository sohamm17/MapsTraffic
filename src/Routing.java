import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
	
	public RouteInfo getRoute(String source_string, String destination_string, double walkSpeed, String formattedDate) throws URISyntaxException
	{
		RouteInfo.maneuvar.latlon src_latlon, dst_latlon;
		
		//Getting latitude and longitude of source and destination
		src_latlon = getLatLong(source_string);
		dst_latlon = getLatLong(destination_string);
		
		System.out.println(src_latlon);
		System.out.println(dst_latlon);
		
		//Calculating the route
		String url = "http://route.cit.api.here.com/routing/7.2/calculateroute.json";
		
	
		URIBuilder builder = new URIBuilder(url)
		.addParameter("app_id", APP_ID)
		.addParameter("app_code", APP_CODE)
		.addParameter("waypoint0", "geo!" + src_latlon.toString())
		.addParameter("waypoint1", "geo!" + dst_latlon.toString())
		.addParameter("mode", "fastest;publicTransportTimeTable") //Getting public routes with the fastest options
		.addParameter("combineChange", "true")
		.addParameter("alternatives", "5") //getting 2 alternative routes
		.addParameter("walkSpeed", String.valueOf(walkSpeed)) //allowed value between 0.5 to 2
		.addParameter("departure", formattedDate)
		.addParameter("instructionformat", "text")
		.addParameter("maneuverAttributes", "trafficTime,waitTime,publicTransportLine,time,roadName,length,link,direction")
		.addParameter("legAttributes", "links,trafficTime,waypoint")
		.addParameter("linkAttributes", "roadName,maneuver,length,nextStopName,dynamicSpeedInfo,speedLimit,nextLink")
		.addParameter("routeAttributes", "lines")
		.addParameter("metricSystem", "metric")
		;
		
		System.out.println(builder.build());
		return parseRoutingResponse(builder);
	}
	
	public RouteInfo parseRoutingResponse(URIBuilder uri) throws URISyntaxException
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
	            	
	            	//Parsing the time
	            	String timeString = manvrObj.getJsonString("time").getString();
	            	DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH);
	            	Date dateTime = format.parse(timeString);
	            	eachManeuvar.time = dateTime;
	            	
	            	if(eachManeuvar.type.compareTo(PUBLICManevar) == 0)//Found a public maneuvar
	            	{
	            		JsonString temp = manvrObj.getJsonString("stopName");
	            		if(temp != null)
	            			eachManeuvar.stopName = temp.getString();
	            		
	            		//Getting the busNumber of the public transport from publicTransportLines
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
	            return route;
	            
	        }
	        catch (Exception e)
	        {
	        	System.err.println("Error: " + e.getMessage());
	        	e.printStackTrace();
	        	return null;
	        }
		}
		catch (IOException ex) 
        {
			System.err.println("Error: " + ex.getMessage());
			return null;
        }
	}
	
	//Given a location it gets the latitude and longitude and returns them through the array latlong
	//lat is at 0th index, lon is at 1st.
	public static RouteInfo.maneuvar.latlon getLatLong(String location) throws URISyntaxException
	{
		String url = "http://geocoder.cit.api.here.com/6.2/geocode.json";
		RouteInfo.maneuvar.latlon latlon = null;
		
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
	            latlon = new RouteInfo.maneuvar.latlon(displayPosition.getJsonNumber("Latitude").doubleValue(), 
	            		displayPosition.getJsonNumber("Longitude").doubleValue());
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
		return latlon;
	}
}
