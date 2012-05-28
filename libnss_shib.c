#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>
#include <libconfig.h>
#include <sys/types.h>
#include <sys/param.h>

#include <curl/curl.h>
#include <curl/types.h>
#include <curl/easy.h>

#include <pwd.h>
#include <grp.h>
#include <nss.h>
#include "netcurl.h"

BODY *body = NULL;
int last_rownum_pwd = -1;
int last_rownum_grp = -1;

static const char *config_file = "/etc/libnss.conf";

const char *url = NULL;
const char *cafile = NULL;
const char *sslcheck = NULL;
const char *username = NULL;
const char *password = NULL;

#ifdef DEBUG
void print_body(BODY *pointer)
{
  if (pointer == NULL) return;

  fprintf(stderr, "-> Printing: ");
  BODY *cursor = pointer;
  while(cursor)
  {
    fprintf(stderr, "%s, ", cursor->row);
    cursor = cursor->next;
  }
  fprintf(stderr, "\n");
}
#endif

void free_body(BODY *cursor)
{
  if (cursor->next != NULL) free_body(cursor->next);
  free(cursor);
}

void cleanbody(void)
{
  if (body != NULL) free_body(body);
  body = NULL;
}

static void readconfig()
{
  config_t cfg;
  const char *val = NULL;

  config_init(&cfg);
  if (!config_read_file(&cfg, config_file))  
  {
    char *message = "Config file not found in /etc/libnss.conf.\n";
    syslog(LOG_ERR, message);

    #ifdef DEBUG
    fprintf(stderr, message);
    fprintf(stderr, "%s\n", config_error_text(&cfg));
    #endif

    config_destroy(&cfg);  
    return;
  }

  if (url == NULL)
  {
    config_lookup_string(&cfg, "url", &val);
    url = (const char *)malloc(strlen(val)+1);
    strcpy((char *)url, val);
    #ifdef DEBUG
    fprintf(stderr, "Read property [url] => %s\n", url);
    #endif
  }

  if (cafile == NULL)
  {
    config_lookup_string(&cfg, "cafile", &val);
    cafile = (const char *)malloc(strlen(val)+1);
    strcpy((char *)cafile, val);
    #ifdef DEBUG
    fprintf(stderr, "Read property [cafile] => %s\n", cafile);
    #endif
  }

  if (sslcheck == NULL)
  {
    config_lookup_string(&cfg, "sslcheck", &val);
    sslcheck = (const char *)malloc(strlen(val)+1);
    strcpy((char *)sslcheck, val);
    #ifdef DEBUG
    fprintf(stderr, "Read property [sslcheck] => %s\n", sslcheck);
    #endif
  }

  if (username == NULL)
  {
    config_lookup_string(&cfg, "username", &val);
    username = (const char *)malloc(strlen(val)+1);
    strcpy((char *)username, val);
    #ifdef DEBUG
    fprintf(stderr, "Read property [username] => %s\n", username);
    #endif
  }

  if (password == NULL)
  {
    config_lookup_string(&cfg, "password", &val);
    password = (const char *)malloc(strlen(val)+1);
    strcpy((char *)password, val);
    #ifdef DEBUG
    fprintf(stderr, "Read property [password] => %s\n", password);
    #endif
  }

  config_destroy(&cfg); 
}

size_t bodycallback(char *ptr, size_t size, size_t nmemb, void *userdata)
{
  char* pstr = (char *)ptr;

  char **rows = split_str(pstr, "\n");
  if (rows == NULL || rows[0] == NULL) return nmemb*size;

  int i = 0;
  for (i = 0; rows[i]; i++)
  {
    #ifdef DEBUG
    fprintf(stderr, "Read body line: %s\n", rows[i]);
    #endif

    BODY *curbody = (BODY *) malloc(sizeof(BODY));
    curbody->row = rows[i];
    curbody->next = body;
    body = curbody;
  }

  return nmemb*size;
}

