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

package it.infn.mib.shibboleth.jaas.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTPPage {

	private int returnCode = -1;
	private Map<String, String> headerFields = null;
	private List<String> bodyRows = null;
	
	public HTTPPage() {
		headerFields = new HashMap<String, String>();
		bodyRows = new ArrayList<String>();
	}
	
	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
	
	public String getHeaderField(String fieldName) {
		if (headerFields != null && headerFields.containsKey(fieldName)) {
			return headerFields.get(fieldName);
		}
		
		return null;
	}
	
	public void addHeaderField(String fieldName, String fieldValues) {
		headerFields.put(fieldName, fieldValues);
	}
	
	public void addBodyRow(String bodyRow) {
		bodyRows.add(bodyRow);
	}
	
}
