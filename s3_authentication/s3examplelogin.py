#!/bin/env python
from httpFunctions import http_methods
import re
import cookielib
import socket
import base64
import hmac
import sha

class CumulusService():
    ssl_check = False
    cookies = cookielib.CookieJar()

    def canonical_string(self, method, path, headers):
        return "GET\n\n\nFri, 29 Jun 2012 15:57:41 GMT\n/ciao_maamma/"

    def authorize(self, headers, method, uri):
        host_name = socket.gethostbyaddr(socket.gethostname())
        url = "https://%s/Shibboleth.sso/AmazonS3" % host_name[0]
        
        insensitive_aws = re.compile(re.escape('aws '), re.IGNORECASE)
        access_key = insensitive_aws.sub('', headers['Authorization']).split(":")[0]
        string_to_sing = self.canonical_string(method, uri, headers)
        secreted_string = insensitive_aws.sub('', headers['Authorization']).split(":")[1]

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

    accessKey = base64.b64encode("http://idp-test1.mib.infn.it/shibboleth!fabio")
    print accessKey
    #accessKey = "TURTEQIGAJRPSEQTMOMFD"
    #secretKey = "reXtIFhdk+JQjwDE1mf+oEmq1R4="
    secretKey = "8PPJM30JNEqnYyWFOobtJOWLuXtRaK+ZiIu2mOWz6splWxepnBDgQhi0bOLAJJgT"

    headers = {}

    myhmac = hmac.new(secretKey, digestmod=sha)
    c_string = cumulus_service.canonical_string("GET", "/garr", headers)
    myhmac.update(c_string)
    encryptedString = base64.encodestring(myhmac.digest()).strip()

    headers['Authorization'] = "AWS %s:%s" % (accessKey, encryptedString)

    try:   
        ret = cumulus_service.authorize(headers, "GET", "/garr")
        print "Il metodo di autenticatione ha restituito %s" % ret
        #utente loggato correttamente
        print "L'utente e' stato loggato correttamente."
    except:
        # Utente non loggato"
        print "L'utente NON e' stato loggato, errore nelle chiavi."

