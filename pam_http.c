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

#include "netcurl.h"

SESSION *session = NULL;

char *getsessvalue(const char *key)
{
  SESSION *cursor = session;
  while (cursor)
  {
    if (strcasestr(cursor->key, key))
    {
      char *sessvalue = (char *) malloc(strlen(cursor->value)+1);
      strcpy(sessvalue, cursor->value);
      return sessvalue;
    }
    cursor = cursor->next;
  }

  return NULL;
}

void cleansession(SESSION *cursor)
{
  if (cursor->next != NULL) cleansession(cursor->next);
  if (cursor->key != NULL) free(cursor->key);
  if (cursor->value != NULL) free(cursor->value);
  free(cursor);
}

void cleanbody()
{
  if (session == NULL) return;
  cleansession(session);
  session = NULL;
}

size_t bodycallback(char *ptr, size_t size, size_t nmemb, void *userdata)
{
  char* pstr = (char *)malloc(size*nmemb+1);
  strncpy(pstr, ptr, size*nmemb);
  pstr[size*nmemb] = '\0';

  int i = 0;
  char **rows = split_str(pstr, "\n", -1);
  
  if (rows == NULL || rows[0] == NULL) return nmemb*size;
  for (i = 0; rows[i]; i++)
  {
    if (strstr(rows[i], "="))
    {
      char **array = split_str(rows[i], "=", 1);
      if (array == NULL || array[0] == NULL || array[1] == NULL) return nmemb*size;

      #ifdef DEBUG
      fprintf(stderr, "Read session value: [%s] => %s\n", array[0], array[1]);
      #endif

      SESSION *cursess = (SESSION *) malloc(sizeof(SESSION));
      cursess->key = (char *) malloc(strlen(array[0])+1);
      strcpy(cursess->key, array[0]);
      cursess->value = (char *) malloc(strlen(array[1])+1);
      strcpy(cursess->value, array[1]);
      cursess->next = session;
      session = cursess;
      free(array);
    }
  }

  return nmemb*size;
}

/* pam arguments are normally of the form name=value.  This gets the
 * 'value' corresponding to the passed 'name' from the argument list. */
static const char *getarg(const char *name, int argc, const char **argv)
{
  int len = strlen(name);
  while (argc)
  {
    if (strlen(*argv) > len && !strncmp(name, *argv, len) && (*argv)[len] == '=')
    {
      return *argv + len + 1;  /* 1 for the = */
    }
    argc--;
    argv++;
  }
  return 0;
}

const char *get_password(pam_handle_t *pamh, const char *username, int flags, int use_first_pass, int try_first_pass)
{
  int rc = 0;
  char *password = NULL;
  struct pam_message msg;
  const struct pam_message *msgp;
  struct pam_response *respp;
  struct pam_conv *item;
  char *message;
  int i = 0;

  rc = pam_get_item (pamh, PAM_AUTHTOK, (const void **) &password);
  if (rc == PAM_SUCCESS && (use_first_pass || try_first_pass)) return password;

  if ((flags & PAM_SILENT) == PAM_SILENT)
  {
    syslog(LOG_ERR, "PAM_SILENT enabled but password requested, unable to continue.\n");
    return NULL;
  }

  if (pam_get_item(pamh, PAM_CONV, (const void**)&item) != PAM_SUCCESS)
  {
    syslog(LOG_ERR, "Couldn't get pam_conv.\n");
    return NULL;
  }

  msgp = &msg;

  message = (char *) malloc(strlen(username) + 14);
  for (i = 0; i < strlen(username) + 14; i++) message[i] = '\0';
  sprintf(message, "%s's password: ", username);

  msg.msg_style = PAM_PROMPT_ECHO_OFF;
  msg.msg = message;

  item->conv(1, &msgp, &respp, item->appdata_ptr);
  return respp[0].resp;
}

PAM_EXTERN int pam_sm_authenticate(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  const char *username = NULL;
  const char *password = NULL;
  const char *cafile = getarg("cafile", argc, argv);
  const char *sslcheck = getarg("sslcheck", argc, argv);
  const char *sess_username = getarg("sess_username", argc, argv);
  const char *url = getarg("url", argc, argv);
  int use_first_pass = 0, try_first_pass = 0;
  int rv = PAM_AUTH_ERR;
  int i = 0;

  if (!url) return PAM_AUTH_ERR;

  for (i = 0; i < argc; i++)
  {
    if (!strcmp (argv[i], "use_first_pass"))
      use_first_pass = 1;
    else if (!strcmp (argv[i], "try_first_pass"))
      try_first_pass = 1;
  }

  #ifdef DEBUG
  fprintf(stderr, "\nCalling auth function with cafile=%s and sslcheck=%s.\n", cafile, sslcheck);
  #endif
  
  if (pam_get_user(pamh, &username, 0) != PAM_SUCCESS)
  {
    syslog(LOG_ERR, "Couldn't get username.\n");
    return PAM_AUTH_ERR;
  }

  password = get_password(pamh, username, flags, use_first_pass, try_first_pass);
  if (password == NULL)
  {
    syslog(LOG_ERR, "Couldn't get password.\n");
    return PAM_AUTH_ERR;
  }

  if (geturl(url, (char*)username, password, cafile, sslcheck))
  {
    rv = PAM_SUCCESS;

    if (sess_username != NULL)
    {
      char *uname = getsessvalue(sess_username);
      pam_set_item(pamh, PAM_USER, uname);
    }
  }

  memset((void *)password, '\0', strlen(password));
  free((void *)password);

  return rv;
}

PAM_EXTERN int pam_sm_setcred(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  #ifdef DEBUG
  fprintf(stderr, "\nSetting values in user session.\n");
  #endif

  char sess_value[1024];
  SESSION *cursor = session;
  while (cursor)
  {
    sprintf(sess_value, "%s=%s", replace_char(cursor->key, '-', '_'), cursor->value);
    if (pam_putenv(pamh, sess_value) != PAM_SUCCESS)
    {
      #ifdef DEBUG
      fprintf(stderr, "Error setting environment variable in user session.\n");
      #endif

      return PAM_AUTH_ERR;    
    }

    cursor = cursor->next;
  }

  return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_acct_mgmt(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  const char *key = "authenticated";
  const char *value = "true";

  #ifdef DEBUG
  fprintf(stderr, "\nChecking authetication rights in session.\n");
  #endif

  char *authed = getsessvalue(key);
  if (authed != NULL && strcasestr(authed, value)) return PAM_SUCCESS;
  return PAM_AUTH_ERR;
}

PAM_EXTERN int pam_sm_open_session(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  #ifdef DEBUG
  fprintf(stderr, "\nOpening PAM session.\n");
  #endif

  return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_close_session(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  #ifdef DEBUG
  fprintf(stderr, "\nClosing PAM session.\n");
  #endif

  cleanbody();
  return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_chauthtok(pam_handle_t *pamh, int flags, int argc, const char **argv)
{
  const char *message = "Unable to set user credentials on Shibboleth. Please contacy the IdP administrator for this task.\n";
  syslog(LOG_ERR, message);

  if ((flags & PAM_SILENT) != PAM_SILENT)
  {
    struct pam_conv *item;
    struct pam_message msg;
    const struct pam_message *msgp;

    if (pam_get_item(pamh, PAM_CONV, (const void**)&item) != PAM_SUCCESS)
    {
      syslog(LOG_ERR, "Couldn't get pam_conv\n");
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
