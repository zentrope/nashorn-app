package com.bobo.nashorn;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.net.ssl.HttpsURLConnection;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.PersistentArrayMap;

public final class Functions {

  public static String lookup(String name) throws IllegalArgumentException {

    IFn deref = Clojure.var("clojure.core", "deref");
    IFn get = Clojure.var("clojure.core", "get");
    IFn envFind = Clojure.var("nashorn.server.db", "property-find");

    Object config = deref.invoke(Clojure.var("nashorn.server.main", "config"));
    Object dbRef = get.invoke(config, Clojure.read(":svc/db"));
    Object result = envFind.invoke(dbRef, name);
    Object value = get.invoke(result, Clojure.read(":value"));

    if (value == null) {
      throw new IllegalArgumentException(format("No value for '%s' in environment.", name));
    }

    return (String)value;
  }

  public static String lookup(String name, String defaultValue) {
    try {
      return lookup(name);
    }
    catch (IllegalArgumentException e) {
      return defaultValue;
    }
  }

  public static String format(String fmt, Object ...args) {
    return String.format(fmt, args);
  }

  public static String httpGet(String urlString) throws Exception {
    return httpGet(urlString, null);
  }

  public static String httpGet(String urlString, ScriptObjectMirror headers) throws Exception {
    try {
      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", "bobo/nashorn");

      if (headers != null) {
        for (String header : headers.keySet()) {
          String value = (String)headers.get(header);
          System.out.println("Setting header:" + header + " : " + value);
          conn.setRequestProperty(header, value);
        }
      }
      return readString(conn.getInputStream());
    }
    catch (Throwable t) {
      System.out.printf("ERROR: %s\n", t);
      throw t;
    }
  }

  public static Map<String, String> httpPost(String urlString, ScriptObjectMirror headers, String data) throws Exception {
    try {
      URL url = new URL(urlString);
      HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("User-Agent", "bobo/nashorn");

      for (String header : headers.keySet()) {
        String value = (String)headers.get(header);
        conn.setRequestProperty(header, value);
      }

      conn.setDoOutput(true);

      DataOutputStream out = new DataOutputStream(conn.getOutputStream());
      out.writeBytes(data);
      out.flush();
      out.close();

      Map<String, String> response = new HashMap<String, String>();
      response.put("status", conn.getResponseCode()+"");
      response.put("body", readString(conn.getInputStream()));
      return response;
    }
    catch (Throwable t) {
      System.out.printf("ERROR: %s\n", t);
      throw t;
    }
  }

  private static String readString(InputStream is) {
    try {
      Scanner scanner = new Scanner(is).useDelimiter("\\A");
      return scanner.hasNext() ? scanner.next() : "";
    }

    finally {
      try {
        is.close();
      }
      catch (Throwable t) {
      }
    }
  }

}
