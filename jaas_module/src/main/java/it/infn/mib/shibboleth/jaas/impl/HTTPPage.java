/*
 * @(#)HTTPPage.java	1.00 06/06/12
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Object representing an HTTP Page opened from a web server.
 * 
 * @version 1.0, 06/06/2012
 */
public class HTTPPage {

	private int returnCode = -1;
	private Map<String, String> headerFields = null;
	private List<String> bodyRows = null;

	/**
	 * Empty constructor which initializes local variables.
	 */
	public HTTPPage() {
		headerFields = new HashMap<String, String>();
		bodyRows = new ArrayList<String>();
	}
	
	/**
	 * Getter for the HTTP return code
	 * 
	 * @return the HTTP return code
	 */
	public int getReturnCode() {
		return returnCode;
	}

	/**
	 * Setter for the HTTP return code
	 * 
	 * @param returnCode the HTTP return code
	 */
	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
	
	/**
	 * Getter for an HTTP header field
	 * 
	 * @param fieldName the header field name
	 * @return the HTTP header field value
	 */
	public String getHeaderField(String fieldName) {
		if (headerFields != null && headerFields.containsKey(fieldName)) {
			return headerFields.get(fieldName);
		}
		
		return null;
	}
	
	/**
	 * Function to add an header field to the header of the web page
	 * 
	 * @param fieldName the name of the header field
	 * @param fieldValue the value of the header field
	 */
	public void addHeaderField(String fieldName, String fieldValue) {
		headerFields.put(fieldName, fieldValue);
	}
	
	/**
	 * Function to add a row to the body object of the page
	 * 
	 * @param bodyRow String containing the row to be added to the body object
	 */
	public void addBodyRow(String bodyRow) {
		bodyRows.add(bodyRow);
	}
	
	/**
	 * Function to retrieve all body rows for the opened page
	 * 
	 * @return A <code>List</code> of <code>String</code> containing all the
	 * rows of the body of the web page
	 */
	public List<String> getBodyRows() {
		return bodyRows;
	}
	
}
