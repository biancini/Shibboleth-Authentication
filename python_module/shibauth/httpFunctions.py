import sys
import urllib2, urllib
import cookielib
import base64

class http_methods():
    debug = False
    ssl_check = False
    cookies = cookielib.CookieJar()

    @staticmethod    
    def get_url(url_to_read, username, password, ssl_check):
        returned_page = None
        http_methods.ssl_check = ssl_check
        
        return_code = -1
        cur_url = url_to_read
        while (return_code != 200):
            try:
                returned_page = http_methods.get_single_url(cur_url)
                cur_url = returned_page.get_header_field("location")
                return_code = returned_page.get_return_code()
            except urllib2.HTTPError, e:
                if (e.code == 401):
                    returned_page = http_methods.get_single_url(cur_url, username, password)
                    cur_url = returned_page.get_header_field("location")
                    return_code = returned_page.get_return_code()
                else: return_code = -1

        return returned_page

    @staticmethod
    def get_single_url(url_to_read, username=None, password=None):
        cookies_string = ';'.join([("%s=%s" % (cookie.name, cookie.value)) for cookie in http_methods.cookies])

        if (http_methods.debug): print >> sys.stderr, 'Opening URL: ' + url_to_read
        if (http_methods.debug): print >> sys.stderr, 'Passing the following cookies: ' + cookies_string

        if username:
            if (http_methods.debug): print >> sys.stderr, 'Basic authentication string: ' + username + ':' + password
            password_mgr = urllib2.HTTPPasswordMgrWithDefaultRealm()
            password_mgr.add_password(None, url_to_read, username, password)
            opener = urllib2.build_opener(no_redirect_handler(), urllib2.HTTPCookieProcessor(http_methods.cookies), urllib2.HTTPBasicAuthHandler(password_mgr))
        else:
  	    opener = urllib2.build_opener(no_redirect_handler(), urllib2.HTTPCookieProcessor(http_methods.cookies))

        data = urllib.urlencode({})
        req = urllib2.Request(url_to_read)
        resp = opener.open(req)

        returned_page = http_page()
        returned_page.set_return_code(resp.code)

        for (header_key, header_value) in resp.info().items():
            returned_page.add_header_field(header_key, header_value)

	if (http_methods.debug): print >> sys.stderr, 'Response code: ' + str(returned_page.get_return_code())

        if (returned_page.get_return_code() == 200):
           for line in resp.read().split('\n'):
               returned_page.add_body_row(line)
		
	return returned_page;

class no_redirect_handler(urllib2.HTTPRedirectHandler):
    def http_error_302(self, req, fp, code, msg, headers):
        infourl = urllib.addinfourl(fp, headers, req.get_full_url())
        infourl.status = code
        infourl.code = code
        return infourl

    http_error_300 = http_error_302
    http_error_301 = http_error_302
    http_error_303 = http_error_302
    http_error_307 = http_error_302

class http_page():
    return_code = -1
    header_fields = {}
    body_rows = []

    def get_return_code(self):
        return self.return_code

    def set_return_code(self, return_code):
        self.return_code = return_code

    def get_header_field(self, field_name):
        if (field_name in self.header_fields.keys()):
            return self.header_fields[field_name]

        return None

    def add_header_field(self, field_name, field_values):
        self.header_fields[field_name] = field_values

    def add_body_row(self, body_row):
        self.body_rows.append(body_row)

    def get_body_rows(self):
        return self.body_rows

