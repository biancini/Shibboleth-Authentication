/*
 * @(#)JAASShibbolethLoginModule.java	1.00 06/06/12
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

package it.infn.mib.shibboleth.jaas;

import it.infn.mib.shibboleth.jaas.impl.HTTPException;
import it.infn.mib.shibboleth.jaas.impl.HTTPMethods;
import it.infn.mib.shibboleth.jaas.impl.HTTPPage;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.log4j.Logger;

/**
 * <p> This sample LoginModule authenticates users with a password.
 * 
 * <p> This LoginModule connects to a web page and performs HTTP
 * basic authentication to verify user credentials.
 *
 * <p> All the rows present on the server page (as couples key=value)
 * are parsed and placed into a session HashMap to be available to
 * the user.
 *
 * @author Andrea Biancini <andrea.biancini@gmail.com>
 * @author Simon Vocella <voxsim@gmail.com>
 * @version 1.0, 06/06/12
 */
public class SAMLLoginModule implements LoginModule {
	private static Logger logger = Logger.getLogger(SAMLLoginModule.class);
	
	private Subject subject = null;
	private CallbackHandler callbackHandler = null;
	
	private String url = null;
	private boolean sslCheck = false;
	private String trustStore = null;
	private String trustStorePassword = null;
	
	private String username = null;
	private char[] password = null;
	private SAMLPrincipal userPrincipal = null;
	private String sessUsername = null;
    
	private boolean succeeded = false;
	private boolean commitSucceeded = false;
	private HTTPPage page = null;
	
	private Integer selection = null;
	private List<Class<?>> recognizers = null;

	/**
	 * Initialize this <code>LoginModule</code>.
	 *
	 * <p>
	 *
	 * @param subject the <code>Subject</code> to be authenticated. <p>
	 *
	 * @param callbackHandler a <code>CallbackHandler</code> for communicating
	 *			with the end user (prompting for user names and
	 *			passwords, for example). <p>
	 *
	 * @param sharedState shared <code>LoginModule</code> state. <p>
	 *
	 * @param options options specified in the login
	 *			<code>Configuration</code> for this particular
	 *			<code>LoginModule</code>.
	 */
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		
		url = (String) options.get("url");
		
		if(url == null || url.equals(""))
			new RuntimeException("Application could not be initialized.  Application url could not be resolved.");
		
		sessUsername = (String) options.get("sess_username");
		sslCheck = "true".equalsIgnoreCase((String) options.get("sslcheck"));
		
		trustStore = (String) options.get("truststore");
		if (trustStore.equals("")) trustStore = null;
		
		trustStorePassword = (String) options.get("truststore_password");
		if (trustStorePassword.equals("")) trustStorePassword = null;
		
