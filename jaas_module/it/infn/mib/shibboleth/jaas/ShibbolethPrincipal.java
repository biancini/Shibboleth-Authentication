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
 * @version 1.4, 01/11/00
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class ShibbolethPrincipal implements Principal, Serializable {

	private static final long serialVersionUID = 7023125781649016438L;
	
	/**
     * @serial
     */
    private String name = null;

    /**
     * Create a SamplePrincipal with a Sample username.
     *
     * <p>
     *
     * @param name the Sample username for this user.
     *
     * @exception NullPointerException if the <code>name</code>
     *			is <code>null</code>.
     */
    public ShibbolethPrincipal(String name) {
		if (name == null) throw new NullPointerException("illegal null input");
		this.name = name;
    }

    /**
     * Return the Sample username for this <code>ShibbolethPrincipal</code>.
     *
     * <p>
     *
     * @return the Sample username for this <code>ShibbolethPrincipal</code>
     */
    public String getName() {
    	return name;
    }

    /**
     * Return a string representation of this <code>ShibbolethPrincipal</code>.
     *
     * <p>
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
     * <p>
     *
     * @param o Object to be compared for equality with this
     *		<code>ShibbolethPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *		<code>ShibbolethPrincipal</code>.
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
     * <p>
     *
     * @return a hash code for this <code>ShibbolethPrincipal</code>.
     */
    public int hashCode() {
    	return name.hashCode();
    }
}