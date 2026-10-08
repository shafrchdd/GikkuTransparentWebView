package com.gikku.transparentwebview;
import android.net.Uri;
import java.util.regex.*;
public final class GikkuSharedLocationParser {
 private GikkuSharedLocationParser(){}
 public static String parse(String text){
  if(text==null)return "";
  try {
   Matcher urls=Pattern.compile("https?://[^\\s<>]+").matcher(text);
   if(!urls.find())return coordinates(text);
   String link=urls.group().replaceAll("[),.]+$","");
   Uri u=Uri.parse(link);
   String host=u.getHost();
   if(host==null)return "";
   host=host.toLowerCase(java.util.Locale.US);
   if(host.equals("maps.app.goo.gl")||host.equals("goo.gl"))return "";
   if(!(host.equals("google.com")||host.endsWith(".google.com")))return "";
   for(String key:new String[]{"destination","query","q","daddr"}){
    String v=u.getQueryParameter(key);
    if(v!=null&&!v.trim().isEmpty())return clean(v);
   }
   Matcher m=Pattern.compile("!3d(-?\\d+(?:\\.\\d+)?)!4d(-?\\d+(?:\\.\\d+)?)").matcher(link);
   if(m.find())return m.group(1)+","+m.group(2);
   String path=u.getPath();
   if(path!=null){
    m=Pattern.compile("/place/([^/]+)").matcher(path);
    if(m.find())return Uri.decode(m.group(1)).replace('+',' ');
   }
   return "";
  }catch(Exception e){return "";}
 }
 private static String clean(String v){
  String c=coordinates(v);
  if(!c.isEmpty())return c;
  if(v.startsWith("http"))return "";
  return v.trim().length()>250?"":v.trim();
 }
 private static String coordinates(String v){
  Matcher m=Pattern.compile("(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)").matcher(v);
  if(m.find()){
   double lat=Double.parseDouble(m.group(1)),lon=Double.parseDouble(m.group(2));
   if(Math.abs(lat)<=90&&Math.abs(lon)<=180)return lat+","+lon;
  }
  return "";
 }
}