		String strRecognizers = (String) options.get("recognizers");
		if (strRecognizers != null && !strRecognizers.equals("")) {
			recognizers = new ArrayList<Class<?>>();
			String[] clazzes = strRecognizers.split(",");
			for (String clazz : clazzes) {
				try {
					recognizers.add(Class.forName(clazz));
				} catch (ClassNotFoundException e) {
					new RuntimeException("Application could not be initialized.  Application recognizers could not be resolved.");
				}
			}
		}
	}

	/**
	 * Authenticate the user by prompting for a user name and password.
	 *
	 * <p>
	 *
	 * @return true in all cases since this <code>LoginModule</code>
	 *		should not be ignored.
	 *
	 * @exception FailedLoginException if the authentication fails. <p>
	 *
	 * @exception LoginException if this <code>LoginModule</code>
	 *		is unable to perform the authentication.
	 */
	public boolean login() throws LoginException {
		// prompt for a user name and password
		if (callbackHandler == null)
		    throw new LoginException("Error: no CallbackHandler available to garner authentication information from the user");
		
		HTTPMethods httpMethods = new HTTPMethods(recognizers, sslCheck, trustStore, trustStorePassword);
		List<Callback> callbacks = new ArrayList<Callback>();
		callbacks.add(new NameCallback("Username: "));
		callbacks.add(new PasswordCallback("Password: ", false));
		
		String[] choices = null;
		try {
			choices = httpMethods.getChoices(url, sslCheck);
			if(choices != null) {
				callbacks.add(new ChoiceCallback("Choice: ", choices, 0, false));
			}
		} catch (HTTPException e) {
			logger.error("Unable to recognize the page.", e);
			throw new FailedLoginException("Unable to recognize the page.");
		}
		
		try {
		    callbackHandler.handle(callbacks.toArray(new Callback[callbacks.size()]));
		    username = ((NameCallback)callbacks.get(0)).getName();
		    char[] tmpPassword = ((PasswordCallback)callbacks.get(1)).getPassword();
		    if (tmpPassword == null) {
		    	// treat a NULL password as an empty password
		    	tmpPassword = new char[0];
		    }
		    password = new char[tmpPassword.length];
		    System.arraycopy(tmpPassword, 0, password, 0, tmpPassword.length);
		    ((PasswordCallback)callbacks.get(1)).clearPassword();
		    
		    if(choices != null) {
		    	int[] selectedIndexes = ((ChoiceCallback)callbacks.get(2)).getSelectedIndexes();
		    	selection = selectedIndexes[0];
		    }
		} catch (java.io.IOException ioe) {
		    throw new LoginException(ioe.toString());
		} catch (UnsupportedCallbackException uce) {
		    throw new LoginException("Error: " + uce.getCallback().toString() + " not available to garner authentication information from the user");
		}

		logger.debug("[SampleLoginModule] user entered user name: " + username);

		try {
			page = httpMethods.getUrl(url, username, new String(password), selection);
			if (page.getReturnCode() == HttpURLConnection.HTTP_OK) {
				for (String curRow : page.getBodyRows()) {
					if (curRow.startsWith("authenticated") && new Boolean(curRow.replace("authenticated=", "").trim()).booleanValue()) {
						// authentication succeeded!!!
						logger.debug("[SampleLoginModule] authentication succeeded");
						succeeded = true;
						return true;
					}
				}
			}
			
			throw new HTTPException("Returned page has return code = " + page.getReturnCode());
		}
		catch (HTTPException e) {
			logger.error(e.getMessage(), e);
			
			// authentication failed -- clean out state
			logger.debug("[SampleLoginModule] auth.entication failed");
			succeeded = false;
			username = null;
		    
			for (int i = 0; i < password.length; i++) password[i] = ' ';
			password = null;
		    
			throw new FailedLoginException("Unable to verify username/password.");
		}
	}

	/**
	 * Logout the user.
	 *
	 * <p> This method removes the <code>ShibbolethPrincipal</code>
	 * that was added by the <code>commit</code> method.
	 *
	 * <p>
	 *
	 * @exception LoginException if the logout fails.
	 *
	 * @return true in all cases since this <code>ShibbolethPrincipal</code>
	 *          should not be ignored.
	 */
	public boolean logout() throws LoginException {
		subject.getPrincipals().remove(userPrincipal);
		succeeded = false;
		succeeded = commitSucceeded;
		username = null;
		if (password != null) {
		    for (int i = 0; i < password.length; i++) password[i] = ' ';
		    password = null;
		}
		userPrincipal = null;
		page = null;
		return true;
	}
	
	/**
	 * <p> This method is called if the LoginContext's
	 * overall authentication failed.
	 * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
	 * did not succeed).
	 *
	 * <p> If this LoginModule's own authentication attempt
	 * succeeded (checked by retrieving the private state saved by the
	 * <code>login</code> and <code>commit</code> methods),
	 * then this method cleans up any state that was originally saved.
	 *
	 * <p>
	 *
	 * @exception LoginException if the abort fails.
	 *
	 * @return false if this LoginModule's own login and/or commit attempts
	 *		failed, and true otherwise.
	 */
	public boolean abort() throws LoginException {
		if (succeeded == false) {
		    return false;
		} else if (succeeded == true && commitSucceeded == false) {
		    // login succeeded but overall authentication failed
		    succeeded = false;
		    username = null;
		    if (password != null) {
				for (int i = 0; i < password.length; i++) password[i] = ' ';
				password = null;
		    }
		    userPrincipal = null;
		} else {
		    // overall authentication succeeded and commit succeeded,
		    // but someone else's commit failed
		    logout();
		}
		return true;
	}

	/**
	 * <p> This method is called if the LoginContext's
	 * overall authentication succeeded
	 * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
	 * succeeded).
	 *
	 * <p> If this LoginModule's own authentication attempt
	 * succeeded (checked by retrieving the private state saved by the
	 * <code>login</code> method), then this method associates a
	 * <code>ShibbolethPrincipal</code>
	 * with the <code>Subject</code> located in the
	 * <code>LoginModule</code>.  If this LoginModule's own
	 * authentication attempted failed, then this method removes
	 * any state that was originally saved.
	 *
	 * <p>
	 *
	 * @exception LoginException if the commit fails.
	 *
	 * @return true if this LoginModule's own login and commit
	 *		attempts succeeded, or false otherwise.
	 */
	public boolean commit() throws LoginException {
		if (succeeded == false) {
		    return false;
		} else {
			Map<String, String> session = new HashMap<String, String>();
			for (String curBodyRow : page.getBodyRows()) {
		    	if (curBodyRow != null) {
		    		if (curBodyRow.contains("=")) session.put(curBodyRow.substring(0, curBodyRow.indexOf("=")), curBodyRow.substring(curBodyRow.indexOf("=")+1));
		    		else session.put(curBodyRow, "");
		    	}
		    }
			
			logger.debug("[SampleLoginModule] pre ShibbolethPrincipal");
			logger.debug("[SampleLoginModule] username: "+username);
			logger.debug("[SampleLoginModule] sessUsername: "+sessUsername);
			
		    // add a Principal (authenticated identity) to the Subject
			if (sessUsername != null && !sessUsername.equals("")) {
				userPrincipal = new SAMLPrincipal(session.get(sessUsername));
			}
			else {
				userPrincipal = new SAMLPrincipal(username);
			}
		    
		    userPrincipal.setSession(session);
		    
		    if (!subject.getPrincipals().contains(userPrincipal))
			subject.getPrincipals().add(userPrincipal);

		    logger.debug("[SampleLoginModule] added ShibbolethPrincipal to Subject.");

		    // in any case, clean out state
		    username = null;
		    for (int i = 0; i < password.length; i++) password[i] = ' ';
		    password = null;
		    page = null;

		    commitSucceeded = true;
		    return true;
		}
	}
	
}
