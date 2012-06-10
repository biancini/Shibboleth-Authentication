import shibauth
import getpass
import logging
from ZSI.client import Binding

def call_webservice(loggeduser, cookies):
    print "Trying to call webservice using SSO with obtained credentials."
    url = 'https://server.hostname/webservice.php'

    client = Binding(url=url)
    for (key, value) in cookies.items():
        client.cookies[key] = value

    print client.oncall(loggeduser)['return']

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

