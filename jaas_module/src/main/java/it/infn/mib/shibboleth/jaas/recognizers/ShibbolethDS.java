package it.infn.mib.shibboleth.jaas.recognizers;

import it.infn.mib.shibboleth.jaas.impl.HTTPException;
import it.infn.mib.shibboleth.jaas.impl.IRecognizer;

import java.io.IOException;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

/**
 * Object representing an HTTP Page recognizer for the Shibboleth DS.
 * 
 * @author Andrea Biancini <andrea.biancini@gmail.com>
 * @author Simon Vocella <voxsim@gmail.com>
 * @version 1.0, 05/03/2014
 */
public class ShibbolethDS implements IRecognizer {
	
	private static final String SHIBBOLETH_XPATH_FORM = "//form";
	private static final String SHIBBOLETH_XPATH_SUBMIT = "//input[@value=\"Select\"]";
	private static final String SHIBBOLETH_ORIGIN_FIELD = "origin";

	/**
	 * {@inheritDoc}
	 */
	public boolean isThisUrl(String htmlCurWebPageText) {
		// TODO: this verification mechanism for Shibboleth DS is too weak...
		return htmlCurWebPageText.contains("/dsc/DS");
	}

	/**
	 * {@inheritDoc}
	 */
	public Page processUrl(Page curWebPage, String username, String password, Integer selection) throws HTTPException {
		if(!curWebPage.isHtmlPage()) {
			throw new HTTPException("The page is not a HTML page");
		}
		
		HtmlPage htmlCurWebPage = (HtmlPage) curWebPage;
		
		final HtmlForm form = (HtmlForm) htmlCurWebPage.getByXPath(SHIBBOLETH_XPATH_FORM).get(1);
		final HtmlSubmitInput submit = (HtmlSubmitInput) form.getFirstByXPath(SHIBBOLETH_XPATH_SUBMIT);
		final HtmlSelect originField = form.getSelectByName(SHIBBOLETH_ORIGIN_FIELD);
		
		originField.setSelectedAttribute(originField.getOptions().get(selection), true);
		try {
			curWebPage = submit.click();
		} catch (IOException e) {
			throw new HTTPException("Error during page processing", e);
		}
		
		return curWebPage;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean continueTheChain() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getChoices(Page curWebPage) {
		HtmlPage htmlCurWebPage = (HtmlPage) curWebPage;
		
		final HtmlForm form = (HtmlForm) htmlCurWebPage.getByXPath(SHIBBOLETH_XPATH_FORM).get(1);
		final HtmlSelect originField = form.getSelectByName(SHIBBOLETH_ORIGIN_FIELD);
		
		String[] choices = new String[originField.getOptions().size()];
		int i = 0;
		for(HtmlOption option : originField.getOptions()) {
			choices[i] = option.getValueAttribute();
			i++;
		}
		
		return choices;
	}

}
