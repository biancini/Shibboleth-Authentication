rows = []

def getrow():
	global rows

	if (len(rows) > 0 and len(rows[0]) > 0):
		row = rows.pop(0)
		return row
	else:
		return ""

def geturl(url, username=None, password=None):
	import mechanize
	import cookielib
	import urllib

	global rows

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
	r = br.open(url)
	html_content = r.read()

	#print "-> %s" % html_content
	rows = html_content.split('\n')
	return 0;
