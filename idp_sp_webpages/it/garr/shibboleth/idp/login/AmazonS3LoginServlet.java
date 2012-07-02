package it.garr.shibboleth.idp.login;

import java.io.IOException;

import javax.security.auth.Subject;
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

public class AmazonS3LoginServlet extends HttpServlet {

	private static final long serialVersionUID = -5727998416723856990L;
	public static final String S3_AUTHN_CTX = "urn:garr:names:tc:SAML:2.0:ac:classes:AmazonS3";

	private final Logger log = LoggerFactory.getLogger(AmazonS3LoginServlet.class);

	private String authenticationMethod;
	private final String failureParam = "loginFailed";

	/** {@inheritDoc} */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		String method = DatatypeHelper.safeTrimOrNullString(config.getInitParameter(LoginHandler.AUTHENTICATION_METHOD_KEY));
		authenticationMethod = (method != null) ? method : S3_AUTHN_CTX;
		log.debug("");
	}

	/** {@inheritDoc} */
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.debug("Auhtenticating user with the following Amazon authentication strings:\n" +
				  "authorization = " + request.getHeader("authorization") + "\n" +
				  "stringToSign = " + request.getHeader("stringToSign"));
		
		try {
			if (authenticateUser(request)){
				request.setAttribute(failureParam, "false");
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
	 * Authenticate a username and password against JAAS. If authentication succeeds the name of the first principal, or
	 * the username if that is empty, and the subject are placed into the request in their respective attributes.
	 *
	 * @param request current authentication request
	 *
	 * @throws LoginException thrown if there is a problem authenticating the user
	 */
	protected boolean authenticateUser(HttpServletRequest request) throws LoginException {
		String accessKey = S3AccessorMethods.getAccessKey(request);
		if (accessKey == null) return false;
		
		try {
			log.debug("Attempting to authenticate user {}", accessKey);
			
			String stringToSign = S3AccessorMethods.getStringToSign(request);
			//String stringToSign = S3AccessorMethods.createStringToSign(request);
			String calculatedSecret = S3AccessorMethods.encryptSignature(stringToSign, accessKey);
			String incomingSecret = S3AccessorMethods.getSecretKey(accessKey);
			
			log.debug("Rolled out Amazon authentication params:\n" +
					  "accessKey = " + accessKey + "\n" +
					  "stringToSign = " + stringToSign + "\n" +
					  "calculatedSecret = " + calculatedSecret);
			
			if (stringToSign == null || calculatedSecret == null) return false;
			
			if (!calculatedSecret.equals(incomingSecret)) throw new LoginException("Denying authorization to user.");
			log.debug("Successfully authenticated user {}", accessKey);

			Subject userSubject = new Subject(false, null, null, null);
			request.setAttribute(LoginHandler.SUBJECT_KEY, userSubject);
			request.setAttribute(LoginHandler.AUTHENTICATION_METHOD_KEY, authenticationMethod);
			return true;
		} catch (LoginException e) {
			log.debug("User authentication for " + accessKey + " failed", e);
			throw e;
		} catch (Throwable e) {
			log.debug("User authentication for " + accessKey + " failed", e);
			throw new LoginException("unknown authentication error");
		}
	}
}
