from httpFunctions import http_methods
import ConfigParser, os

config = ConfigParser.ConfigParser()
config.readfp(open('shibauth.config'))

url = config.get('HTTP params', 'url')
sslcheck = config.getboolean('HTTP params', 'sslcheck')
sess_username = config.get('HTTP params', 'sess_username')
debug = config.getboolean('HTTP params', 'debug')

def login(username, password=None):
    http_methods.debug = debug
    returned_page = http_methods.get_url(url, username, password, False)

    if (returned_page.get_return_code() == 200):
        session = {}
        for cur_row in returned_page.get_body_rows():
            vals = cur_row.split("=")
            if (len(vals) > 0 and vals[0]):
                session[vals[0]] = (len(vals) > 1) and vals[1] or None
        if not session["authenticated"].lower() == "true": raise Exception("Unable to log in user")
        loggeduser = (sess_username) and session[sess_username] or username
        return loggeduser, session
    else:
        raise Exception("Unable to log in user")

