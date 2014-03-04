/*
 * @(#)MyCallbackHandler.java	1.00 06/06/12
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

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.junit.Ignore;

/**
 * Default callback handler that prompts the user to insert the required data.
 * 
 * @version 1.0, 06/06/2012
 */
@Ignore
public class FixedTestCallbackHandler implements CallbackHandler {
	
	private String username;
	private String password;
	
	public FixedTestCallbackHandler(String username, String password) {
		this.username = username;
		this.password = password;
	}
    
	/**
	 * Invoke an array of Callbacks.
	 * 
	 * @param callbacks The array of callbacks available
	 * @exception IOException if there is an error reading from console.
	 * @exception UnsupportedCallbackException if the callback throws an error. 
	 */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    	for (int i = 0; i < callbacks.length; i++) {
    		if (callbacks[i] instanceof NameCallback) {
    			((NameCallback)callbacks[i]).setName(username);
    		} else if (callbacks[i] instanceof PasswordCallback) {
    			((PasswordCallback) callbacks[i]).setPassword(password.toCharArray());
    		} else {
    			throw new UnsupportedCallbackException(callbacks[i], "MyCallbackHandler: Unrecognized Callback");
    		}
    	}
    }
}