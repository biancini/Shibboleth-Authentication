Shibboleth Autentication
========================

Implementation of a series of mechanisms to authenticate users on Shibboleth from non 
web-based application.

Inside this project the following modules are created:

  * a PAM module to authenticate over HTTP Basich Auhentication
  * a JAAS module to authenticate inside Java applications
  
Copyright, 2012-2012, Andrea Biancini


PAM module
----------

The PAM module permits to authenticate over HTTP Basic Authentication.
It uses libcurl to interact with the webserver and reads user session information from the
web pages served after authentication.
 
To obtain more information on PAM modules, visit:

  [http://debian.securedservers.com/kernel/pub/linux/libs/pam/Linux-PAM-html/Linux-PAM_SAG.html]
  
This module, actually, consists of two parts. A PAM module and a NSS module.

The NSS module permits to obtain user information from a web page behind basic authentication.
The page must expose two different contents depending on query string:

  * if ?passwd is provided the page must supply a file with the same structure of
    /etc/passwd and listing all users defined by the server
  * if ?group is provided the page must supply a file with the same structure of
    /etc/group and listing all user groups defined by the server

To obtain more information on NSS modules, visit:

  [http://www.gnu.org/software/libc/manual/html_node/Name-Service-Switch.html#Name-Service-Switch]


JAAS module
-----------

The JAAS module permits to authenticate inside Java applications.
It uses the java.net and java.io classes to connect with the webserver and reads user session
information from the web pages served after authentication (in a very similar way to the PAM
module written in C.

To obtain more information on JAAS modules, visit:

  [http://docs.oracle.com/javase/1.4.2/docs/guide/security/jaas/JAASLMDevGuide.html]
  