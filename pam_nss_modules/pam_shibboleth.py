session = {}

def geturl(params, username, password):
  import mechanize
  import cookielib
  import urllib
  global session

  br = mechanize.Browser()
  cj = cookielib.LWPCookieJar()
  br.set_cookiejar(cj)
  br.set_handle_equiv(True)
  br.set_handle_gzip(False)
  br.set_handle_redirect(True)
  br.set_handle_referer(True)
  br.set_handle_robots(False)
  br.set_handle_refresh(mechanize._http.HTTPRefreshProcessor(), max_time=1)
  br.addheaders = [('User-agent', 'Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.1) Gecko/2008071615 Fedora/3.0.1-1.fc9 Firefox/3.0.1')]

  # Get first url and get redirected to IdP login page
  br.open(params['url'])

  # Submit first form with username and password provided
  br.select_form(nr=0)
  br.form['j_username'] = username
  br.form['j_password'] = password
  r = br.submit()

  html_content = r.read()
  #print html_content

  # If uApprove ToU, approve it
  if '<title>Terms Of Use</title>' in html_content:
    br.select_form(nr=0)
    #br.form['accept'].selected = True
    for i in range(0, len(br.find_control(type="checkbox").items)):
      br.find_control(type="checkbox").items[i].selected =True
    r = br.submit()
    html_content = r.read()

  # If uApprove attribute release, approve it
  if '<title>Attribute Release</title>' in html_content:
    br.select_form(nr=0)
    r = br.submit()

  # Submit form to create user session
  br.select_form(nr=0)
  r = br.submit()

  html_content = r.read()
  #print html_content
  session = {}

  rows = html_content.split('\n')
  for row in rows:
    if '=' in row:
      vals = row.split('=')
      session[vals[0].replace('-', '_')] = vals[1]

def parse_args(pamh, argv):
  use_first_pass = False
  try_first_pass = False

  params = {}
  params['url'] = "https://servername/secure/pam.php"
  params['sslcheck'] = False
  params['sess_username'] = "uid"
  params['cafile'] = ""

  for arg in argv[1:]:
    if arg.startswith("use_first_pass"):
      use_first_pass = True
    elif arg.startswith("try_first_pass"):
      try_first_pass = True
    else:
      for parname in params:
        if arg.startswith(parname + "="):
          params[parname] = arg[len(parname)+1:]

  params['use_first_pass'] = use_first_pass
  params['try_first_pass'] = try_first_pass
  return params

def send_error_msg(pamh, msg):
    return send_msg(pamh, pamh.PAM_ERROR_MSG, msg)

def send_info_msg(pamh, msg):
    return send_msg(pamh, pamh.PAM_TEXT_INFO, msg)

def send_msg(pamh, msg_style, msg):
    pammsg = pamh.Message(msg_style, msg)
    rsp = pamh.conversation(pammsg)
    return rsp

def pam_sm_authenticate(pamh, flags, argv):
  global session
  try:
    session = {}
    params = parse_args(pamh, argv)
    username = pamh.get_user(None)
    password = send_msg(pamh, pamh.PAM_PROMPT_ECHO_OFF, "%s's password:" % username)
    geturl(params, username, password.resp)
  except pamh.exception, e:
    return e.pam_result

  if params['sess_username'] in session: pamh.user = session[params['sess_username']]
  if 'authenticated' in session and session['authenticated'] == 'true':
    return pamh.PAM_SUCCESS
  else:
    return pamh.PAM_AUTH_ERR

def pam_sm_setcred(pamh, flags, argv):
  global session
  for value in session:
    pamh.env[value] = session[value]
  return pamh.PAM_SUCCESS

def pam_sm_acct_mgmt(pamh, flags, argv):
  return pamh.PAM_SUCCESS

def pam_sm_open_session(pamh, flags, argv):
  return pamh.PAM_SUCCESS

def pam_sm_close_session(pamh, flags, argv):
  return pamh.PAM_SUCCESS

def pam_sm_chauthtok(pamh, flags, argv):
  return pamh.PAM_SYSTEM_ERR

if __name__ == "__main__":
    params = {'url': 'https://servername/secure/pam.php',
              'sslcheck': False,
              'sess_username': 'uid'};
    geturl(params, 'user', 'password')
    print "Session values: %s" % session
