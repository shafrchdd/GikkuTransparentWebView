package com.gikku.transparentwebview;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.location.*;
import android.os.Bundle;
import android.os.Looper;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
public class GikkuSmartNavigation extends AndroidNonvisibleComponent implements LocationListener {
 private final Activity activity; private LocationManager lm;
 private String apiKey="",destination=""; private ArrayList<String> stops=new ArrayList<>();
 private double lat=0,lng=0,speed=0,fuel=100; private boolean located=false;
 private double schoolRadius=250,hospitalRadius=250,fuelRadius=3000,speedLimit=30,lowFuel=20;
 private long lastNearby=0;
 public GikkuSmartNavigation(ComponentContainer c){super(c.$form());activity=c.$context();lm=(LocationManager)activity.getSystemService(Activity.LOCATION_SERVICE);}
 @SimpleFunction public void SetGoogleApiKey(String key){apiKey=key.trim();}
 @SimpleFunction public void SetFuelLevel(double percent){fuel=percent;if(fuel<=lowFuel)SafetyWarning("FUEL","Fuel below "+lowFuel+"%");}
 @SimpleFunction public void ConfigureWarnings(double schoolMeters,double hospitalMeters,double petrolMeters,double speedKmh,double fuelPercent){schoolRadius=schoolMeters;hospitalRadius=hospitalMeters;fuelRadius=petrolMeters;speedLimit=speedKmh;lowFuel=fuelPercent;}
 @SimpleFunction public void StartLocation(){try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,2,this,Looper.getMainLooper());}catch(SecurityException e){NavigationError("Grant location permission: "+e.getMessage());}catch(Exception e){NavigationError(e.toString());}}
 @SimpleFunction public void StopLocation(){try{lm.removeUpdates(this);}catch(Exception ignored){}}
 @Override public void onLocationChanged(Location l){lat=l.getLatitude();lng=l.getLongitude();located=true;speed=l.hasSpeed()?Math.max(0,l.getSpeed()*3.6):0;LocationUpdated(lat,lng,speed,l.getAccuracy());if(System.currentTimeMillis()-lastNearby>45000){lastNearby=System.currentTimeMillis();if(!apiKey.isEmpty()){checkNearby("school",schoolRadius);checkNearby("hospital",hospitalRadius);if(fuel<=lowFuel)checkNearby("gas_station",fuelRadius);}}}
 @SimpleEvent public void LocationUpdated(double latitude,double longitude,double speedKmh,float accuracyMeters){EventDispatcher.dispatchEvent(this,"LocationUpdated",latitude,longitude,speedKmh,accuracyMeters);}
 @SimpleEvent public void SearchResults(String json){EventDispatcher.dispatchEvent(this,"SearchResults",json);}
 @SimpleEvent public void RouteReady(String json){EventDispatcher.dispatchEvent(this,"RouteReady",json);}
 @SimpleEvent public void NearbyPlace(String type,String name,double distanceMeters){EventDispatcher.dispatchEvent(this,"NearbyPlace",type,name,distanceMeters);}
 @SimpleEvent public void SafetyWarning(String type,String message){EventDispatcher.dispatchEvent(this,"SafetyWarning",type,message);}
 @SimpleEvent public void NavigationError(String message){EventDispatcher.dispatchEvent(this,"NavigationError",message);}
 @SimpleFunction public double CurrentSpeed(){return speed;}
 @SimpleFunction public double Latitude(){return lat;}
 @SimpleFunction public double Longitude(){return lng;}
 @SimpleFunction public void SetDestination(String placeIdOrCoordinates){destination=placeIdOrCoordinates;}
 @SimpleFunction public void AddStop(String placeIdOrCoordinates){stops.add(placeIdOrCoordinates);}
 @SimpleFunction public void RemoveStop(int index){if(index>=1&&index<=stops.size())stops.remove(index-1);}
 @SimpleFunction public void ClearStops(){stops.clear();}
 @SimpleFunction public String TripStops(){return new JSONArray(stops).toString();}
 @SimpleFunction public void OpenGoogleMaps(){if(destination.isEmpty()){NavigationError("Choose a destination");return;}try{StringBuilder u=new StringBuilder("https://www.google.com/maps/dir/?api=1&destination="+enc(destination)+"&travelmode=driving");if(located)u.append("&origin=").append(lat).append(",").append(lng);if(!stops.isEmpty()){StringJoiner j=new StringJoiner("|");for(String s:stops)j.add(s);u.append("&waypoints=").append(enc(j.toString()));}Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(u.toString()));activity.startActivity(i);}catch(Exception e){NavigationError(e.toString());}}
 private String enc(String s){try{return java.net.URLEncoder.encode(s,"UTF-8");}catch(Exception e){return s;}}
 private void request(String url,String method,String body,String fieldMask,java.util.function.Consumer<String> done){new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod(method);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestProperty("X-Goog-Api-Key",apiKey);if(fieldMask!=null)c.setRequestProperty("X-Goog-FieldMask",fieldMask);if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream os=c.getOutputStream()){os.write(body.getBytes("UTF-8"));}}InputStream stream=(c.getResponseCode()<400?c.getInputStream():c.getErrorStream());ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=stream.read(buf))!=-1)b.write(buf,0,n);String response=b.toString("UTF-8");if(c.getResponseCode()>=400)throw new IOException(response);activity.runOnUiThread(()->done.accept(response));}catch(Exception e){activity.runOnUiThread(()->NavigationError(e.toString()));}}).start();}
 @SimpleFunction public void SearchPlace(String query){if(apiKey.isEmpty()){NavigationError("Google API key missing");return;}try{JSONObject b=new JSONObject().put("textQuery",query);request("https://places.googleapis.com/v1/places:searchText","POST",b.toString(),"places.id,places.displayName,places.formattedAddress,places.location",this::SearchResults);}catch(Exception e){NavigationError(e.toString());}}
 @SimpleFunction public void CalculateRoute(){if(apiKey.isEmpty()||!located||destination.isEmpty()){NavigationError("API key, GPS fix and destination required");return;}try{JSONObject body=new JSONObject();body.put("origin",new JSONObject().put("location",new JSONObject().put("latLng",new JSONObject().put("latitude",lat).put("longitude",lng))));body.put("destination",waypoint(destination));JSONArray arr=new JSONArray();for(String s:stops)arr.put(waypoint(s));if(arr.length()>0)body.put("intermediates",arr);body.put("travelMode","DRIVE");body.put("routingPreference","TRAFFIC_AWARE");body.put("computeAlternativeRoutes",false);request("https://routes.googleapis.com/directions/v2:computeRoutes","POST",body.toString(),"routes.duration,routes.staticDuration,routes.distanceMeters,routes.legs.steps.navigationInstruction,routes.legs.steps.distanceMeters,routes.legs.steps.startLocation,routes.legs.steps.endLocation",this::RouteReady);}catch(Exception e){NavigationError(e.toString());}}
 private JSONObject waypoint(String s)throws Exception{if(s.startsWith("ChIJ")||s.startsWith("places/"))return new JSONObject().put("placeId",s.replace("places/",""));String[] v=s.split(",");if(v.length==2){try{return new JSONObject().put("location",new JSONObject().put("latLng",new JSONObject().put("latitude",Double.parseDouble(v[0].trim())).put("longitude",Double.parseDouble(v[1].trim()))));}catch(Exception ignored){}}return new JSONObject().put("address",s);}
 private void checkNearby(String type,double radius){try{JSONObject b=new JSONObject().put("includedTypes",new JSONArray().put(type)).put("maxResultCount",5).put("locationRestriction",new JSONObject().put("circle",new JSONObject().put("center",new JSONObject().put("latitude",lat).put("longitude",lng)).put("radius",radius)));request("https://places.googleapis.com/v1/places:searchNearby","POST",b.toString(),"places.displayName,places.location",s->{try{JSONArray places=new JSONObject(s).optJSONArray("places");if(places==null)return;for(int i=0;i<places.length();i++){JSONObject p=places.getJSONObject(i),l=p.getJSONObject("location");float[] dist=new float[1];Location.distanceBetween(lat,lng,l.getDouble("latitude"),l.getDouble("longitude"),dist);String name=p.optJSONObject("displayName").optString("text");NearbyPlace(type,name,dist[0]);if((type.equals("school")||type.equals("hospital"))&&speed>speedLimit)SafetyWarning(type,"Slow down near "+name);if(type.equals("gas_station")&&fuel<=lowFuel)SafetyWarning("FUEL","Low fuel: nearby "+name);}}catch(Exception e){NavigationError(e.toString());}});}catch(Exception e){NavigationError(e.toString());}}
}
