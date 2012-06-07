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

import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Test class that executes a login with the Shibboleth auth module.
 */
public class LoginMain {

	/**
	 * Main method that executes authentication.
	 * 
	 * @param args No arguments to be passed
	 */
	public static void main(String[] args) {
		try {
			
			LoginContext lc =  new LoginContext("Shibboleth", new MyCallbackHandler());
			lc.login();
			System.out.println("User logged in successfully.");
			
			// Prints the session values for the logged in user
			for (Principal curPrincipal : lc.getSubject().getPrincipals()) {
				if (curPrincipal instanceof ShibbolethPrincipal) {
					System.out.println("Username for logged user is: " + ((ShibbolethPrincipal) curPrincipal).getName());
					
					System.out.println("Printing session for logged in user: ");
					System.out.print(((ShibbolethPrincipal) curPrincipal).printSession());
				}
			}
			
		} catch (LoginException e) {
			System.err.println("Error logging in user.");
			e.printStackTrace();
		}
	}

}
