package it.infn.mib.shibboleth.idp.nss;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

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
	private static String ldapUrl = null;
	private static String baseDN = null;
	
	public void init() throws ServletException {
		ldapUrl = getServletConfig().getInitParameter("ldapUrl");
		baseDN = getServletConfig().getInitParameter("baseDN");
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		String queryString = request.getQueryString();
		
		try {
			Ldap ldap = new Ldap(new LdapConfig(ldapUrl, baseDN));
			Iterator<SearchResult> results = ldap.search(new SearchFilter("(&(objectClass=inetOrgPerson)(uid=*))"), new String[]{"uid", "uidnumber", "gidnumber", "displayname", "homedirectory", "loginshell"});
			
			if (queryString.equals("?group")) {
				String allNames = null;
				while (results.hasNext()) {
					SearchResult curResult = results.next();
					out.print(curResult.getAttributes().get("uid").get().toString() + ":x:");
					out.println(curResult.getAttributes().get("uidnumber").get().toString() + ":");
					allNames = (allNames == null) ? "" : allNames + ",";
					allNames += curResult.getAttributes().get("uid");
				}
				out.println("shibuser:x:10000:" + allNames);
			}
			else {
				while (results.hasNext()) {
					SearchResult curResult = results.next();
					out.print(curResult.getAttributes().get("uid").get().toString() + ":x:");
					out.print(curResult.getAttributes().get("uidnumber").get().toString() + ":");
					out.print(curResult.getAttributes().get("gidnumber").get().toString() + ":");
					out.print(curResult.getAttributes().get("displayname").get().toString() + ":");
					out.print(curResult.getAttributes().get("homedirectory").get().toString() + ":");
					out.println(curResult.getAttributes().get("loginshell").get().toString());
				}
			}
		} catch (NamingException e) {
			e.printStackTrace();
		}
	
	}
}
