/*
 * @(#)LoginMain.java	1.00 06/06/12
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

package it.infn.mib.shibboleth.jaas.test;

import it.infn.mib.shibboleth.jaas.ShibbolethPrincipal;
import it.infn.mib.shibboleth.jaas.test.ws.BackendBindingStub;
import it.infn.mib.shibboleth.jaas.test.ws.BackendPort;
import it.infn.mib.shibboleth.jaas.test.ws.BackendServiceLocator;
import it.infn.mib.shibboleth.jaas.test.ws.CookieHandler;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisProperties;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.Handler;
import org.apache.axis.SimpleChain;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.client.AxisClient;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.transport.http.HTTPSender;
import org.apache.axis.transport.http.HTTPTransport;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test class that executes a login with the Shibboleth auth module.
 * 
 * @version 1.0, 06/06/2012
 */
public class LoginMainTest {

	private static String loggedUser = null;
	private static Map<String, String> session = null;

	/**
	 * Method to create the cookie token config to pass cookies to the webservice
	 * 
	 * @param cookies the cookies to be passed to the WS call
	 * @return the engine configuration to manage webservice calls
	 * @exception if some method goes an error
	 */
	@Ignore
	private static EngineConfiguration createCookieTokenConfig(Map<String, String> cookies) throws LoginException { 
		SimpleProvider clientConfig=new SimpleProvider(); 
		Handler cookieHandler = (Handler) new CookieHandler(cookies);
		SimpleChain reqHandler = new SimpleChain(); 
		SimpleChain respHandler = new SimpleChain(); 
		reqHandler.addHandler(cookieHandler); 
		Handler pivot = (Handler) new HTTPSender(); 
		Handler transport = new SimpleTargetedChain(reqHandler, pivot, respHandler); 
		clientConfig.deployTransport(HTTPTransport.DEFAULT_TRANSPORT_NAME, transport); 
		return clientConfig;   
	}

	/**
	 * Method to get the WS handler with cookie tokens.
	 * 
	 * @param wsEndpoint the webservice endpoint URL
	 * @param cookies the cookies to be passed to the WS call
	 * @return the stub to call the webservice
	 * @exception if some method goes an error
	 */
	@Ignore
	private static BackendBindingStub getCookieTokenBinding(String wsEndpoint, Map<String,String> cookies) throws LoginException {
		BackendBindingStub binding = null;
		
		try {
			BackendServiceLocator loc = new BackendServiceLocator(wsEndpoint);
		    EngineConfiguration clientConfig = createCookieTokenConfig(cookies);
		    loc.setEngineConfiguration(clientConfig); 
		    loc.setEngine(new AxisClient(clientConfig));
		    binding = (BackendBindingStub) loc.getBackendPort();
		}
		catch (ServiceException jre) {
			if (jre.getLinkedCause() != null) jre.getLinkedCause().printStackTrace();
			throw new LoginException("JAX-RPC ServiceException caught: " + jre);
		}

		binding.setMaintainSession(true);
		return binding;
	}
	
	/**
	 * Function to call a webservice with Shibboleth authentication
	 * 
	 * @param args No arguments to be passed
	 * @throws LoginException when the login has an exception
	 * @throws RemoteException when there is an error in calling webservice
	 */
	@Test
	private static void callWebservice() throws LoginException, RemoteException {
		login();
		System.out.println("Trying to call webservice using obtained credentials.");
		
		String wsEndpoint = "https://cloud-mi-03.mib.infn.it/secure/webservice.php";
		AxisProperties.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
		
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put("_shibsession_" + session.get("Shib-Session-Unique"), session.get("Shib-Session-ID"));
		
		BackendPort service = getCookieTokenBinding(wsEndpoint, cookies);
		System.out.println(service.oncall(loggedUser));
	}
	
	/**
	 * Function to login a user.
	 * 
	 * @param args No arguments to be passed
	 * @throws LoginException When the login has an exception
	 */
	@Test
	private static void login() throws LoginException {
		LoginContext lc =  new LoginContext("Shibboleth", new MyCallbackHandler());
		lc.login();
		System.out.println("User logged in successfully.");
		
		// Prints the session values for the logged in user
		for (Principal curPrincipal : lc.getSubject().getPrincipals()) {
			if (curPrincipal instanceof ShibbolethPrincipal) {
				loggedUser = ((ShibbolethPrincipal) curPrincipal).getName();
				session = ((ShibbolethPrincipal) curPrincipal).getSession();
			}
		}
		
		System.out.println("User logged in successfully.");
		System.out.println("Username for logged user is: " + loggedUser);
				
		System.out.println("Printing session for logged in user: ");
		for (String curKey : session.keySet())
			System.out.println("[" + curKey + "] => " + session.get(curKey));
	}
	
	/**
	 * Function to login a user.
	 * 
	 * @param args No arguments to be passed
	 * @return true if the user session was found in environment, false otherwise
	 */
	@Test
	private static void sso() {
		String sessUsername = "eduPersonPrincipalName";
		
		// The environment variables should not contain the dash character
		// so the login module may translate the dash with an underline symbol.
		// Here do the opposite translation (replaceAll instructions).
		loggedUser = System.getProperty(sessUsername.replaceAll("-", "_"));
		String shibUnique = System.getProperty("Shib-Session-Unique".replaceAll("-", "_"));
		String shibId = System.getProperty("Shib-Session-ID".replaceAll("-", "_"));
		
		if (loggedUser == null || shibUnique == null || shibId == null) {
			System.err.println("Error retrieving user session from environment.");
			assert(false);
		}
		
		session = new HashMap<String, String>();
		session.put("Shib-Session-Unique", shibUnique); 
		session.put("Shib-Session-ID", shibId);
		
		System.out.println("User logged in successfully.");
		System.out.println("Username for logged user is: " + loggedUser);
				
		System.out.println("Printing session for logged in user: ");
		for (String curKey : session.keySet())
			System.out.println("[" + curKey + "] => " + session.get(curKey));
	}

}
