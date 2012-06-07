from httpFunctions import http_methods

url = "https://cloud-mi-03.mib.infn.it/secure/pam.php"

def login(username, password=None):
    http_methods.debug = False
    returned_page = http_methods.get_url(url, username, password, False)
    if (returned_page.get_return_code() == 200):
        session = {}
        for cur_row in returned_page.get_body_rows():
            vals = cur_row.split("=")
            if (len(vals) > 0 and vals[0]):
                session[vals[0]] = (len(vals) > 1) and vals[1] or None
        return session
    else:
        raise Exception("Unable to log in user")

