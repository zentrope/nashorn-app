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

public final class Functions {

  private static String getString(InputStream is) {
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

  public static String lookup(String name) {
    if (name == "github_token") {
      // My personal system, so...
      return System.getenv("HOMEBREW_GITHUB_API_TOKEN");
    }
    return System.getenv(name);
  }

  public static String sprintf(String fmt, Object ...args) {
    return String.format(fmt, args);
  }

  public static String httpGet(String urlString) {
    try {
      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", "kfi/nashorn");
      return getString(conn.getInputStream());
    }
    catch (Throwable t) {
      System.out.printf("ERROR: %s\n", t);
      return null;
    }
  }

  public static Map<String, String> httpPost(String urlString, ScriptObjectMirror headers, String data) {
    try {
      URL url = new URL(urlString);
      HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("User-Agent", "kfi/nashorn");

      for (String header : headers.keySet()) {
        String value = (String)headers.get(header);
        System.out.printf("header: %s → %s\n", header, value);
        conn.setRequestProperty(header, value);
      }

      conn.setDoOutput(true);

      DataOutputStream out = new DataOutputStream(conn.getOutputStream());
      out.writeBytes(data);
      out.flush();
      out.close();

      Map<String, String> response = new HashMap<String, String>();
      response.put("status", conn.getResponseCode()+"");
      response.put("body", getString(conn.getInputStream()));
      return response;
    }
    catch (Throwable t) {
      System.out.printf("ERROR: %s\n", t);
      return null;
    }
  }

}
