import shibauth
import getpass
import urllib2
import urlparse
import suds
import logging
import cookielib

def call_webservice(loggeduser, cookies):
    print "Trying to call webservice using SSO with obtained credentials."
    url = 'https://server.honstname/webservice.php?wsdl'

    logging.basicConfig(level=logging.INFO)
    #logging.getLogger('cookielib').setLevel(logging.DEBUG)
    #cookielib.debug = True
    #logging.getLogger('suds.client').setLevel(logging.DEBUG)

    cookiejar = cookielib.CookieJar()
    for (key, value) in cookies.items():
        cookie = cookielib.Cookie(version=0, name=key, value=value, port=None, port_specified=False,
                                  domain=urlparse.urlparse(url)[1], domain_specified=True, domain_initial_dot=False,
                                  path='/', path_specified=True, secure=False, expires=None, discard=True,
                                  comment=None, comment_url=None, rest=None, rfc2109=True)
        cookiejar.set_cookie(cookie)
    
    transport = suds.transport.https.HttpTransport()
    opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(cookiejar))
    transport.urlopener = opener
    client = suds.client.Client(url, transport=transport, cache=None)

    opener = urllib2.build_opener(urllib2.HTTPCookieProcessor())
    client.options.transport.cookiejar = cookiejar

    print client.service.BackendPort.oncall(loggeduser)

if __name__ == "__main__":
    username = raw_input('Enter your username: ')
    password = getpass.getpass('Enter your password: ')

    try:
        loggeduser, session = shibauth.login(username, password)
        print "User logged in successfully."
        print "Username for logged user is: %s" % loggeduser

        print "Printing session for logged in user:"
        for key,val in session.items():
            print "Session value: [%s] => %s" % (key, val)

        cookies = {}
        cookies["_shibsession_%s" % session["Shib-Session-Unique"]] = session["Shib-Session-ID"]
        call_webservice(loggeduser, cookies)
    except Exception, e:
        print "Error logging in user: %s" % e

