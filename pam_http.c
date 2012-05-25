#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>

#define PAM_SM_AUTH
#define PAM_SM_PASSWORD
#define PAM_SM_ACCOUNT
#define PAM_SM_SESSION
#include <security/pam_modules.h>

#include <curl/curl.h>
#include <curl/types.h>
#include <curl/easy.h>

char *cookies = NULL;

static size_t nop_wf(void* a, size_t x, size_t y, void* b) { return x * y; }

size_t headercallback(void *ptr, size_t size, size_t nmemb, void *userdata)
{
  int i = 0;
  int j = 0;
  int write = -1;
  if (ptr == NULL) return -1;

  char* pstr = (char *)malloc(size*nmemb+1);
  strncpy(pstr, ptr, size*nmemb);
  pstr[size*nmemb] = '\0';

  char headcookie[] = "Set-Cookie";
  if (strcasestr(pstr, headcookie))
  {
    char *newstr = (char *)malloc(size*nmemb+1);
    for (i = 0; pstr[i]; i++)
    {
      if (write == -1 && i > 0 && pstr[i-1] == ' ') write = 0;
      if (write == 0 && pstr[i] == ';') write = 1;
      if (write == 0) newstr[j++] = pstr[i];
    }
    newstr[j] = '\0';

    if (cookies == NULL || strstr(cookies, newstr) == NULL)
    {
      char *oldcookies = cookies;
      cookies = (char *)malloc(((oldcookies != NULL) ? strlen(oldcookies) : 0)+strlen(newstr)+2);
      for (i = 0; i < (oldcookies && strlen(oldcookies))+(strlen(newstr))+2; i++) { cookies[i] = '\0'; }
      if (oldcookies == NULL || strlen(oldcookies) == 0) sprintf(cookies, "%s", newstr);
      else sprintf(cookies, "%s;%s", oldcookies, newstr);
      if (strlen(cookies) == 0) cookies = NULL;
      if (oldcookies != NULL && strlen(oldcookies) > 0) free(oldcookies);
    }
  }

  return nmemb * size;  
}