enum nss_status _nss_shib_getpwnam_r(const char *name, struct passwd *result, char *buffer, size_t buflen, int *errnop)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_getpwnam_r with name=%s.\n", name);
  #endif

  readconfig();
  char newurl[1024];
  sprintf(newurl, "%s?passwd", url);
  if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
  if (body == NULL) return NSS_STATUS_UNAVAIL;

  BODY *cursor = body;
  while (cursor)
  {
    char *cur_row = (char *)malloc(strlen(cursor->row)+1);
    strcpy(cur_row, cursor->row);
    char **array = split_str(cur_row, ":");
    if (array[0] != NULL)
    {
      if (strcmp(array[0], name) == 0)
      {
        result->pw_name = array[0];
        result->pw_passwd = array[1];
        result->pw_uid = atoi(array[2]);
        result->pw_gid = atoi(array[3]);
        result->pw_gecos = array[4];
        result->pw_dir = array[5];
        result->pw_shell = array[6];

        #ifdef DEBUG
        fprintf(stderr, "Found item for name=%s: [uid=%d, gid=%d]\n", name, atoi(array[2]), atoi(array[3]));
        #endif

        cleanbody();
        return NSS_STATUS_SUCCESS;
      }
    }

    free(cur_row);
    cursor = cursor->next;
  }

  cleanbody();
  return NSS_STATUS_NOTFOUND;
}

enum nss_status _nss_shib_getpwuid_r(uid_t uid, struct passwd *result, char *buffer, size_t buflen, int *errnop)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_getpwuid_r with uid=%d.\n", uid);
  #endif

  readconfig();
  char newurl[1024];
  sprintf(newurl, "%s?passwd", url);
  if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
  if (body == NULL) return NSS_STATUS_UNAVAIL;

  BODY *cursor = body;
  while (cursor)
  {
    char *cur_row = (char *)malloc(strlen(cursor->row)+1);
    strcpy(cur_row, cursor->row);
    char **array = split_str(cur_row, ":");
    if (array[0] != NULL)
    {
      if (atoi(array[2]) == uid)
      {
        result->pw_name = array[0];
        result->pw_passwd = array[1];
        result->pw_uid = atoi(array[2]);
        result->pw_gid = atoi(array[3]);
        result->pw_gecos = array[4];
        result->pw_dir = array[5];
        result->pw_shell = array[6];

        #ifdef DEBUG
        fprintf(stderr, "Found item for uid=%d: [name=%s, gid=%d]\n", uid, array[0], atoi(array[3]));
        #endif

        cleanbody();
        return NSS_STATUS_SUCCESS;
      }
    }

    free(cur_row);
    cursor = cursor->next;
  }

  cleanbody();
  return NSS_STATUS_NOTFOUND;
}

enum nss_status _nss_shib_setpwent(void)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_setpwent.\n");
  #endif

  last_rownum_pwd = -1;
  return NSS_STATUS_SUCCESS;
}

enum nss_status _nss_shib_getpwent_r(struct passwd *result, char *buffer, size_t buflen, int *errnop)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_getpwent_r (iter=%d).\n", last_rownum_pwd);
  #endif

  readconfig();
  char newurl[1024];
  sprintf(newurl, "%s?passwd", url);
  if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
  if (body == NULL) return NSS_STATUS_UNAVAIL;

  int i = 0;
  BODY *cursor = body;
  while (cursor)
  {
    if (last_rownum_pwd == -1 && i > last_rownum_pwd)
    {
      char *cur_row = (char *)malloc(strlen(cursor->row)+1);
      strcpy(cur_row, cursor->row);
      char **array = split_str(cur_row, ":");
      if (array[0] != NULL)
      {
        result->pw_name = array[0];
        result->pw_passwd = array[1];
        result->pw_uid = atoi(array[2]);
        result->pw_gid = atoi(array[3]);
        result->pw_gecos = array[4];
        result->pw_dir = array[5];
        result->pw_shell = array[6];

        #ifdef DEBUG
        fprintf(stderr, "Found item: [name=%s, uid=%d, gid=%d]\n", array[0], atoi(array[2]), atoi(array[3]));
        #endif

        cleanbody();
        last_rownum_pwd = i;
        return NSS_STATUS_SUCCESS;
      }

      free(cur_row);
    }

    i++;
    cursor = cursor->next;
  }

  cleanbody();
  return NSS_STATUS_NOTFOUND;
}

