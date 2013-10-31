from httpFunctions import http_methods
import ConfigParser
import os
import sys

config = ConfigParser.ConfigParser()
config.readfp(open('shibauth.config'))

url = config.get('HTTP params', 'url')
sslcheck = config.getboolean('HTTP params', 'sslcheck')
sess_username = config.get('HTTP params', 'sess_username')
debug = config.getboolean('HTTP params', 'debug')

def login(username, password=None):
    http_methods.debug = debug
    params = {'url': url,
              'sslcheck': sslcheck,
              'username': username,
              'password': password,
              'sess_username': sess_username};
    http_methods.geturl(params)
    session = http_methods.getsession()

    try:
      if session['authenticated'].lower() != "true": raise Exception("Unable to log in user")
      loggeduser = (sess_username) and session[sess_username] or username
      return loggeduser, session
    except Exception as e:
      if debug: print >> sys.stderr, "Exception: %s" % e
      raise Exception("Unable to log in user")
