/*
 * @(#)ShibbolethPrincipal.java	1.00 06/06/12
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

import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * <p> This class implements the <code>Principal</code> interface
 * and represents a Sample user.
 *
 * <p> Principals such as this <code>SamplePrincipal</code>
 * may be associated with a particular <code>Subject</code>
 * to augment that <code>Subject</code> with an additional
 * identity.  Refer to the <code>Subject</code> class for more information
 * on how to achieve this.  Authorization decisions can then be based upon 
 * the Principals associated with a <code>Subject</code>.
 * 
 * @author Andrea Biancini <andrea.biancini@gmail.com>
 * @version 1.0, 06/06/2012
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class ShibbolethPrincipal implements Principal, Serializable {

	private static final long serialVersionUID = 7023125781649016438L;
	
    private String name = null;
    private Map<String, String> session = null;

    /**
     * Create a ShibbolethPrincipal with a username.
     *
     * @param name the username for this user.
     *
     * @exception NullPointerException if the <code>name</code> is <code>null</code>.
     */
    public ShibbolethPrincipal(String name) {
		if (name == null) throw new NullPointerException("illegal null input");
		this.name = name;
		this.session = new HashMap<String, String>();
    }

    /**
     * Return the username for this <code>ShibbolethPrincipal</code>.
     *
     * @return the username for this <code>ShibbolethPrincipal</code>
     */
    public String getName() {
    	return name;
    }
    
    /**
     * Return the session for this <code>ShibbolethPrincipal</code>.
     *
     * @return the session for this <code>ShibbolethPrincipal</code>
     */
    public Map<String, String> getSession() {
    	return session;
    }
    
    /**
     * Sets the session for this <code>ShibbolethPrincipal</code>.
     *
     * @param session The map with the session key-value pairs
     */
    public void setSession(Map<String, String> session) {
    	this.session = session;
    }
    
    /**
     * Return the Sample username for this <code>ShibbolethPrincipal</code>.
     *
     * @param key The key to be inserted in the <code>session</code> map
     * @param value The value of the provided key to be inserted in the <code>session</code> map
     */
    public void addSessionValue(String key, String value) {
    	session.put(key, value);
    }
    
    /**
     * Return the Sample username for this <code>ShibbolethPrincipal</code>.
     *
     * @param key The key to be searched in the <code>session<code> map
     * @return The value of the provided key or <code>null</code> if the key is not found
     */
    public String getSessionValue(String key) {
    	if (session.containsKey(key)) return session.get(key);
    	return null;
    }
    
    /**
     * Return a String with the content of the session for this <code>ShibbolethPrincipal</code>.
     *
     * @return A string with all values in the <code>session</code> map
     */
    public String printSession() {
    	String printSession = "";
    	for (String curKey : session.keySet()) {
    		printSession += "[" + curKey + "] => " + session.get(curKey) + "\n";
    	}
    	return printSession;
    }

    /**
     * Return a string representation of this <code>ShibbolethPrincipal</code>.
     *
     * @return a string representation of this <code>ShibbolethPrincipal</code>.
     */
    public String toString() {
    	return("ShibbolethPrincipal:  " + name);
    }

    /**
     * Compares the specified Object with this <code>ShibbolethPrincipal</code>
     * for equality.  Returns true if the given object is also a
     * <code>ShibbolethPrincipal</code> and the two SamplePrincipals
     * have the same username.
     *
     * @param o Object to be compared for equality with this <code>ShibbolethPrincipal</code>.
     * @return true if the specified Object is equal equal to this <code>ShibbolethPrincipal</code>.
     */
    public boolean equals(Object o) {
    	if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof ShibbolethPrincipal)) return false;
        ShibbolethPrincipal that = (ShibbolethPrincipal)o;
        if (this.getName().equals(that.getName())) return true;
        return false;
    }
 
    /**
     * Return a hash code for this <code>ShibbolethPrincipal</code>.
     *
     * @return a hash code for this <code>ShibbolethPrincipal</code>.
     */
    public int hashCode() {
    	return name.hashCode();
    }
}