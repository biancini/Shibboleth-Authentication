Installation instructions for S3 login handler
==============================================

To install the S3 login handler some specific information and configuration is required on the IdP and SP side of the
authentication mechanisms.

The code developed integrates with the idp webapplication developed by Internet2 as a standard implementation of Shibboleth.
The version of code used as a base for development is shibboleth-identityprovider-2.3.5 available from
  [http://shibboleth.net/downloads/identity-provider/2.3.5/]


Java jar deployment
-------------------

The Java classes developed in idp_sp_webpages must be compiled and included in a jar together with the folders:
`META-INF`, `WEB-INF` and `schema`.

The default location for this jar file is within the `WEB-INF/lib` jar files of the idp webapplication provided by Internet2.


Pubblish of the servlets and handlers
-------------------------------------

The servlets and handlers developed must be defined in the `web.xml` in the `WEB-INF` of the idp webapplications.
The modification to be added are:

    <!-- Servlet with LDAP configurations -->
    <servlet>
        <servlet-name>LdapConfig</servlet-name>
        <servlet-class>it.garr.shibboleth.idp.LdapConfigServlet</servlet-class>
        <load-on-startup>3</load-on-startup>
        <init-param>
             <param-name>ldapUrl</param-name>
             <param-value>ldap://127.0.0.1:389</param-value>
        </init-param>
        <init-param>
             <param-name>baseDN</param-name>
             <param-value>ou=people,dc=example,dc=com</param-value>
        </init-param>
        <init-param>
             <param-name>bindDN</param-name>
             <param-value>cn=admin,dc=example,dc=com</param-value>
        </init-param>
        <init-param>
             <param-name>credential</param-name>
             <param-value>password</param-value>
        </init-param>
        <init-param>
             <param-name>salt</param-name>
             <param-value>770A8A64551D51A51151551651D512</param-value>
        </init-param>
    </servlet>

    <!-- Servlet for doing S3 authentication -->
    <servlet>
        <servlet-name>AmazonS3AuthHandler</servlet-name>
        <servlet-class>it.garr.shibboleth.idp.login.AmazonS3LoginServlet</servlet-class>
        <load-on-startup>3</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>AmazonS3AuthHandler</servlet-name>
        <url-pattern>/Authn/AmazonS3</url-pattern>
    </servlet-mapping>

    <!-- Servlet for retrieving S3 credentials -->
    <servlet>
        <servlet-name>AmazonS3Retrieve</servlet-name>
        <servlet-class>it.garr.shibboleth.idp.retrieves3credentials.AmazonS3RetrieveServlet</servlet-class>
        <load-on-startup>3</load-on-startup>
        <init-param>
             <param-name>mailHost</param-name>
             <param-value>servermail.hostname.com</param-value>
        </init-param>
        <init-param>
             <param-name>mailHostPort</param-name>
             <param-value>465</param-value>
        </init-param>
        <init-param>
             <param-name>useSSL</param-name>
             <param-value>true</param-value>
        </init-param>
        <init-param>
             <param-name>mailUser</param-name>
             <param-value>technicaluser</param-value>
        </init-param>
        <init-param>
             <param-name>mailPass</param-name>
             <param-value>mailpassword</param-value>
        </init-param>
        <init-param>
             <param-name>mailFrom</param-name>
             <param-value>technical_email@example.com</param-value>
        </init-param>
        <init-param>
             <param-name>mailSubject</param-name>
             <param-value>Amazon S3 SecretKey</param-value>
        </init-param>
        <init-param>
             <param-name>mailText</param-name>
             <param-value><![CDATA[<h1><font color='#0099CC'><b>&emsp;Amazon S3 Authentication information</b></font></h1><br/>
                                   <i>&emsp;&emsp;Dear $USER$,<br/><br/>
                                   &emsp;&emsp;As you requested Your <b>Amazon S3</b> Secret Key is reported below.</i><br/><br/><br/>
                                   <b>&emsp;&emsp;Secret Key:</b>&nbsp;$SECRET_KEY$<br/><br/><br/><br/>
                                   &emsp;&emsp;<i>Best reguards,<br/>&emsp;&emsp;The technical administrator</i>]]></param-value>
        </init-param>
    </servlet>

    <servlet-mapping>
        <servlet-name>AmazonS3Retrieve</servlet-name>
        <url-pattern>/retrieveS3</url-pattern>
    </servlet-mapping>

    <!-- Servlet added for PAM/NSS authentication -->

    <servlet>
        <servlet-name>nss_jsp</servlet-name>
        <servlet-class>it.infn.mib.shibboleth.idp.nss.NSSServlet</servlet-class>
    </servlet>

    <servlet-mapping>
        <servlet-name>nss_jsp</servlet-name>
        <url-pattern>/nss</url-pattern>
    </servlet-mapping>


Configuration of the new LoginHandler
-------------------------------------

The configuration of the login handler is done in `IDP_HOME/conf/handler.xml` file.
The modifications to be added are:

    <ph:ProfileHandlerGroup xmlns:ph="urn:mace:shibboleth:2.0:idp:profile-handler"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                        xmlns:garr="http://garr.it/shibboleth/authn"
                        xsi:schemaLocation="urn:mace:shibboleth:2.0:idp:profile-handler classpath:/schema/shibboleth-2.0-idp-profile-handler.xsd
                                            http://garr.it/shibboleth/authn classpath:/schema/amazons3-profile-handler.xsd">
        ...
        <ph:LoginHandler xsi:type="garr:AmazonS3">
            <ph:AuthenticationMethod>urn:garr:names:tc:SAML:2.0:ac:classes:AmazonS3</ph:AuthenticationMethod>
        </ph:LoginHandler>
        ...
    </ph:ProfileHandlerGroup>

In the first line, the opening tag of the XML has been modified to include the `xmlns:garr` namespace and the schema location
`http://garr.it/shibboleth/authn classpath:/schema/amazons3-profile-handler.xsd`.

Then a new LoginHandler is defined with the type `garr:AmazonS3` and the proper S3 authentication method.

Configuration of the new attribute resolver
-------------------------------------------

The configuration of the attribute resolver is done in `IDP_HOME/conf/attribute-resolver.xml`.
The modification to be added are:

    <resolver:AttributeResolver xmlns:resolver="urn:mace:shibboleth:2.0:resolver" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                            xmlns:pc="urn:mace:shibboleth:2.0:resolver:pc" xmlns:ad="urn:mace:shibboleth:2.0:resolver:ad" 
                            xmlns:dc="urn:mace:shibboleth:2.0:resolver:dc" xmlns:enc="urn:mace:shibboleth:2.0:attribute:encoder" 
                            xmlns:sec="urn:mace:shibboleth:2.0:security" 
                            xsi:schemaLocation="urn:mace:shibboleth:2.0:resolver classpath:/schema/shibboleth-2.0-attribute-resolver.xsd
                                               urn:mace:shibboleth:2.0:resolver:pc classpath:/schema/shibboleth-2.0-attribute-resolver-pc.xsd
                                               urn:mace:shibboleth:2.0:resolver:ad classpath:/schema/shibboleth-2.0-attribute-resolver-ad.xsd
                                               urn:mace:shibboleth:2.0:resolver:dc classpath:/schema/shibboleth-2.0-attribute-resolver-dc.xsd
                                               urn:mace:shibboleth:2.0:attribute:encoder classpath:/schema/shibboleth-2.0-attribute-encoder.xsd
                                               urn:mace:shibboleth:2.0:security classpath:/schema/shibboleth-2.0-security.xsd
                                               urn:garr.it:shibboleth:2.0:resolver classpath:/schema/garr-attribute-connector.xsd">

        ...
        <resolver:AttributeDefinition id="amazonS3AccessKey" xsi:type="ad:Simple" xmlns="urn:garr.it:shibboleth:2.0:resolver">
            <resolver:Dependency ref="myGarrConnector" />
            <resolver:AttributeEncoder xsi:type="enc:SAML1String" name="urn:mace:dir:attribute-def:amazons3accesskey" />
            <resolver:AttributeEncoder xsi:type="enc:SAML2String" name="urn:oid:0.9.2342.19200300.500.1.1" friendlyName="amazonS3AccessKey" />
        </resolver:AttributeDefinition>
        ...
        <resolver:DataConnector id="myGarrConnector" xsi:type="GarrConnector"
                                xmlns="urn:garr.it:shibboleth:2.0:resolver">
        </resolver:DataConnector>
        ...
    </resolver:AttributeResolver>

The first line, the opening tag of the XML has been modified to include the schema location 
`urn:garr.it:shibboleth:2.0:resolver classpath:/schema/garr-attribute-connector.xsd`.

Then a new attribute is defined called `amazonS3AccessKey` in the proper namespace.

At last a new DataConnector is defined to implement the Garr attribute resolver which computes the S3 access key and returns it to the
user session

For this parameter to be passed to the session of the SP, it must be declared in the `IDP_HOME/conf/attribute-filter.xml` file so that
it does not get filtered out for the SP.
An example configuration of this filter is:

    <afp:AttributeRule attributeID="amazonS3AccessKey">
        <afp:PermitValueRule xsi:type="basic:ANY" />
    </afp:AttributeRule>

