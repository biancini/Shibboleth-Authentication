package it.infn.mib.shibboleth.idp;

import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vt.middleware.ldap.Ldap;
import edu.vt.middleware.ldap.LdapConfig;
import edu.vt.middleware.ldap.SearchFilter;

public class LdapConfigServlet extends HttpServlet {
	
	private final Logger log = LoggerFactory.getLogger(LdapConfigServlet.class);
	private static final long serialVersionUID = -12610378821702164L;
	
	private static String ldapUrl = null;
	private static String baseDN = null;
	private static String bindDN = null;
	private static String credential = null;
	private static String salt = null;
	
	public void init() throws ServletException {
		ldapUrl = getServletConfig().getInitParameter("ldapUrl");
		baseDN = getServletConfig().getInitParameter("baseDN");
		bindDN = getServletConfig().getInitParameter("bindDN");
		credential = getServletConfig().getInitParameter("credential");
		salt = getServletConfig().getInitParameter("salt");
		
		try {
		if(ldapUrl.equals("") || baseDN.equals("") || bindDN.equals("") || credential.equals("") || salt.equals(""));		
			} catch (Exception e1) {
				log.error("Parametri mancanti nel file di configurazione.");
			}
		
		LdapConfig ldapConfig = new LdapConfig(LdapConfigServlet.getLdapUrl(), LdapConfigServlet.getBaseDN());
		ldapConfig.setBindDn(LdapConfigServlet.getBindDN());
		ldapConfig.setBindCredential(LdapConfigServlet.getCredential());
		Ldap ldap = new Ldap(ldapConfig);
		
		try {
			Iterator<SearchResult> results = ldap.search(new SearchFilter("(&(objectClass=inetOrgPerson)(uid=*))"), new String[]{"uid", "userPassword"});
			
			boolean foundUserPassword = false;
			while (results.hasNext()) {
				if(((SearchResult) results.next()).getAttributes().get("userPassword").get() != null) foundUserPassword = true;
			}
			
			if (foundUserPassword)
				log.info("LDAP configuration properly working.");
			else
				log.error("Attention, the browse of the LDAP do not returned any user with a valid password. " +
						  "The problem may be due to a misconfiguration of the user accessing LDAP.\n" +
						  "The user used for accessing the LDAP server must have read rights on the 'userPassword' field of the 'inetOrgPerson' schema.");
			
		} catch (NamingException e) {
			log.error("Attention, the browse of the LDAP was not possible. " +
					  "The problem may be due to a misconfiguration of the LDAP access in web.xml.", e);
		}
		
	}
	
	public static String getLdapUrl() {
		return ldapUrl;
	}
	
	public static String getBaseDN() {
		return baseDN;
	}
	
	public static String getBindDN() {
		return bindDN;
	}
	
	public static String getCredential() {
		return credential;
	}
	
	public static String getSalt() {
		return salt;
	}
	
}
