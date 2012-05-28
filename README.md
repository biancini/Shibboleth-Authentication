pam_http
========

Implementation of a PAM module to authenticate over HTTP Basich Auhentication
Copyright, 2012-2012, Andrea Biancini

This project consists of two parts. A PAM module and a NSS module.

PAM module
----------

The PAM module permits to authenticate over HTTP Basic Authentication.
It uses libcurl to interact with the webserver and reads user session information from the
web pages served after authentication.
 
To obtain more information on PAM modules, visit:

  [http://debian.securedservers.com/kernel/pub/linux/libs/pam/Linux-PAM-html/Linux-PAM_SAG.html]
  

NSS module
----------

The NSS module permits to obtain user information from a web page behind basic authentication.
The page must expose two different contents depending on query string:

  * if ?passwd is provided the page must supply a file with the same structure of
    /etc/passwd and listing all users defined by the server
  * if ?group is provided the page must supply a file with the same structure of
    /etc/group and listing all user groups defined by the server

To obtain more information on NSS modules, visit:

  [http://www.gnu.org/software/libc/manual/html_node/Name-Service-Switch.html#Name-Service-Switch]
