Python Module
=============

This folder contains all files that implement the Python module used to login with
Shibboleth regular Python applications.

This folder also contains an example of a client that authenticates with Shibboleth
and then uses the session cookie to call a webservice behind Shibboleth authentication.

A set of very simple APIs have been created to permit high level programming languages (used in real
applications) to leverage the authentication mechanisms implemented in this project.

For information about installing and using such APIs, refer to the following page of this wiki:

  [[Usage of JAAS and Python modules](/biancini/Shibboleth-Authentication/wiki/Use-JAAS-Python)]

### Python

The Python module permits to authenticate inside python applications.
It uses the urllib2 library to connect with the webserver and reads user session
information from the web pages served after authentication (in a very similar way to the PAM
module written in C).

To obtain more information on urllib2, visit:

  [http://docs.python.org/library/urllib2.html]
