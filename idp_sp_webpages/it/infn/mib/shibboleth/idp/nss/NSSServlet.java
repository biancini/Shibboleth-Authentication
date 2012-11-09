package it.infn.mib.shibboleth.idp.nss;

import it.garr.shibboleth.idp.LdapConfigServlet;
import it.garr.shibboleth.idp.TimeLimitedCacheMap;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.SearchFilter;

public class NSSServlet extends HttpServlet {
	private static final long serialVersionUID = 1075071397951598150L;
	private static final String USERS_KEY = "users";
	private static final String GROUPS_KEY = "groups";
	private static TimeLimitedCacheMap cacheLdap = null;
	
	private Iterator<SearchResult> connectLdap() throws NamingException {
		// Connection with LDAP and query for users
		LdapConfig ldapConfig = new LdapConfig(LdapConfigServlet.getLdapUrl(), LdapConfigServlet.getBaseDN());
		ldapConfig.setBindDn(LdapConfigServlet.getBindDN());
		ldapConfig.setBindCredential(LdapConfigServlet.getCredential());
		Ldap ldap = new Ldap(ldapConfig);
	
		Iterator<SearchResult> results = ldap.search(new SearchFilter("(&(objectClass=inetOrgPerson)(uid=*))"), new String[]{"uid", "uidnumber", "gidnumber", "displayname", "homedirectory", "loginshell"});
		return results;
	}
	
	private HashMap<String, String> getListUsers() throws NamingException {
		if (cacheLdap == null) cacheLdap = new TimeLimitedCacheMap(2*60, 15, 2*60, TimeUnit.MINUTES);
		Iterator<SearchResult> results = connectLdap();
		
		if (!cacheLdap.containsKey(USERS_KEY)) {
			HashMap<String, String> mapUserValues = new HashMap<String, String>();
			
			while (results.hasNext()) {
				SearchResult curResult = results.next();
				
				String userValues = "";
				String uid = curResult.getAttributes().get("uid").get().toString(); 
				userValues += uid + ":x:";
				userValues += curResult.getAttributes().get("uidnumber").get().toString() + ":";
				userValues += curResult.getAttributes().get("gidnumber").get().toString() + ":";
				userValues += curResult.getAttributes().get("displayname").get().toString() + ":";
				userValues += curResult.getAttributes().get("homedirectory").get().toString() + ":";
				userValues += curResult.getAttributes().get("loginshell").get().toString();
				
				mapUserValues.put(uid, userValues);
			}
			
			cacheLdap.put(USERS_KEY, mapUserValues);
		}
		
		return cacheLdap.get(USERS_KEY);
	}
	
	private HashMap<String, String> getListGroups() throws NamingException {
		if (cacheLdap == null) cacheLdap = new TimeLimitedCacheMap(2*60, 15, 2*60, TimeUnit.MINUTES);
		Iterator<SearchResult> results = connectLdap();
		
		if (!cacheLdap.containsKey(GROUPS_KEY)) {
			HashMap<String, String> mapUserValues = new HashMap<String, String>();
			
			while (results.hasNext()) {
				SearchResult curResult = results.next();
				
				String groupValues = "";
				String gid = curResult.getAttributes().get("uid").get().toString(); 
				groupValues += gid + ":x:";
				
				mapUserValues.put(gid, groupValues);
			}
			
			cacheLdap.put(GROUPS_KEY, mapUserValues);
		}
		
		return cacheLdap.get(GROUPS_KEY);
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		String queryString = request.getQueryString();
		
		try {
			// Production of output file
			HashMap<String, String> elements = (queryString != null && queryString.equals("group")) ? getListGroups() : getListUsers();
			for (String curElement : elements.values()) {
				out.println(curElement);
			}			
		} catch (NamingException e) {
			getServletContext().log("Exception occurred while accessing LDAP.", e);
		}
	
	}
}
