package it.infn.mib.shibboleth.jaas.impl;

import java.io.IOException;

import com.gargoylesoftware.htmlunit.Page;

public interface IRecognizer {
	public boolean isThisUrl(String htmlCurWebPageText);
	public Page processUrl(Page curWebPage, String username, String password, Integer selection) throws HTTPException, IOException;
	public boolean continueTheChain();
	public String[] getChoices(Page curWebPage);
}
