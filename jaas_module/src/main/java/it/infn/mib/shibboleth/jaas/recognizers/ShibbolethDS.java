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

public class ShibbolethDS implements IRecognizer {
	
	private static final String SHIBBOLETH_XPATH_FORM = "//form";
	private static final String SHIBBOLETH_XPATH_SUBMIT = "//input[@value=\"Select\"]";
	private static final String SHIBBOLETH_ORIGIN_FIELD = "origin";

	public boolean isThisUrl(String htmlCurWebPageText) {
		return htmlCurWebPageText.contains("/dsc/DS");
	}

	public Page processUrl(Page curWebPage, String username, String password, Integer selection) throws HTTPException, IOException {
		if(!curWebPage.isHtmlPage()) {
			throw new HTTPException("The page is not a HTML page");
		}
		
		HtmlPage htmlCurWebPage = (HtmlPage) curWebPage;
		
		final HtmlForm form = (HtmlForm) htmlCurWebPage.getByXPath(SHIBBOLETH_XPATH_FORM).get(1);
		final HtmlSubmitInput submit = (HtmlSubmitInput) form.getFirstByXPath(SHIBBOLETH_XPATH_SUBMIT);
		final HtmlSelect originField = form.getSelectByName(SHIBBOLETH_ORIGIN_FIELD);
		
		originField.setSelectedAttribute(originField.getOptions().get(selection), true);
		curWebPage = submit.click();
		
		return curWebPage;
	}

	public boolean continueTheChain() {
		return true;
	}

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
