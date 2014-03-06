package it.infn.mib.shibboleth.jaas.impl;

import com.gargoylesoftware.htmlunit.Page;

/**
 * Object representing an HTTP Page recognizer.
 * 
 * @author Andrea Biancini <andrea.biancini@gmail.com>
 * @author Simon Vocella <voxsim@gmail.com>
 * @version 1.0, 05/03/2014
 */
public interface IRecognizer {
	/**
	 * Function that verifies if the current page has to be processed by this recognizer.
	 * 
	 * @param htmlCurWebPageText the body text of the current webpage
	 * @return true or false depending it this recognizer is able to manage the current page or not
	 */
	public boolean isThisUrl(String htmlCurWebPageText);
	
	/**
	 * Function that processes the current page.
	 * 
	 * @param curWebPage the page to be opened
	 * @param username the username to be passed for authentication, if required
	 * @param password the password to be passed for authentication, if required
	 * @param selection the IdP selection index to be chosen from WAYF/DS, if requested
	 * @throws HTTPException if an exception rises during page processing
	 * @return the Page processed
	 */
	public Page processUrl(Page curWebPage, String username, String password, Integer selection) throws HTTPException;
	
	/**
	 * Function that indicates whether the recognizer chain must be continued or broken.
	 * 
	 * @return true or false indicating whether the recognizer chain must be continuer or not
	 */
	public boolean continueTheChain();
	
	/**
	 * Function that provides the choices available to the user to choose from WAYF/DS, if available
	 * 
	 * @param curWebPage the current webpage for the WAYF/DS
	 * @return an array of String containing IdP names for the user choice
	 */
	public String[] getChoices(Page curWebPage);
}
