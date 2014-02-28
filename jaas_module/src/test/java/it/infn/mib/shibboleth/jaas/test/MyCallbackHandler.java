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

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

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
public class MyCallbackHandler implements CallbackHandler {
    
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
    			((NameCallback)callbacks[i]).setName(readLine(((NameCallback) callbacks[i]).getPrompt(), false));
    		} else if (callbacks[i] instanceof PasswordCallback) {
    			((PasswordCallback) callbacks[i]).setPassword(readLine(((PasswordCallback) callbacks[i]).getPrompt(), true).toCharArray());
    		} else {
    			throw new UnsupportedCallbackException(callbacks[i], "MyCallbackHandler: Unrecognized Callback");
    		}
    	}
    }
    
    /**
	 * Method that reads a line from input console.
	 * 
	 * @param prompt The text to be show to the user asking for input.
	 * @param masquered A boolean value indicating if the input of the user
	 * must be masquered on the console or not.
	 * @return The line of text read.
	 * @exception IOException if there is an error reading from console.
	 */
    public static String readLine(String prompt, boolean masquered) throws IOException {
    	Console console = System.console();
        if (console != null) {
        	String input = null;
        	if (masquered) input = new String(console.readPassword(prompt));
            else input = new String(console.readLine(prompt));
        	return input;
        }
        else { 
        	System.err.println("Unable to obtain console");
        	System.out.print(prompt);
        	InputStreamReader converter = new InputStreamReader(System.in);
        	BufferedReader in = new BufferedReader(converter);
        	return in.readLine();
        }
    }
}