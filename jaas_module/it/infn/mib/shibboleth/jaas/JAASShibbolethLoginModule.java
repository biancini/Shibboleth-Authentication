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

import it.infn.mib.shibboleth.jaas.impl.HTTPMethods;
import it.infn.mib.shibboleth.jaas.impl.HTTPPage;

import java.net.HttpURLConnection;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

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
 * <p> This LoginModule recognizes the debug option.
 * If set to true in the login Configuration,
 * debug messages will be output to the error stream, System.err.
 *
 * @version 1.0, 06/06/12
 */
public class JAASShibbolethLoginModule implements LoginModule {
	
	public static void main(String [] args) {
		HTTPPage page = HTTPMethods.getUrl("https://cloud-mi-03.mib.infn.it/secure/pam.php", "andrea", "ciaoandrea");
		System.out.println(page.getReturnCode());
	}
	
	private Subject subject = null;
	private CallbackHandler callbackHandler = null;
	private boolean debug = false;
	
	//private boolean useFirstPass = false;
	//private boolean tryFirstPass = false;
	
	private String username = null;
    private char[] password = null;
    private ShibbolethPrincipal userPrincipal = null;
    
    private boolean succeeded = false;
    private boolean commitSucceeded = false;


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
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;

		// initialize any configured options
		debug = "true".equalsIgnoreCase((String)options.get("debug"));
		HTTPMethods.debug = debug;
		
		//tryFirstPass = options.get("try_first_pass") != null;
		//useFirstPass = options.get("use_first_pass") != null;
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
	@Override
	public boolean login() throws LoginException {
		// prompt for a user name and password
		if (callbackHandler == null)
		    throw new LoginException("Error: no CallbackHandler available to garner authentication information from the user");

		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("Username: ");
		callbacks[1] = new PasswordCallback("Password: ", false);
	 
		try {
		    callbackHandler.handle(callbacks);
		    username = ((NameCallback)callbacks[0]).getName();
		    char[] tmpPassword = ((PasswordCallback)callbacks[1]).getPassword();
		    if (tmpPassword == null) {
		    	// treat a NULL password as an empty password
		    	tmpPassword = new char[0];
		    }
		    password = new char[tmpPassword.length];
		    System.arraycopy(tmpPassword, 0, password, 0, tmpPassword.length);
		    ((PasswordCallback)callbacks[1]).clearPassword();
		} catch (java.io.IOException ioe) {
		    throw new LoginException(ioe.toString());
		} catch (UnsupportedCallbackException uce) {
		    throw new LoginException("Error: " + uce.getCallback().toString() + " not available to garner authentication information from the user");
		}

		// print debugging information
		if (debug) {
		    System.err.println("\t\t[SampleLoginModule] user entered user name: " + username);
		    System.err.print("\t\t[SampleLoginModule] user entered password: ");
		    for (int i = 0; i < password.length; i++) System.err.print(password[i]);
		    System.err.println();
		}

		HTTPPage page = HTTPMethods.getUrl("https://cloud-mi-03.mib.infn.it/secure/pam.php", username, new String(password));
		if (page.getReturnCode() == HttpURLConnection.HTTP_OK) {
		    // authentication succeeded!!!
		    if (debug) System.err.println("\t\t[SampleLoginModule] authentication succeeded");
		    succeeded = true;
		    return true;
		} else {
		    // authentication failed -- clean out state
		    if (debug) System.err.println("\t\t[SampleLoginModule] authentication failed");
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
	@Override
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
	@Override
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
	@Override
	public boolean commit() throws LoginException {
		if (succeeded == false) {
		    return false;
		} else {
		    // add a Principal (authenticated identity)
		    // to the Subject

		    // assume the user we authenticated is the SamplePrincipal
		    userPrincipal = new ShibbolethPrincipal(username);
		    if (!subject.getPrincipals().contains(userPrincipal))
			subject.getPrincipals().add(userPrincipal);

		    if (debug) System.err.println("\t\t[SampleLoginModule] added ShibbolethPrincipal to Subject.");

		    // in any case, clean out state
		    username = null;
		    for (int i = 0; i < password.length; i++) password[i] = ' ';
		    password = null;

		    commitSucceeded = true;
		    return true;
		}
	}
	
}