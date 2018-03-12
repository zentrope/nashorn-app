package com.bobo.nashorn;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONObject;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

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

  public static JSONObject fromJSON(String json) {
    return new JSONObject(json);
  }

  public static Map<String,Object> httpGet(String urlString) throws IOException {
    return httpGet(urlString, null);
  }

  public static Map<String,Object> httpGet(String urlString, Map<String, Object> headers) throws IOException {
    HttpURLConnection conn = doGET(urlString, headers);
    Map<String, Object> response = makeResponse(conn);
    conn.disconnect();
    return response;
  }

  public static Map<String, Object> httpPost(String urlString, Map<String, Object> headers, String data) throws IOException {
    HttpURLConnection conn = doPOST(urlString, headers, data);
    Map<String, Object> response = makeResponse(conn);
    conn.disconnect();
    return response;
  }

  //---------------------------------------------------------------------------

  private static HttpURLConnection doGET(String urlString, Map<String, Object> headers) throws IOException {
    return doConnect("GET", urlString, headers, null);
  }

  private static HttpURLConnection doPOST(String urlString, Map<String, Object> headers, String data) throws IOException {
    return doConnect("POST", urlString, headers, data);
  }

  private static HttpURLConnection doConnect(String method, String urlString, Map<String, Object> headers, String data) throws IOException {
    URL resourceUrl, base, next;
    HttpURLConnection conn;
    String url = urlString;
    String location;

    int maxTries = 10;
    int tries = 0;

    while (true) {
      resourceUrl = new URL(url);
      conn = (HttpURLConnection)resourceUrl.openConnection();
      conn.setInstanceFollowRedirects(false);
      conn.setRequestMethod(method);
      conn.setRequestProperty("User-Agent", "bobo");

      if (headers != null) {
        for (String header : headers.keySet()) {
          String value = (String)headers.get(header);
          conn.setRequestProperty(header, value);
        }
      }

      if (method == "POST" && data != null) {
        conn.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        out.writeBytes(data);
        out.flush();
        out.close();
      }

      tries += 1;
      switch (conn.getResponseCode()) {
        case HttpURLConnection.HTTP_MOVED_PERM:
        case HttpURLConnection.HTTP_MOVED_TEMP:
          if (tries >= maxTries) {
            throw new IOException(String.format("Redirect exceeded %s attempts.", maxTries));
          }

          location = conn.getHeaderField("Location");
          location = URLDecoder.decode(location, "UTF-8");
          base = new URL(url);
          next = new URL(base, location);
          url = next.toExternalForm();
          continue;
      }
      break;
    }
    return conn;
  }

  private static Map<String, Object> makeResponse(HttpURLConnection conn) throws IOException {
      Map<String, Object> response = new HashMap<String, Object>();
      response.put("status", conn.getResponseCode());
      response.put("message", conn.getResponseMessage());
      response.put("headers", readHeaders(conn));
      response.put("body", readString(conn.getInputStream()));
      return response;
  }

  private static Map<String, String> readHeaders(URLConnection conn) {
    Map<String, List<String>> headers = conn.getHeaderFields();
    Map<String, String> result = new HashMap<String, String>();
    for (Map.Entry<String, List<String>> entry : headers.entrySet())
      result.put(entry.getKey(), String.join(", ", entry.getValue()));
    return result;
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
