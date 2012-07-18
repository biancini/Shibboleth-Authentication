JAAS Module
===========

This folder contains all files that implement the JAAS module used to login with
Shibboleth regular Java applications.

This folder also contains an example of a client that authenticates with Shibboleth
and then uses the session cookie to call a webservice behind Shibboleth authentication.

A set of very simple APIs have been created to permit high level programming languages (used in real
applications) to leverage the authentication mechanisms implemented in this project.

For information about installing and using such APIs, refer to the following page of this wiki:

  [[Usage of JAAS and Python modules](/biancini/Shibboleth-Authentication/wiki/Use-JAAS-Python)]

### JAAS

The JAAS module permits to authenticate inside Java applications.
It uses the java.net and java.io classes to connect with the webserver and reads user session
information from the web pages served after authentication (in a very similar way to the PAM
module written in C).

To obtain more information on JAAS modules, visit:

  [http://docs.oracle.com/javase/1.4.2/docs/guide/security/jaas/JAASLMDevGuide.html]

