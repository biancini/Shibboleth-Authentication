session = {}

def get_password(pamh, params):
  password = None
  if "use_first_pass" in params and params["use_first_pass"]:
    password = pamh.authtok
    return password

  if "try_first_pass" in params and params["try_first_pass"]:
    password = pamh.authtok
  
  if password is None:
    username = pamh.get_user(None)
    msg = pamh.user_prompt
    if msg is None: msg = "%s's password:" % username
    res = send_msg(pamh, pamh.PAM_PROMPT_ECHO_OFF, msg)
    password = res.resp

  return password

def is_shibbolethds(html_content):
  return "wayf.css" in html_content

def shibbolethds(pamh, br, params):
  br.select_form(nr=0)

  num = 1
  msg = "Chose the IdP you want to use for login. Available IdPs are:\n"
  listidps = {}
  for c in br.form.find_control('origin').get_items():
    msg = "%s%d. %s\n" % (msg, num, ' - '.join([a.text for a in c.get_labels()]))
    listidps[num] = c.name
    num = num+1
  msg ="%sInsert the number of the IdP to be used: " % msg

  rsp = send_msg(pamh, pamh.PAM_PROMPT_ECHO_ON, msg)
  br.form['origin'] = [listidps[int(rsp.resp)]]
  r = br.submit()
  html_content = r.read()
  return html_content

def is_shibboleth(html_content):
  return ('j_username' in html_content and 'j_password' in html_content)

def shibboleth(pamh, br, params):
  username = pamh.get_user(None)
  password = get_password(pamh, params)

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
  return html_content

def is_simplesaml(html_content):
  return ('id="username"' in html_content and 'id="password"' in html_content)

def simplesaml(pamh, br, params):
  username = pamh.get_user(None)
  password = get_password(pamh, params)

  # Submit first form with username and password provided
  br.select_form(nr=0)
  br.form['username'] = username
  br.form['password'] = password
  r = br.submit()

  html_content = r.read()

  # Submit form to create user session
  br.select_form(nr=0)
  r = br.submit()
  html_content = r.read()

  #print html_content
  return html_content

def geturl(pamh, params):
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
  r = br.open(params['url'])
  html_content = r.read()

  continue_loop = True
  while continue_loop:
    if is_shibbolethds(html_content):
      #print "It is a Shibboleth DS page"
      html_content = shibbolethds(pamh, br, params)
    elif is_shibboleth(html_content):
      #print "It is a Shibboleth IdP page"
      html_content = shibboleth(pamh, br, params)
      continue_loop = False
    elif is_simplesaml(html_content):
      #print "It is a Simple SAML IdP page"
      html_content = simplesaml(pamh, br, params)
      continue_loop = False
    else:
      html_content = ""
      continue_loop = False

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
    geturl(pamh, params)
  except pamh.exception, e:
    return e.pam_result

  if params['sess_username'] in session: pam.user = session[param['sess_username']]
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
    params = {'url': 'https://sp-servername/secure/pam.php',
              'sslcheck': False,
              'sess_username': 'uid'};
    geturl(None, params, 'username', 'password')
    print session
