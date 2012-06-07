import shibauth
import getpass

if __name__ == "__main__":
    username = raw_input('Enter your username: ')
    password = getpass.getpass('Enter your username: ')

    try:
        loggeduser, session = shibauth.login(username, password)
        print "User logged in successfully."
        print "Username for logged user is: %s" % loggeduser

        print "Printing session for logged in user:"
        for key,val in session.items():
            print "Session value: [%s] => %s" % (key, val)
    except Exception, e:
        print "Error logging in user: %s" % e

