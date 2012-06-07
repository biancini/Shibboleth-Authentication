import shibauth
import getpass

if __name__ == "__main__":
    username = raw_input('Enter your username: ')
    password = getpass.getpass('Enter your username: ')

    try:
        session = shibauth.login(username, password)
        for key,val in session.items():
            print "Session value: [%s] => %s" % (key, val)
    except Exception, e:
        print "Error validating user: %s" % e

