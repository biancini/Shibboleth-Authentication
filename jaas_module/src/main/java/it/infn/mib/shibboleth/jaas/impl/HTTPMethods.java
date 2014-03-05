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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

/**
 * Static methods to open web pages.
 * <p>
 * These methods manage authentication, cookies and redirects.
 * 
 * @author Andrea Biancini <andrea.biancini@gmail.com>
 * @author Simon Vocella <voxsim@gmail.com>
 * @version 1.1, 05/03/14
 */
public class HTTPMethods {
	private boolean debug = false;
	private List<String> cookies = new ArrayList<String>();
	protected List<Class<?>> recognizers = null;
	
	protected boolean sslCheck = false;
	protected String trustStore = null;
	protected String trustStorePassword = null;
	
	public HTTPMethods(boolean debug, List<Class<?>> recognizers, boolean sslCheck, String trustStore, String trustStorePassword) {
		this.debug = debug;
		this.recognizers = recognizers;
		this.sslCheck = sslCheck;
		this.trustStore = trustStore;
		this.trustStorePassword = trustStorePassword;
	}
	
	/**
	 * Retrieve an URL managing cookies, following redirects and performing HTTP Basic
	 * Authentication of requested by the server.
	 * 
	 * @param urlToRead The URL of the page to be opened.
	 * @param username The username to be passed in HTTP Basic authentication to pages requesting it.
	 * @param password The password to be passed in HTTP Basic authentication to pages requesting it.
	 * @param sslCheck A flag indicating whether the checks on SSL certificates must be performed or not. 
	 * @param trustStore The name of the trust store file to be used for SSL certificates checks.
	 * @param trustStorePassword The paHTTPMethodsssword to be used to open the trust store used for SSL certificates checks.
	 * @return <code>HTTPPage</code> object containing the page opened from the server
	 * @throws HTTPException
	 * 
	 * @exception <code>HTTPException</code> if there has been an error in retrieving the URL.
	 * @see it.infn.mib.shibboleth.jaas.impl.HTTPPage
	 * @see it.infn.mib.shibboleth.jaas.impl.HTTPException
	 */
	public HTTPPage getUrl(String urlToRead, String username, String password, int selection) throws HTTPException {
		HTTPPage returnedPage = null;
		final WebClient webClient = new WebClient();
		
		try {
			webClient.getOptions().setUseInsecureSSL(!sslCheck);
			Page curWebPage = (Page) webClient.getPage(urlToRead);
			
			boolean recognized;
			while(curWebPage.isHtmlPage()) {
				recognized = false;
				HtmlPage htmlCurWebPage = (HtmlPage) (curWebPage);
				
				if (recognizers != null) {
					for (Class<?> clazz : recognizers) {
						IRecognizer landingPage = (IRecognizer) clazz.newInstance();
						if (landingPage.isThisUrl(htmlCurWebPage.asXml())) {
							recognized = true;
							curWebPage = landingPage.processUrl(htmlCurWebPage, username, password, selection);
							
							if (!landingPage.continueTheChain()) {
								break;
							}
						}
					}
				} else {
					throw new HTTPException("Initialize recognizers property");
				}
				
				if(!recognized) {
					throw new HTTPException("The page has not been recognized");
				}
			}
			
			TextPage textCurWebPage = (TextPage) curWebPage;
			returnedPage = new HTTPPage();
			returnedPage.setReturnCode(curWebPage.getWebResponse().getStatusCode());
			
			for (NameValuePair curHeader : curWebPage.getWebResponse().getResponseHeaders()) {
				returnedPage.addHeaderField(curHeader.getName(), curHeader.getValue());
				
				if (curHeader.getName().equals("Set-Cookie")) {
					String curCookie = curHeader.getValue();
					if (curCookie.indexOf(";") > 0) curCookie = curCookie.substring(0, curCookie.indexOf(";"));
					cookies.add(curCookie);
				}
			}
			
			if (debug) System.err.println("Response code: " + returnedPage.getReturnCode());
			if (debug) System.err.println("textCurWebPage.getContent(): " + textCurWebPage.getContent());
			
			if (returnedPage.getReturnCode() == HttpURLConnection.HTTP_OK) {
				String[] bodyLines = textCurWebPage.getContent().split("\n");
				for (String line : bodyLines) {
					returnedPage.addBodyRow(line);
				}
			}
		} catch (Exception e) {
			throw new HTTPException("Error during login with Shibboleth.", e);
		} finally {
			webClient.closeAllWindows();
		}
		
		return returnedPage;
	}
	
	public String[] getChoices(String urlToRead, boolean sslCheck) throws HTTPException {
		final WebClient webClient = new WebClient();
		
		try {
			webClient.getOptions().setUseInsecureSSL(!sslCheck);
			HtmlPage curWebPage = webClient.getPage(urlToRead);
			
			if (recognizers != null) {
				for (Class<?> clazz : recognizers) {
					IRecognizer landingPage = (IRecognizer) clazz.newInstance();
					if (landingPage.isThisUrl(curWebPage.asXml())) {
						return landingPage.getChoices(curWebPage);
					}
				}
			} else {
				throw new HTTPException("Initialize recognizers property");
			}
		} catch (Exception e) {
			throw new HTTPException("Error during login with Shibboleth.", e);
		} finally {
			webClient.closeAllWindows();
		}
		
		throw new HTTPException("The page has not been recognized");
	}
}
