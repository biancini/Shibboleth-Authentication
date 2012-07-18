S3 Authentication
=================

This folder contains all files that implement a S3 authentication mechanism on the 
Shibboleth IdP.

The S3 LoginHandler permits an authentication via the standard Amazon S3 authorization mechanisms.
This mechanims uses a couple of keys called access key and secret key, as described in this
page:

  [http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html]

The LoginHandler has been implemented following the Internet2 specification described at this page:

  [https://wiki.shibboleth.net/confluence/display/SHIB2/IdPDevExtLoginHandler]

This extension to the Shibboleth IdP implements also a new SAML attribute to be provided to SP.
The SAML attribute contains the S3 authorization key (to be used with the secret key, kept only on the IdP and
sent to the user via the specific portlet).

The attribute is implemented by extending data connector as specified here:

  [https://wiki.shibboleth.net/confluence/display/SHIB2/IdPDevExtDataCtr]
  [https://wiki.shibboleth.net/confluence/display/SHIB2/IdPAddAttribute]

The S3 authentication mechanism can be installed following the documentation on this wiki at the page:

  [[Installation instructions for S3 login handler](/biancini/Shibboleth-Authentication/wiki/Install-S3)]