static int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck)
{
  CURLcode hResult = -1;
  curl_global_init(CURL_GLOBAL_ALL);
  CURL *hCurl = curl_easy_init();
  char *userpass = NULL;
  char *new_url = NULL;
  long http_code = 0;

  if (!hCurl) return 0;

  userpass = malloc(strlen(username)+strlen(password)+2);
  sprintf(userpass, "%s:%s", username, password);
  #ifdef DEBUG
  fprintf(stderr, "Basich authentication string: %s\n", userpass);
  #endif

  if (!userpass) goto cleanup;
  curl_easy_setopt(hCurl, CURLOPT_URL, url);
  curl_easy_setopt(hCurl, CURLOPT_WRITEFUNCTION, nop_wf);
  curl_easy_setopt(hCurl, CURLOPT_NOPROGRESS, 1);
  curl_easy_setopt(hCurl, CURLOPT_FAILONERROR, 1);

  curl_easy_setopt(hCurl, CURLOPT_FOLLOWLOCATION, 0);
  curl_easy_setopt(hCurl, CURLOPT_HEADERFUNCTION, headercallback); 

  // seed SSL randomness from somewhere; this is really problematic
  // because libcurl wants to read 16 kilobytes of randomness.  (Why
  // does it think it needs 131072 bits?  Does it think someone might
  // spend 10^39334 universe-lifetimes to brute-force our SSL
  // connection?)
  curl_easy_setopt(hCurl, CURLOPT_RANDOM_FILE, "/dev/urandom");

  int ssl = strncmp(sslcheck, "true", 10);
  if (ssl == 0)
  {
    curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYPEER, 1);
    curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYHOST, 2);
    curl_easy_setopt(hCurl, CURLOPT_CAINFO, cafile);
  }
  else
  {
    curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYPEER, 0);
    curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYHOST, 0);
  }
 
  new_url = (char *)malloc(strlen(url)+1);
  strcpy(new_url, url);
  new_url[strlen(url)] = '\n';

  do
  {
    char *passcookies = NULL;
    if (cookies != NULL)
    {
      passcookies = (char *)malloc(strlen(cookies) + 1);
      strcpy(passcookies, cookies);
      passcookies[strlen(cookies)] = '\0';
      free(cookies);
      cookies = NULL;
    }

    curl_easy_setopt(hCurl, CURLOPT_COOKIE, passcookies);
    char *nurl = (char *)malloc(strlen(new_url) + 1);
    strcpy(nurl, new_url);
    nurl[strlen(new_url)] = '\0';

    #ifdef DEBUG
    fprintf(stderr, "Redirecting to ULR: %s\n", new_url);
    fprintf(stderr, "Passing the following cookies: %s\n", passcookies);
    #endif

    char *old_url = (char *)malloc(strlen(new_url) + 1);
    strcpy(old_url, new_url);
    old_url[strlen(new_url)] = '\0';

    curl_easy_setopt(hCurl, CURLOPT_URL, nurl);
    hResult = curl_easy_perform(hCurl);

    if (passcookies != NULL && strlen(passcookies) > 0) free(passcookies);
    
    if (curl_easy_getinfo(hCurl, CURLINFO_RESPONSE_CODE, &http_code) == CURLE_OK)
    {
      #ifdef DEBUG
      fprintf(stderr, "Response code: %1u\n", http_code);
      #endif

      if (http_code == 401) {
        #ifdef DEBUG
        fprintf(stderr, "Adding Basic authentication directives.\n");
        #endif

        curl_easy_setopt(hCurl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
        curl_easy_setopt(hCurl, CURLOPT_USERPWD, userpass);

        curl_easy_setopt(hCurl, CURLOPT_URL, old_url);
        hResult = curl_easy_perform(hCurl);
      }
      else
      {
        free(old_url);
      }
    }
  }
  while (curl_easy_getinfo(hCurl, CURLINFO_REDIRECT_URL, &new_url) == CURLE_OK && new_url != NULL);

  #ifdef DEBUG
  char *eff_url = NULL;
 
  fprintf(stderr, "Curl call result: %d\n", hResult);
  if (curl_easy_getinfo(hCurl, CURLINFO_EFFECTIVE_URL, &eff_url) == CURLE_OK)
    fprintf(stderr, "Effective URL: %s\n", eff_url);
  //free(eff_url);
  #endif

 cleanup:
  curl_easy_cleanup(hCurl);
  curl_global_cleanup();

  if (new_url != NULL) free(new_url);
  memset(userpass, '\0', strlen(userpass));
  free(userpass);

  return hResult == 0;
}

/* pam arguments are normally of the form name=value.  This gets the
 * 'value' corresponding to the passed 'name' from the argument list. */
static const char *getarg(const char *name, int argc, const char **argv) {
  int len = strlen(name);
  while (argc) {
    if (strlen(*argv) > len && 
	!strncmp(name, *argv, len) && 
	(*argv)[len] == '=') {
      return *argv + len + 1;  /* 1 for the = */
    }
    argc--;
    argv++;
  }
  return 0;
}

