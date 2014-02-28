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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

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
	
	private static final String SHIBBOLETH_XPATH_FORM = "//form";
	private static final String SHIBBOLETH_XPATH_BUTTON = "//button";
	private static final String SHIBBOLETH_USERNAME_FIELD = "j_username";
	private static final String SHIBBOLETH_PASSWORD_FIELD = "j_password";
	
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
	public static HTTPPage getUrl(String urlToRead, String username, String password, boolean sslCheck, String trustStore, String trustStorePassword) throws HTTPException {
		HTTPPage returnedPage = null;
		
		try {
			returnedPage = loginShibboleth(urlToRead, username, trustStorePassword, sslCheck);
		} catch (FailingHttpStatusCodeException | IOException e) {
			throw new HTTPException("Error during login with Shibboleth.", e);
		}
		
		return returnedPage;
	}
	
	protected static HTTPPage loginShibboleth(String urlToRead, String username, String password, boolean sslCheck) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		final WebClient webClient = new WebClient();
		webClient.getOptions().setUseInsecureSSL(!sslCheck);

		HtmlPage curWebPage = webClient.getPage(urlToRead);
		final HtmlForm form = (HtmlForm) curWebPage.getByXPath(SHIBBOLETH_XPATH_FORM).get(0);
		final HtmlButton button = (HtmlButton) form.getByXPath(SHIBBOLETH_XPATH_BUTTON).get(0);

		final HtmlTextInput usernameField = form.getInputByName(SHIBBOLETH_USERNAME_FIELD);
		final HtmlPasswordInput passwordField = form.getInputByName(SHIBBOLETH_PASSWORD_FIELD);

		usernameField.setValueAttribute(username);
		passwordField.setValueAttribute(password);

		curWebPage = button.click();
		
		HTTPPage returnedPage = new HTTPPage();
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
		
		if (returnedPage.getReturnCode() == HttpURLConnection.HTTP_OK) {
			String[] bodyLines = curWebPage.asText().split("\n");
			for (String line : bodyLines) {
				returnedPage.addBodyRow(line);
			}
		}
		
		webClient.closeAllWindows();
		return returnedPage;
	}

}
