#!/bin/env python
from httpFunctions import http_methods
import cookielib

class CumulusService():
    ssl_check = False
    cookies = cookielib.CookieJar()

    def authorize(self, headers, method, uri):
        url = "https://cloud-mi-03.mib.infn.it/Shibboleth.sso/AmazonS3"
        
        access_key = headers['authorization'].lower().replace("aws ", "").split(":")[0]
        string_to_sing = "stringtobesigned"
        secreted_string = headers['authorization'].lower().replace("aws ", "").split(":")[1]

        returned_page = http_methods.get_url(url, access_key, string_to_sing, secreted_string, False)
        
        if (returned_page.get_return_code() == 200):
            session = {}
            for cur_row in returned_page.get_body_rows():
                #print cur_row
                vals = cur_row.split("=")
                if (len(vals) > 0 and vals[0]):
                    session[vals[0]] = (len(vals) > 1) and vals[1] or None
                    #print 'Added session variable: [%s] => %s' % (vals[0], (len(vals) > 1) and vals[1] or None)
            if session["authenticated"].lower() == "true":
                return "OK"
     
        # If code arrives here, the user has not been logged in
        ec = 'AccessDenied'
        ex = Exception(ec)
        raise ex

if __name__ == "__main__":
    cumulus_service = CumulusService()

    headers = {}
    headers['authorization'] = "AWS accesskey:encriptedstring";

    try:   
        ret = cumulus_service.authorize(headers, "GET", "/garr")
        print "Il metodo di autenticatione ha restituito %s" % ret
        #utente loggato correttamente
        print "L'utente e' stato loggato correttamente."
    except:
        # Utente non loggato"
        print "L'utente NON e' stato loggato, errore nelle chiavi."