PAM_EXTERN int pam_sm_authenticate(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  struct pam_conv *item;
  struct pam_message msg;
  const struct pam_message *msgp;
  struct pam_response *respp;
  const char *username;
  const char *cafile = getarg("cafile", argc, argv);  /* possibly NULL */
  const char *sslcheck = getarg("sslcheck", argc, argv);  /* possible NULL */
  const char *url = getarg("url", argc, argv);
  int rv = PAM_SUCCESS;
  char *lsslcheck;
  char *message;
  int i = 0;

  if (!url) return PAM_AUTH_ERR;

  lsslcheck = (char *) malloc(strlen(sslcheck));
  for (i = 0; sslcheck[i]; i++)
    lsslcheck[i] = tolower(sslcheck[i]);

  #ifdef DEBUG
  fprintf(stderr, "Calling auth function with cafile=%s and sslcheck=%s.\n", cafile, lsslcheck);
  #endif
  
  msgp = &msg;

  if ((flags & PAM_SILENT) == PAM_SILENT)
  {
    syslog(LOG_AUTHPRIV, "PAM_SILENT enabled but password requested, unable to continue.\n");
    return PAM_AUTH_ERR;
  }

  if (pam_get_item(pamh, PAM_CONV, (const void**)&item) != PAM_SUCCESS)
  {
    syslog(LOG_AUTHPRIV, "Couldn't get pam_conv.\n");
    return PAM_AUTH_ERR;
  }

  if (pam_get_user(pamh, &username, 0) != PAM_SUCCESS)
  {
    syslog(LOG_AUTHPRIV, "Couldn't get username.\n");
    return PAM_AUTH_ERR;
  }

  message = (char *) malloc(strlen(username) + 14);
  for (i = 0; i < strlen(username) + 14; i++)
    message[i] = '\0';
  sprintf(message, "%s's password: ", username);

  msg.msg_style = PAM_PROMPT_ECHO_OFF;
  msg.msg = message;

  item->conv(1, &msgp, &respp, item->appdata_ptr);

  if (!geturl(url, (char*)username, respp[0].resp, cafile, lsslcheck))
      rv = PAM_AUTH_ERR;

  memset(respp[0].resp, '\0', strlen(respp[0].resp));
  free(respp);

  return rv;
}

PAM_EXTERN int pam_sm_setcred(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  const char *message = "Unable to set user credentials on Shibboleth. Please contacy the IdP administrator for this task.\n";
  syslog(LOG_AUTHPRIV, message);

  if ((flags & PAM_SILENT) != PAM_SILENT)
  {
    struct pam_conv *item;
    struct pam_message msg;
    const struct pam_message *msgp;


    if (pam_get_item(pamh, PAM_CONV, (const void**)&item) != PAM_SUCCESS)
    {
      syslog(LOG_AUTHPRIV, "Couldn't get pam_conv\n");
      return PAM_AUTH_ERR;
    }

    msgp = &msg;
    msg.msg_style = PAM_ERROR_MSG;
    msg.msg = message;
    item->conv(1, &msgp, NULL, item->appdata_ptr);
  }

  return PAM_CRED_ERR;
}

PAM_EXTERN int pam_sm_acct_mgmt(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  // This function performs the task of establishing whether the user is permitted to gain access at this time.
  // It should be understood that the user has previously been validated by an authentication module.
  // This function checks for other things. Such things might be: the time of day or the date, the terminal line, remote hostname, etc. .
  return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_open_session(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_close_session(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_chauthtok(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  const char *message = "Unable to set user credentials on Shibboleth. Please contacy the IdP administrator for this task.\n";
  syslog(LOG_AUTHPRIV, message);

  if ((flags & PAM_SILENT) != PAM_SILENT)
  {
    struct pam_conv *item;
    struct pam_message msg;
    const struct pam_message *msgp;

    if (pam_get_item(pamh, PAM_CONV, (const void**)&item) != PAM_SUCCESS)
    {
      syslog(LOG_AUTHPRIV, "Couldn't get pam_conv\n");
      return PAM_AUTH_ERR;
    }

    msgp = &msg;
    msg.msg_style = PAM_ERROR_MSG;
    msg.msg = message;
    item->conv(1, &msgp, NULL, item->appdata_ptr);
  }

  return PAM_PERM_DENIED;
}


#ifdef PAM_STATIC

struct pam_module _pam_http_modstruct = {
  "pam_http",
  pam_sm_authenticate,
  pam_sm_setcred,
  pam_sm_acct_mgmt,
  pam_sm_open_session,
  pam_sm_close_session,
  pam_sm_chauthtok
};

#endif
