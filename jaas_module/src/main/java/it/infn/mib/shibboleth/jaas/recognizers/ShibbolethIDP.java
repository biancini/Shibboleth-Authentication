package it.infn.mib.shibboleth.jaas.recognizers;

import it.infn.mib.shibboleth.jaas.impl.HTTPException;
import it.infn.mib.shibboleth.jaas.impl.IRecognizer;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

/**
 * Object representing an HTTP Page recognizer for the Shibboleth IdP.
 * 
 * @author Andrea Biancini <andrea.biancini@gmail.com>
 * @author Simon Vocella <voxsim@gmail.com>
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
		return htmlCurWebPageText.contains(SHIBBOLETH_USERNAME_FIELD) && htmlCurWebPageText.contains(SHIBBOLETH_PASSWORD_FIELD);
	}

	/**
	 * {@inheritDoc}
	 */
	public Page processUrl(Page curWebPage, String username, String password, String entityid) throws HTTPException {
		if(!curWebPage.isHtmlPage()) {
			throw new HTTPException("The page is not a HTML page");
		}
		
		try
		{
			HtmlPage htmlCurWebPage = (HtmlPage) curWebPage;
			
			final HtmlForm form = (HtmlForm) htmlCurWebPage.getFirstByXPath(SHIBBOLETH_XPATH_FORM);
			final HtmlButton button = (HtmlButton) form.getFirstByXPath(SHIBBOLETH_XPATH_BUTTON);
			final HtmlTextInput usernameField = form.getInputByName(SHIBBOLETH_USERNAME_FIELD);
			final HtmlPasswordInput passwordField = form.getInputByName(SHIBBOLETH_PASSWORD_FIELD);
			
			usernameField.setValueAttribute(username);
			passwordField.setValueAttribute(password);
			curWebPage = button.click();
			
			if (curWebPage.isHtmlPage()) {
				throw new HTTPException("The page result is not a Text page");
			}
			
			return curWebPage;
		} catch(Exception e) {
			throw new HTTPException("Something going wrong", e);
		}
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
