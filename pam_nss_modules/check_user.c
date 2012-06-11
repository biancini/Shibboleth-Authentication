/*
  You need to add the following (or equivalent) to the /etc/pam.conf file.
  # check authorization
  check_user   auth       required     /usr/lib/security/pam_http.so
  check_user   account    required     /usr/lib/security/pam_http.so
*/

#include <security/pam_appl.h>
#include <security/pam_misc.h>
#include <stdio.h>
#include <stdlib.h>

#define WITH_OPENSSL
#define WITH_COOKIES
#include "soapH.h"
#include "BackendBinding.nsmap"

static struct pam_conv conv = {
    misc_conv,
    NULL
};

void call_webservice(char *loggeduser, const char *cookie_key, const char *cookie_value)
{
    char *endpoint = "https://server.hostname/webservice.php";
    struct soap *soap = soap_new();
    if (soap_ssl_client_context(soap, SOAP_SSL_NO_AUTHENTICATION, NULL, NULL, NULL, NULL, NULL)) 
    { 
    /  soap_print_fault(soap, stderr); 
     exit(1); 
    } 

    char **salutation = (char **)malloc(sizeof(char *));

    char *new_cookie_key = (char *) malloc(strlen(cookie_key)+14);
    sprintf(new_cookie_key, "_shibsession_%s", cookie_key);

    soap->cookies = soap_set_cookie(soap, new_cookie_key, cookie_value, NULL, NULL);
    soap_set_cookie_expire(soap, new_cookie_key, 10, NULL, NULL);

    #ifdef DEBUG
    fprintf(stderr, "Passing the following cookie to WS:\n");
    fprintf(stderr, "[%s] => %s\n", soap->cookies->name, soap->cookies->value);
    #endif

    if (soap_call_ns2__oncall(soap, endpoint, NULL, loggeduser, salutation) == SOAP_OK) fprintf(stdout, "The salutation from WS is %s\n", *salutation);
    else soap_print_fault(soap, stderr);

    soap_done(soap);
    soap_end(soap);
    soap_free(soap);
}

int main(int argc, char *argv[])
{
    pam_handle_t *pamh = NULL;
    int retval = 0;
    const char *user = "nobody";
    const void *authenticated_user = NULL;
    int call_ws = 0;

    if (argc == 2) user = argv[1];
    if (argc == 3)
    {
      if (strcmp(argv[1], "-ws") == 0) call_ws = 1;
      else if (strcmp(argv[1], "-env") != 0)
      {
        fprintf(stderr, "Usage: check_user [-ws|-env] [username]\n");
        exit(1);
      }

     user = argv[2];
    }

    if (argc > 3) {
        fprintf(stderr, "Usage: check_user [-ws|-env] [username]\n");
        exit(1);
    }

    retval = pam_start("check_user", user, &conv, &pamh);
        
    if (retval == PAM_SUCCESS) retval = pam_authenticate(pamh, 0);
    if (retval == PAM_SUCCESS) retval = pam_acct_mgmt(pamh, 0);
    if (retval == PAM_SUCCESS) retval = pam_setcred(pamh, 0);
    if (retval == PAM_SUCCESS) retval = pam_get_item(pamh, PAM_USER, &authenticated_user);

    /* This is where we have been authorized or not. */

    if (retval == PAM_SUCCESS)
    {
      fprintf(stdout, "Authenticated (user: %s).\n", (char *)authenticated_user);

      const char *cur_var_unique = pam_getenv(pamh, "Shib_Session_Unique");
      const char *cur_var_id = pam_getenv(pamh, "Shib_Session_ID");

      if (call_ws == 1)
      {
        char *cur_var_username = (char *)pam_getenv(pamh, "eduPersonPrincipalName");
        fprintf(stdout, "\nCall webservice with SSO credentials obtained via Shibboleth login:\n");
        call_webservice(cur_var_username, cur_var_unique, cur_var_id);
      }
      else 
      {
        fprintf(stdout, "\nExecute these two directives to have the proper envirnoment variables initialized in your session:\n");
        fprintf(stdout, "export Shib_Session_Unique=%s\n", cur_var_unique);
        fprintf(stdout, "export Shib_Session_ID=%s\n", cur_var_id);
      }
    }
    else fprintf(stdout, "Not Authenticated: %s.\n", pam_strerror(pamh, retval));

    if (pam_end(pamh,retval) != PAM_SUCCESS)
    {
        pamh = NULL;
        fprintf(stderr, "check_user: failed to release authenticator\n");
        exit(1);
    }

    return (retval == PAM_SUCCESS ? 0 : 1);
}

