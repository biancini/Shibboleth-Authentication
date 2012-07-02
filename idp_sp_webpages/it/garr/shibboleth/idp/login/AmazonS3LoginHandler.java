/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.garr.shibboleth.idp.login;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.util.URLBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.provider.AbstractLoginHandler;
import edu.internet2.middleware.shibboleth.idp.authn.provider.UsernamePasswordCredential;

/**
 * Authenticate a username and password against a JAAS source.
 * 
 * This login handler creates a {@link javax.security.auth.Subject} and binds it to the request as described in the
 * {@link edu.internet2.middleware.shibboleth.idp.authn.LoginHandler} documentation. If the JAAS module does not create
 * a principal for the user a {@link edu.internet2.middleware.shibboleth.idp.authn.UsernamePrincipal} is created, using
 * the entered username. If the <code>storeCredentialsInSubject</code> init parameter of the authentication servlet is
 * set to true a {@link UsernamePasswordCredential} is created, based on the entered username and password, and stored
 * in the Subject's private credentials.
 */
public class AmazonS3LoginHandler extends AbstractLoginHandler {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AmazonS3LoginHandler.class);

    /** The context-relative path of the servlet used to perform authentication. */
    private String authenticationServletURL;

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