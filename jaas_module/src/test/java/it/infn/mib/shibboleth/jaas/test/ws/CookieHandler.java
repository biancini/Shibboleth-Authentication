package it.infn.mib.shibboleth.jaas.test.ws;

import java.util.Map;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.transport.http.HTTPConstants;

public class CookieHandler extends BasicHandler {
	private static final long serialVersionUID = -2222269423530332417L;
	
	private Map<String,String> cookies = null;
	public CookieHandler(Map<String,String> cookies) {
		this.cookies = cookies;
	}
	
	public void invoke(MessageContext msgContext) throws AxisFault {
		if (cookies != null) {
			String curCookies = "";
			
			for (String curKey : cookies.keySet()) {
				curCookies += curKey + "=" + cookies.get(curKey) + ";";
			}
			
			msgContext.setProperty(HTTPConstants.HEADER_COOKIE, curCookies);
		}
	}
}