enum nss_status _nss_shib_setgrent_r(struct group *result, void *args)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_setgrent_r.\n");
  #endif

  last_rownum_grp = -1;
  return NSS_STATUS_SUCCESS;
}

enum nss_status _nss_shib_endgrent_r(struct group *result, void* args)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_endgrent_r.\n");
  #endif

  last_rownum_grp = -1;
  return NSS_STATUS_SUCCESS;
}

enum nss_status _nss_shib_getgrent_r(struct group *result, char *buffer, size_t buflen, int *errnop)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_getgrent_r (iter=%d).\n", last_rownum_grp);
  #endif

  readconfig();
  char newurl[1024];
  sprintf(newurl, "%s?group", url);
  if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
  if (body == NULL) return NSS_STATUS_UNAVAIL;

  int i = 0;
  BODY *cursor = body;
  while (cursor)
  {
    if (last_rownum_grp == -1 || i > last_rownum_grp)
    {
      char *cur_row = (char *)malloc(strlen(cursor->row)+1);
      strcpy(cur_row, cursor->row);
      char **array = split_str(cur_row, ":");
      if (array[0] != NULL)
      {
        result->gr_name = array[0];
        result->gr_passwd = array[1];
        result->gr_gid = atoi(array[2]);
        result->gr_mem = split_str(array[3], ",");

        #ifdef DEBUG
        fprintf(stderr, "Found item: [grname=%s, gid=%d]\n", array[0], atoi(array[2]));
        #endif

        cleanbody();
        last_rownum_grp = i;
        return NSS_STATUS_SUCCESS;
      }

      free(cur_row);
    }

    i++;
    cursor = cursor->next;
  } 

  cleanbody(); 
  return NSS_STATUS_NOTFOUND;
}

enum nss_status _nss_shib_getgrnam_r(const char *name, struct group *result, char *buffer, size_t buflen, int *errnop)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_getgrnam_r with grname=%s.\n", name);
  #endif

  readconfig();
  char newurl[1024];
  sprintf(newurl, "%s?group", url);
  if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
  if (body == NULL) return NSS_STATUS_UNAVAIL;

  BODY *cursor = body;
  while (cursor)
  {
    char *cur_row = (char *)malloc(strlen(cursor->row)+1);
    strcpy(cur_row, cursor->row);
    char **array = split_str(cur_row, ":");
    if (array[0] != NULL)
    {
      if (strcmp(array[0], name) == 0)
      {
        result->gr_name = array[0];
        result->gr_passwd = array[1];
        result->gr_gid = atoi(array[2]);
        result->gr_mem = split_str(array[3], ",");

        #ifdef DEBUG
        fprintf(stderr, "Found item for grname=%s: [gid=%d]\n", name, atoi(array[2]));
        #endif

        cleanbody();
        return NSS_STATUS_SUCCESS;
      }
    }

    free(cur_row);
    cursor = cursor->next;
  }  

  cleanbody();
  return NSS_STATUS_NOTFOUND;
}

enum nss_status _nss_shib_getgrgid_r(gid_t gid, struct group *result, char *buffer, size_t buflen, int *errnop)
{
  #ifdef DEBUG
  fprintf(stderr, "\nEntering _nss_shib_getgrgid_r with gid=%d.\n", gid);
  #endif

  readconfig();
  char newurl[1024];
  sprintf(newurl, "%s?group", url);
  if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
  if (body == NULL) return NSS_STATUS_UNAVAIL;

  BODY *cursor = body;
  while (cursor)
  {
    char *cur_row = (char *)malloc(strlen(cursor->row)+1);
    strcpy(cur_row, cursor->row);
    char **array = split_str(cur_row, ":");
    if (array[0] != NULL && atoi(array[2]) == gid)
    {
      result->gr_name = array[0];
      result->gr_passwd = array[1];
      result->gr_gid = atoi(array[2]);
      result->gr_mem = split_str(array[3], ",");

      #ifdef DEBUG
      fprintf(stderr, "Found item for gid=%d: [name=%s]\n", gid, array[0]);
      #endif

      cleanbody();
      return NSS_STATUS_SUCCESS;
    }

    free(cur_row);
    cursor = cursor->next;
  }  

  cleanbody();
  return NSS_STATUS_NOTFOUND;
}
