/*
 * @(#)HTTPMethods.java	1.00 06/06/12
 *
 * Copyright 2012-2012 Andrea Biancini. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY 
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY 
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR 
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR 
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE 
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, 
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER 
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed, licensed or 
 * intended for use in the design, construction, operation or 
 * maintenance of any nuclear facility. 
 */

package it.infn.mib.shibboleth.jaas.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;

public class HTTPMethods {
	public static boolean debug = false;
	private static List<String> cookies = new ArrayList<String>();
	private static boolean sslCheck = false;
	private static String trustStore = null;
	private static String trustStorePassword = null;
	
	public static HTTPPage getUrl(String urlToRead, String username, String password, boolean sslCheck, String trustStore, String trustStorePassword) {
		HTTPPage returnedPage = null;
		HTTPMethods.sslCheck = sslCheck;
		HTTPMethods.trustStore = trustStore;
		HTTPMethods.trustStorePassword = trustStorePassword;
		
		while (urlToRead != null) {
			returnedPage = getSingleUrl(urlToRead, null);
			
			if (returnedPage.getReturnCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
				returnedPage = getSingleUrl(urlToRead, username + ":" + password);
		
			urlToRead = returnedPage.getHeaderField("Location");
		}
		
		return returnedPage;
	}
	
	private static HttpURLConnection createConnectionHttps(String urlToRead) throws NoSuchAlgorithmException, KeyManagementException, MalformedURLException, IOException {
	    // All set up, we can get a resource through https now:
	    final HttpURLConnection conn = (HttpURLConnection) new URL(urlToRead).openConnection();
	    
		if (HTTPMethods.sslCheck == false) {
			final TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(final X509Certificate[] chain, final String authType) { }
						@Override
						public void checkServerTrusted(final X509Certificate[] chain, final String authType) { }
						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}
					}
				};
			
			// Install the all-trusting trust manager
		    final SSLContext sslContext = SSLContext.getInstance("SSL");
		    sslContext.init( null, trustAllCerts, new java.security.SecureRandom() );
		    // Create an ssl socket factory with our all-trusting manager
		    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		    
		    // Tell the url connection object to use our socket factory which bypasses security checks
		    ((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
		}
		else {
			if (trustStore != null) System.setProperty( "javax.net.ssl.trustStore", trustStore);
			if (trustStorePassword != null) System.setProperty( "javax.net.ssl.trustStorePassword", trustStorePassword);
		}
		
		return conn;
	}
	
	protected static HTTPPage getSingleUrl(String urlToRead, String basicString) {
		HttpURLConnection conn = null;
		BufferedReader rd = null;
		String line = null;
		HTTPPage returnedPage = new HTTPPage();
		
		try {
			String cookiesString = "";
	        for (String cookie: cookies) cookiesString += (cookiesString.equals("") ? "" : ";") + cookie;
	        
			if (debug) System.err.println("Opening URL: " + urlToRead);
			if (debug) System.err.println("Passing the following cookies: " + cookiesString);

			if (urlToRead.startsWith("https://")) conn = createConnectionHttps(urlToRead);
			else conn = (HttpURLConnection) new URL(urlToRead).openConnection();
			
			conn.setRequestMethod("GET");
			conn.setInstanceFollowRedirects(false);
			conn.addRequestProperty("Cookie", cookiesString);
			
			if (basicString != null) {
				if (debug) System.err.println("Basic authentication string: " + basicString);
				String encoding = new Base64().encodeAsString(basicString.getBytes());
				conn.setRequestProperty("Authorization", "Basic " + encoding);
			}
			
			conn.connect();
			returnedPage.setReturnCode(conn.getResponseCode());
			
			for (String headerKey : conn.getHeaderFields().keySet()) {
				if (headerKey != null) {
					returnedPage.addHeaderField(headerKey, conn.getHeaderField(headerKey));
				
					if (headerKey.equals("Set-Cookie")) {
						String curCookie = conn.getHeaderField(headerKey);
						if (curCookie.indexOf(";") > 0) curCookie = curCookie.substring(0, curCookie.indexOf(";"));
						cookies.add(conn.getHeaderField(headerKey));
					}
				}
			}
			
			if (debug) System.err.println("Response code: " + returnedPage.getReturnCode());
			
			if (returnedPage.getReturnCode() == HttpURLConnection.HTTP_OK) {
				rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				while ((line = rd.readLine()) != null) {
					returnedPage.addBodyRow(line);
				}
				rd.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return returnedPage;
		} finally {
			if (conn != null) conn.disconnect();
		}
		
		return returnedPage;
	}
}
