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

/**
 * Static methods to open web pages.
 * <p>
 * These methods manage authentication (via HTTP Basic), cookies and redirects.
 * 
 *  @version 1.0, 06/06/12
 */
public class HTTPMethods {
	public static boolean debug = false;
	private static List<String> cookies = new ArrayList<String>();
	private static boolean sslCheck = false;
	private static String trustStore = null;
	private static String trustStorePassword = null;
	
	/**
	 * Retrieve an URL managing cookies, following redirects and performing HTTP Basic
	 * Authentication of requested by the server.
	 * 
	 * @param urlToRead The URL of the page to be opened.
	 * @param username The username to be passed in HTTP Basic authentication to pages requesting it.
	 * @param password The password to be passed in HTTP Basic authentication to pages requesting it.
	 * @param sslCheck A flag indicating whether the checks on SSL certificates must be performed or not. 
	 * @param trustStore The name of the trust store file to be used for SSL certificates checks.
	 * @param trustStorePassword The password to be used to open the trust store used for SSL certificates checks.
	 * @return <code>HTTPPage</code> object containing the page opened from the server
	 * 
	 * @exception <code>HTTPException</code> if there has been an error in retrieving the URL.
	 * @see it.infn.mib.shibboleth.jaas.impl.HTTPPage
	 * @see it.infn.mib.shibboleth.jaas.impl.HTTPException
	 */
	public static HTTPPage getUrlOld(String urlToRead, String username, String password, boolean sslCheck, String trustStore, String trustStorePassword) throws HTTPException {
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
	public static HTTPPage getUrl(String urlToRead, String username, String password, boolean sslCheck, String trustStore, String trustStorePassword) throws HTTPException {
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
	
	/**
	 * Create the HTTPs connection enabling or disabling the check on SSL certificates.
	 * 
	 * @param urlToRead The URL of the page to be opened.
	 * @return <code>HttpURLConnection</code> object with the connection with the server
	 * @exception NoSuchAlgorithmException if the SSL cypher algorithm is not supported
	 * @exception KeyManagementException if the management of certificate keys returns an error
	 * @exception MalformedURLException if the supplied URL is not in a correct format
	 * @exception IOException if there is an error in accessing the keystore file
	 */
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
		    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
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

	/**
	 * Open a single HTTP page managing cookies and Basic authentication.
	 * 
	 * @param urlToRead The URL of the page to be opened.
	 * @param basicString The string to be passed as basic authentication. If <code>null</code>
	 * no authentication is performed on the webserver, otherwise the <code>username:password</code>
	 * specified in this parameter as passed as the Basic authentication string.
	 * @return <code>HttpURLConnection</code> object with the connection with the server
	 *
	 * @exception <code>HTTPException</code> if there has been an error in retrieving the URL.
	 * @see it.infn.mib.shibboleth.jaas.impl.HTTPPage
	 * @see it.infn.mib.shibboleth.jaas.impl.HTTPException
	 */
	protected static HTTPPage getSingleUrl(String urlToRead, String basicString) throws HTTPException {
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
				String encoding = new String(new Base64().encode(basicString.getBytes()));
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
						cookies.add(curCookie);
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
		} catch (KeyManagementException e) {
			throw new HTTPException("Error while retrieving url " + urlToRead, e, "KeyManagementException");
		} catch (NoSuchAlgorithmException e) {
			throw new HTTPException("Error while retrieving url " + urlToRead, e, "NoSuchAlgorithmException");
		} catch (MalformedURLException e) {
			throw new HTTPException("Error while retrieving url " + urlToRead, e, "MalformedURLException");
		} catch (IOException e) {
			throw new HTTPException("Error while retrieving url " + urlToRead, e, "IOException");
		} finally {
			if (conn != null) conn.disconnect();
		}
		
		return returnedPage;
	}
}
