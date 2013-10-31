import sys
import urllib2
import urllib
import cookielib
import base64

class http_methods():
    debug = False
    ssl_check = False
    cookies = cookielib.CookieJar()

    session = {}
    @staticmethod
    def is_shibbolethds(html_content):
        return "wayf.css" in html_content

    @staticmethod
    def send_msg(msg):
        return raw_input(msg)

    @staticmethod
    def shibbolethds(br, params):
        br.select_form(nr=0)

        num = 1
        msg = "Chose the IdP you want to use for login. Available IdPs are:\n"
        listidps = {}
        for c in br.form.find_control('origin').get_items():
            msg = "%s%d. %s\n" % (msg, num, ' - '.join([a.text for a in c.get_labels()]))
            listidps[num] = c.name
            num = num+1
        msg ="%sInsert the number of the IdP to be used: " % msg

        rsp = send_msg(msg)
        br.form['origin'] = [listidps[int(rsp.resp)]]
        r = br.submit()
        html_content = r.read()
        return html_content

    @staticmethod
    def is_shibboleth(html_content):
        return ('j_username' in html_content and 'j_password' in html_content)

    @staticmethod
    def shibboleth(br, params):
        # Submit first form with username and password provided
        br.select_form(nr=0)
        br.form['j_username'] = params['username']
        br.form['j_password'] = params['password']
        r = br.submit()

        html_content = r.read()
        if (http_methods.debug): print >> sys.stderr, html_content

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

    @staticmethod
    def is_simplesaml(html_content):
        return ('id="username"' in html_content and 'id="password"' in html_content)

    @staticmethod
    def simplesaml(br, params):
        # Submit first form with username and password provided
        br.select_form(nr=0)
        br.form['username'] = params['username']
        br.form['password'] = params['password']
        r = br.submit()

        html_content = r.read()

        # Submit form to create user session
        br.select_form(nr=0)
        r = br.submit()
        html_content = r.read()

        if (http_methods.debug): print >> sys.stderr, html_content
        return html_content

    @staticmethod
    def geturl(params):
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
        if (http_methods.debug): print >> sys.stderr, "Trying to access url: %s" % params['url']
        r = br.open(params['url'])
        html_content = r.read()

        continue_loop = True
        while continue_loop:
            if http_methods.is_shibbolethds(html_content):
                if (http_methods.debug): print >> sys.stderr, "It is a Shibboleth DS page"
                html_content = http_methods.shibbolethds(br, params)
            elif http_methods.is_shibboleth(html_content):
                if (http_methods.debug): print >> sys.stderr, "It is a Shibboleth IdP page"
                html_content = http_methods.shibboleth(br, params)
                continue_loop = False
            elif http_methods.is_simplesaml(html_content):
                if (http_methods.debug): print >> sys.stderr, "It is a Simple SAML IdP page"
                html_content = http_methods.simplesaml(br, params)
                continue_loop = False
            else:
                html_content = ""
                continue_loop = False

        if (http_methods.debug): print >> sys.stderr, html_content
        session = {}

        rows = html_content.split('\n')
        for row in rows:
            if '=' in row:
                vals = row.split('=')
                session[vals[0].replace('-', '_')] = vals[1]

    @staticmethod
    def getsession():
        return session
