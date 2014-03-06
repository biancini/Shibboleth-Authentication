package it.infn.mib.shibboleth.jaas.recognizers;

import it.infn.mib.shibboleth.jaas.impl.HTTPException;
import it.infn.mib.shibboleth.jaas.impl.IRecognizer;

import java.io.IOException;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

/**
 * Object representing an HTTP Page recognizer for the SimpleSAML IdP.
 * 
 * @author Andrea Biancini <andrea.biancini@gmail.com>
 * @author Simon Vocella <voxsim@gmail.com>
 * @version 1.0, 05/03/2014
 */
public class SimpleSAMLIDP implements IRecognizer {
	
	private static final String SHIBBOLETH_XPATH_FORM = "//form";
	private static final String SHIBBOLETH_XPATH_SUBMIT = "//input[@value=\"Login\"]";
	private static final String SHIBBOLETH_USERNAME_FIELD = "username";
	private static final String SHIBBOLETH_PASSWORD_FIELD = "password";

	/**
	 * {@inheritDoc}
	 */
	public boolean isThisUrl(String htmlCurWebPageText) {
		return htmlCurWebPageText.contains(SHIBBOLETH_USERNAME_FIELD) && htmlCurWebPageText.contains(SHIBBOLETH_PASSWORD_FIELD);
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
		final HtmlSubmitInput submit = (HtmlSubmitInput) form.getByXPath(SHIBBOLETH_XPATH_SUBMIT).get(0);
		final HtmlTextInput usernameField = form.getInputByName(SHIBBOLETH_USERNAME_FIELD);
		final HtmlPasswordInput passwordField = form.getInputByName(SHIBBOLETH_PASSWORD_FIELD);
		
		usernameField.setValueAttribute(username);
		passwordField.setValueAttribute(password);

		try {
			curWebPage = submit.click();
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
