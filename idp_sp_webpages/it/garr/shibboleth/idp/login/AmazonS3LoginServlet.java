package it.garr.shibboleth.idp.login;

import it.infn.mib.shibboleth.idp.LdapConfigServlet;

import java.io.IOException;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.AuthenticationEngine;
import edu.internet2.middleware.shibboleth.idp.authn.AuthenticationException;
import edu.internet2.middleware.shibboleth.idp.authn.LoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.UsernamePrincipal;

public class AmazonS3LoginServlet extends HttpServlet {

	private static final long serialVersionUID = -5727998416723856990L;
	public static final String S3_AUTHN_CTX = "urn:garr:names:tc:SAML:2.0:ac:classes:AmazonS3";

	private final Logger log = LoggerFactory.getLogger(AmazonS3LoginServlet.class);

	private String authenticationMethod;
	private final String failureParam = "loginFailed";
	
	private static String salt = null;
	
	/** {@inheritDoc} */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		//salt = getServletConfig().getInitParameter("salt");
		salt = "770A8A65DA156D24EE2A093277530142";
		
		String method = DatatypeHelper.safeTrimOrNullString(config.getInitParameter(LoginHandler.AUTHENTICATION_METHOD_KEY));
		authenticationMethod = (method != null) ? method : S3_AUTHN_CTX;
		log.debug("");
	}

	/** {@inheritDoc} */
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.debug("Auhtenticating user with the following Amazon authentication strings:\n" +
				  "Authorization = " + request.getHeader("Authorization") + "\n" +
				  "stringToSign = " + request.getHeader("stringToSign"));
		
		try {
			if (authenticateUser(request)){
				AuthenticationEngine.returnToAuthenticationEngine(request, response);
			} else{
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			}
		} catch (LoginException e) {
			request.setAttribute(failureParam, "true");
			request.setAttribute(LoginHandler.AUTHENTICATION_EXCEPTION_KEY, new AuthenticationException(e));
			
			response.getWriter().println("Failure in authenticating request: " + e.getMessage());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
	}

	/**
	 * Authenticate a access and secret against the LDAP server used by Shibboleth. If authentication succeeds 
	 * the username is placed into the request in its respective attribute.
	 *
	 * @param request current authentication request
	 * @throws LoginException thrown if there is a problem authenticating the user
	 */
	protected boolean authenticateUser(HttpServletRequest request) throws LoginException {
		String accessKey = null;
		try {
			accessKey = S3AccessorMethods.getAccessKey(request);
		} catch (Exception e) {
			log.error("Error while retriving the accessKey: " + e);
			throw new LoginException("Error while retriving the accessKey.");
		} 
		
		try {
			log.debug("Attempting to authenticate user {}", accessKey);
			
			log.debug("Trying to connect to ldap: " + LdapConfigServlet.getLdapUrl() + " with baseDN: " + LdapConfigServlet.getBaseDN());
			S3AccessorMethods.connectLdap(LdapConfigServlet.getLdapUrl(), LdapConfigServlet.getBaseDN(), LdapConfigServlet.getBindDN(), LdapConfigServlet.getCredential(), accessKey);
			
			if (S3AccessorMethods.getUsername(accessKey) != null){
				log.debug("Utente " + accessKey + " trovato nel db ldap" + "\nDati utente: \n" + S3AccessorMethods.printUserParameters());
			} else {
				log.debug("Utente " + accessKey + " non trovato in ldap");
			}
			
			String stringToSign = S3AccessorMethods.getStringToSign(request);
			String secretKey = S3AccessorMethods.getSecretKey(salt);
			String calculatedSecret = S3AccessorMethods.encryptSignature(stringToSign, secretKey);
			String incomingSecret = S3AccessorMethods.getIncomingAuthorization(request, accessKey);
			String username = S3AccessorMethods.getUsername(accessKey);
			
			log.debug("Rolled out Amazon authentication params:\n" +
					  "accessKey = " + accessKey + "\n" +
					  "stringToSign = " + stringToSign + "\n" +
					  "calculatedSecret = " + calculatedSecret + "\n" + 
					  "incomingSecret = " + incomingSecret);
			
			if (stringToSign == null || calculatedSecret == null) return false;
			
			if (!calculatedSecret.equals(incomingSecret)) throw new LoginException("Denying authorization to user.");
			log.debug("Successfully authenticated user {}", accessKey);
			
			request.setAttribute(LoginHandler.PRINCIPAL_NAME_KEY, username);
			request.setAttribute(LoginHandler.PRINCIPAL_KEY, new UsernamePrincipal(username));
			request.setAttribute(LoginHandler.AUTHENTICATION_METHOD_KEY, authenticationMethod);
			return true;
		} catch (LoginException e) {
			log.error("User authentication for " + accessKey + " failed");
			throw e;
		} catch (Throwable e) {
			log.error("User authentication for " + accessKey + " failed");
			throw new LoginException("Unknown authentication error.");
		}
	}

}
