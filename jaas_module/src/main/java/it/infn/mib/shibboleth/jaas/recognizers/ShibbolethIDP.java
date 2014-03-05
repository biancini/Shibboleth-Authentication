package it.infn.mib.shibboleth.jaas.recognizers;

import it.infn.mib.shibboleth.jaas.impl.HTTPException;
import it.infn.mib.shibboleth.jaas.impl.IRecognizer;

import java.io.IOException;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

/**
 * Object representing an HTTP Page recognizer for the Shibboleth IdP.
 * 
 * @version 1.0, 05/03/2014
 */
public class ShibbolethIDP implements IRecognizer {
	
	private static final String SHIBBOLETH_XPATH_FORM = "//form";
	private static final String SHIBBOLETH_XPATH_BUTTON = "//button";
	private static final String SHIBBOLETH_USERNAME_FIELD = "j_username";
	private static final String SHIBBOLETH_PASSWORD_FIELD = "j_password";

	/**
	 * {@inheritDoc}
	 */
	public boolean isThisUrl(String htmlCurWebPageText) {
		return htmlCurWebPageText.contains("j_username") && htmlCurWebPageText.contains("j_password");
	}

	/**
	 * {@inheritDoc}
	 */
	public Page processUrl(Page curWebPage, String username, String password, Integer selection) throws HTTPException {
		if(!curWebPage.isHtmlPage()) {
			throw new HTTPException("The page is not a HTML page");
		}
		
		HtmlPage htmlCurWebPage = (HtmlPage) curWebPage;
		
		final HtmlForm form = (HtmlForm) htmlCurWebPage.getByXPath(SHIBBOLETH_XPATH_FORM).get(0);
		final HtmlButton button = (HtmlButton) form.getByXPath(SHIBBOLETH_XPATH_BUTTON).get(0);
		final HtmlTextInput usernameField = form.getInputByName(SHIBBOLETH_USERNAME_FIELD);
		final HtmlPasswordInput passwordField = form.getInputByName(SHIBBOLETH_PASSWORD_FIELD);
		
		usernameField.setValueAttribute(username);
		passwordField.setValueAttribute(password);

		try {
			curWebPage = button.click();
		} catch (IOException e) {
			throw new HTTPException("Error during page processing", e);
		}
		
		if(curWebPage.isHtmlPage()) {
			throw new HTTPException("The page result is not a Text page");
		}
		
		return curWebPage;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean continueTheChain() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getChoices(Page curWebPage) {
		return null;
	}

}
