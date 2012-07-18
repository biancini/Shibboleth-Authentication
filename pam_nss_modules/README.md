PAM and NSS Modules
===================

This folder contains all files that implement PAM and NSS modules used to integrate Shibboleth
between the authentication mechanisms for a Linux workstation.

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

  [http://www.gnu.org/software/libc/manual/html_node/Name-Service-Switch.html]

The PAM and NSS module installation has as a requirement the installation of the HTTP Basic authentication handler for SHibboleht. This installation is documented on this wiki at the page:

  [[Configuration to authenticate via HTTP Basic Authentication](/biancini/Shibboleth-Authentication/wiki/Install-Basic)]

After this configuration the PAM and NSS modules can be installed as described on this wiki at the page:

   [[Installation of the PAM and NSS modules into a linux machine](/biancini/Shibboleth-Authentication/wiki/Install-PAM-NSS)]
