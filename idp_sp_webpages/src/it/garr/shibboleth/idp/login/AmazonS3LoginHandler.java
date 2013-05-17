package it.garr.shibboleth.idp.login;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.util.URLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.provider.AbstractLoginHandler;

/**
 * Authenticate a secret and access key (Amazon S3 standard) againsta the LDAP used by Shibboleth.
 * 
 * This login handler creates a {@link edu.internet2.middleware.shibboleth.idp.authn.UsernamePrincipal} using
 * the entered username.
 */
public class AmazonS3LoginHandler extends AbstractLoginHandler {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AmazonS3LoginHandler.class);

    /** The context-relative path of the servlet used to perform authentication. */
    private String authenticationServletURL = null;

    /**
     * Constructor.
     * 
     * @param servletPath context-relative path to the authentication servlet, may start with "/"
     */
    public AmazonS3LoginHandler(String servletPath) {
        super();
        setSupportsPassive(false);
        setSupportsForceAuthentication(true);
        authenticationServletURL = servletPath;
    }

    public void login(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        // forward control to the servlet
        try {
            StringBuilder pathBuilder = new StringBuilder();
            pathBuilder.append(httpRequest.getContextPath());
            if(!authenticationServletURL.startsWith("/")){
                pathBuilder.append("/");
            }

            pathBuilder.append(authenticationServletURL);

            URLBuilder urlBuilder = new URLBuilder();
            urlBuilder.setScheme(httpRequest.getScheme());
            urlBuilder.setHost(httpRequest.getServerName());
            urlBuilder.setPort(httpRequest.getServerPort());
            urlBuilder.setPath(pathBuilder.toString());

            log.debug("Redirecting to {}", urlBuilder.buildURL());
            httpResponse.sendRedirect(urlBuilder.buildURL());
            return;

        } catch (IOException ex) {
            log.error("Unable to redirect to authentication servlet.", ex);
        }
    }
    
//	@Override
//	public List<String> getSupportedAuthenticationMethods() {
//		List<String> supportedMethods = new ArrayList<String>();
//		supportedMethods.add("urn:garr:names:tc:SAML:2.0:ac:classes:AmazonS3");
//		return supportedMethods;
//	}
}