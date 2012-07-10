Configuration of IdP and SP to authenticate via HTTP Basic Authentication
=========================================================================


Goal of the configuration
-------------------------

Goal of this page is to describe the procedure to configure an IdP and a SP of Shibboleth to permit the user authentication via
the HTTP Basic authentication mechanism.

User authentication in Shibboleth is usually performed by presenting, to the user, a web for where he has to specify his username
and passowrd. This authentication mechanism is very effective and works very well for all the web-based application.
In these applications, in fact, the user points his browser to the URL of the application he wants to use. The applicatio, then,
via the typical mechanism of SP, redirects the user browser to a web page on the IdP where he can specify his username and password
and perform a proper authentication.

This authentication mechanism does not adact well to application that are not web-based. In these cases, in fact, the access from the
user may come from outside a web-browser and for this reason it could not be simple to execute the login (which, as sees, is based on
a HTML page shown from the IdP to the user's browser).

To circumvent this proble, in this page an alternative mechanism will be presented to permit IdP to verify users creantials via an
authentication mechanism based on HTTP Basic. This authentication mechanism does not modify the essence of Shibboleth, but permits to
be easily integrated in application which are not accessed via web.
As an exemple, with this module it will be possible ao authenticate Shibboleth users as system users of Linux boxes (via PAM and NSS
modules developed for this purpose).

In the following of this page the various configuration to permit HTTP Basic authentication of Shibboleth users are described.
The more intese configuration will be on IdP side, there will also be some modification at SP level to permit the non-web-based
authentication of users.


Configurations on the Identity Provider (IdP) side
--------------------------------------------------

As expected, the configuration of an HTTP Basic authentication mechanism for Shibboleth involves mainly the IdP.
In the following the steps to configure this authentication mechanism are presented.
The described configuration steps are applicable to working standard Shibboleth configuration using LDAP as a backend for authorization
information about users.

The steps for configuring this authentication mechanisms are the following:

### IdP with a configuration with Tomcat behind Apache HTTPd

The IdP used for the test of this module has been the one developed by Internet2.
It consists of a Java web application usually deployed on the Tomcat application server.
The IdP must be configured so that tomcat is accessed behind Apache HTTPd webserver.

This very typical configuration is usually done this way:

In the Tomcat configuration file `server.xml` it must be specified one single connector for AJP (the connector that permits Tomcat to
interact with HTTPd webserver):

    <!-- Define an AJP 1.3 Connector on port 8009 -->
    <Connector port="8009" address="127.0.0.1"
               enableLookups="false" redirectPort="443"
               protocol="AJP/1.3"
               tomcatAuthentication="false" />

In the Apache HTTPd configuration, for the specific VirtualHost, it must specified the configuration to work as a Proxy to the
AJP connector of Tomcat:

    <VirtualHost _default_:443>
          ProxyRequests off
          ProxyPass / ajp://127.0.0.1:8009/
          ProxyPassReverse / ajp://127.0.0.1:8009/
          SSLEngine on
          ....
    </VirtualHost>


### Apache HTTPd configuration to perform  HTTP Basic authentication

Apache HTTPd must be configuret to request the HTTP Basic authentication for user trying to access a specific URL used by the
IdP to authenticate the users.

This configuration of HTTPd requires to enable the Basic authentication by verifying the user credential in the LDAP server used
also by the IdP to authenticate the users and retrieve authorization data.

    <Location /idp/Authn/RemoteUser>
        AuthType Basic
        AuthName "Identity Provider Authentication"
        AuthBasicProvider ldap
        AuthLDAPURL ldap://127.0.0.1:389/ou=people,dc=example,dc=com?uid?sub
        AuthLDAPBindDN "cn=admin,dc=example,dc=com"
        AuthLDAPBindPassword password
        Require valid-user
    </Location>

The connection data to connect to LDAP is the same used by the IdP, and usually specified in the file `IDP_HOME/conf/login.config`.

### Addition of the specific login handler for Basic authentication

The IdP must be configured so that, in addition to the usual authentication handler, it activates also a second handler to permit HTTP
Basic authentication,

This configuration is performed in the `IDP_HOME/conf/handler.xml` file.

    <ph:LoginHandler xsi:type="ph:RemoteUser">
        <ph:AuthenticationMethod>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport:BasicAuthn</ph:AuthenticationMethod>
    </ph:LoginHandler>

### Configuration of the authnMethod parameter for the RemoteUserAuthHandler servlet
At last the IdP must be configured so that the servlet serving the RemoteUser handler use the authentication method specified in the
Shibboleth configuration (i.e. `urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport:BasicAuthn`).

This configuration is done in the idp web application on Tomcat.
The file `web.xml` must be modified so that the configuration for the servlet RemoteUserAuthHandler is as follows:

    <servlet>
        <servlet-name>RemoteUserAuthHandler</servlet-name>
        <servlet-class>edu.internet2.middleware.shibboleth.idp.authn.provider.RemoteUserAuthServlet</servlet-class>
        <load-on-startup>3</load-on-startup>
        <init-param>
            <param-name>authnMethod</param-name>
            <param-value>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport:BasicAuthn</param-value>
        </init-param>
    </servlet>

Configuration on Service Provider (SP) side
-------------------------------------------

To authenticate the users via HTTP Basic authentication, the SP must configure a specific SessionInitiator that uses the BasicAuthn
authentication method.
The configuration is done with the following steps:

### Configurations in the shibboleth2.xml file

    <SessionInitiator type="Chaining" Location="/BasicLogin" id="WebDAVLogin"
                      entityID="https://identityprovider.fqdn.com/idp/shibboleth"
                      target="https://serviceprovider.fqdn.com/">
         <SessionInitiator type="SAML2" defaultACSIndex="3" template="bindingTemplate.html"
                       outgoingBindings="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
                       authnContextClassRef="urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport:BasicAuthn"/>
    </SessionInitiator>

The `defaultACSIndex` (in this example set to 3) must reference to the AssertionConsumerService which uses the HTTP-Artifact mechanism.
In the shibboleth2.xml configuration file it must be specified:

    <md:AssertionConsumerService Location="/SAML2/Artifact" index="3"
           Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact"/>

As it is possible to see, the index of this AssertionConsumerService (3) corrisponds to the value specified in defaultACSIndex.

### Usage of the correct libCurl library

Shibboleth uses, for HTTP Assertion, the libCurl library. The more recent versions of Linux, notably the ones based on RH/Fedora,
use a libCurl library compiled in an compatible way with Netscape Security Services.
Shibboleth, on the other hands, uses different mechanisms for security.

For this reason, in order for IdP and SP to work correctly together, it is necessary to install the libcurl.so.4 library in the lib
folder in the location where the SP is installed (`SP_HOME/lib`).
This library can be downloaded from the Internet2 website, for more information refer to the link:

  [https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPLinuxRH6]

### Configuration of Apache to manage multiple authentication mechanisms
The SessionInitiator configured for the Basic authentication can be configured in addition to the SessionInitiator already present in
Shibboleth (accessed via WAYF or directly showing to the user the web-pages to perform the login via username and password).

In this case the Apache configuration must be modified in order to instruct a specific Location to use the Shibboleth authentication
on the newly defined SessionInitiator.

    <Location />
        ShibRequireSessionWith BasicLogin
        AuthType shibboleth
        ShibRequireSession On
        ShibUseHeaders On
        Require valid-user 
    </Location>

