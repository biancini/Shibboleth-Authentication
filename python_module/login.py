import shibauth
import getpass
import logging
import sys
import os
from ZSI.client import Binding

def call_webservice(cookies):
    global loggeduser, session
    print "Trying to call webservice using SSO with obtained credentials."
    url = 'https://cloud-mi-03.mib.infn.it/secure/webservice.php'

    client = Binding(url=url)
    for (key, value) in cookies.items():
        client.cookies[key] = value

    print client.oncall(loggeduser)['return']


def login():
    username = raw_input('Enter your username: ')
    password = getpass.getpass('Enter your password: ')

    return shibauth.login(username, password)

def sso():
    sess_username = "eduPersonPrincipalName".replace("-", "_")
    shib_unique = "Shib-Session-Unique".replace("-", "_")
    shib_id = "Shib-Session-ID".replace("-", "_")

    if sess_username not in os.environ or shib_unique not in os.environ or shib_id not in os.environ:
        print "Error retrieving user session from environment."
        return None, None

    loggeduser = os.environ[sess_username]
    
    session = {}
    session["Shib_Session_Unique"] = os.environ[shib_unique]
    session["Shib_Session_ID"] = os.environ[shib_id]

    return loggeduser, session

if __name__ == "__main__":

    perform_sso = False
    for arg in sys.argv:
        if arg == "-sso": perform_sso = True

    if perform_sso:
        loggeduser, session = sso()
        if (loggeduser is None or session is None): loggeduser, session = login()
    else: loggeduser, session = login()

    try:
        print "User logged in successfully."
        print "Username for logged user is: %s" % loggeduser

        print "Printing session for logged in user:"
        for key,val in session.items():
            print "Session value: [%s] => %s" % (key, val)

        cookies = {}
        cookies["_shibsession_%s" % session["Shib_Session_Unique"]] = session["Shib_Session_ID"]
        call_webservice(cookies)
    except Exception, e:
        print "Error logging in user: %s" % e

