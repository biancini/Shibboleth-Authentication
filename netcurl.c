#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>

#include <curl/curl.h>
#include <curl/types.h>
#include <curl/easy.h>
#include "netcurl.h"

struct curl_slist *cookies = NULL;

void free_cursor(struct curl_slist *cursor)
{
  if (cursor->next != NULL) free_cursor(cursor->next);
  free(cursor);
}

void free_cookies()
{
  if (cookies == NULL) return;
  free_cursor(cookies);
}

static size_t nop_wf(void* a, size_t x, size_t y, void* b)
{
  return x*y;
}

char **split_str(char *str, const char *delimiters)
{
  char **tokenArray = (char**) malloc(sizeof(char *));
  int count = 1;
  int i = 0;

  tokenArray[0] = NULL;
  if (str == NULL || strstr(str, delimiters) == NULL) return tokenArray;
  tokenArray = (char**) realloc(tokenArray, 2*sizeof(char *));
  tokenArray[0] = str;
  tokenArray[1] = NULL;

  for (i = 0; str[i]; i++)
  {
    if (str[i] == delimiters[0])
    {
       tokenArray = (char **) realloc(tokenArray, (count+2)*sizeof(char *));
       str[i] = '\0';
       tokenArray[count] = &str[i+1];
       tokenArray[count+1] = NULL;
       count++;
    }
  }

  return tokenArray;
}

size_t headercallback(void *ptr, size_t size, size_t nmemb, void *userdata)
{
  int i = 0;
  int j = 0;
  int write = -1;
  if (ptr == NULL) return -1;

  char *pstr = (char *)ptr;

  char headcookie[] = "Set-Cookie";
  if (strcasestr(pstr, headcookie))
  {
    int len = 0;
    for (i = 0; pstr[i]; i++)
    {
      if (write == -1 && i > 0 && pstr[i-1] == ' ') write = 0;
      if (write == 0 && pstr[i] == ';') write = 1;
      if (write == 0) len++;
    }

    char *newstr = (char *)malloc(len+1);
    int eqsign = -1;
    write = -1;
    for (i = 0; pstr[i]; i++)
    {
      if (write == -1 && i > 0 && pstr[i-1] == ' ') write = 0;
      if (write == 0 && pstr[i] == ';') write = 1;
      if (write == 0) newstr[j++] = pstr[i];
      if (eqsign == -1 && newstr[j-1] == '=') eqsign = j-1;
    }
    newstr[j] = '\0';

    int found = 1;
    struct curl_slist *cursor = cookies;
    while (cursor)
    {
      if (eqsign > 0)
      {
        newstr[eqsign] = '\0';
        if (strcasestr(cursor->data, newstr))
        {
          free(cursor->data);
          newstr[eqsign] = '=';
          cursor->data = newstr;
          found = 0;
        }
        else
        {
          newstr[eqsign] = '=';
        }
      }

      cursor = cursor->next;
    }

    if (found != 0)
    {
      struct curl_slist *curcookie = (struct curl_slist *) malloc(sizeof(struct curl_slist));
      curcookie->data = newstr;
      curcookie->next = cookies;
      cookies = curcookie;
    }
  }

  return nmemb * size;  
}

long __geturl(const char *url, const char *userpass, const char *cafile, const char *sslcheck, char **url_new)
{
  CURLcode hResult = -1;
  curl_global_init(CURL_GLOBAL_ALL);
  CURL *hCurl = curl_easy_init();
  long http_code = 0;

  if (!hCurl) return http_code;

  curl_easy_setopt(hCurl, CURLOPT_URL, url);
  curl_easy_setopt(hCurl, CURLOPT_WRITEFUNCTION, nop_wf);
  curl_easy_setopt(hCurl, CURLOPT_NOPROGRESS, 1);
  curl_easy_setopt(hCurl, CURLOPT_FAILONERROR, 1);

  curl_easy_setopt(hCurl, CURLOPT_FOLLOWLOCATION, 0);
  curl_easy_setopt(hCurl, CURLOPT_HEADERFUNCTION, headercallback); 
  curl_easy_setopt(hCurl, CURLOPT_WRITEFUNCTION, bodycallback);

  // seed SSL randomness from somewhere; this is really problematic
  // because libcurl wants to read 16 kilobytes of randomness.  (Why
  // does it think it needs 131072 bits?  Does it think someone might
  // spend 10^39334 universe-lifetimes to brute-force our SSL
  // connection?)
  curl_easy_setopt(hCurl, CURLOPT_RANDOM_FILE, "/dev/urandom");

  if (strcasestr(sslcheck, "true"))
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
 
  char passcookies[2048];
  passcookies[0] = '\0';
  if (cookies != NULL)
  {
    struct curl_slist *cursor	= cookies;
    while (cursor)
    {
      if (passcookies[0] != '\0') strcat(passcookies, ";");
      strcat(passcookies, cursor->data);

      cursor = cursor->next;  
    }
  }

  if (passcookies != NULL && strlen(passcookies) > 0) curl_easy_setopt(hCurl, CURLOPT_COOKIE, passcookies);

  #ifdef DEBUG
  fprintf(stderr, "Opening URL: %s\n", url);
  fprintf(stderr, "Passing the following cookies: %s\n", passcookies);
  #endif

  if (userpass)
  {
    #ifdef DEBUG
    fprintf(stderr, "Basic authentication string: %s\n", userpass);
    #endif

    curl_easy_setopt(hCurl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
    curl_easy_setopt(hCurl, CURLOPT_USERPWD, userpass);
  }

  curl_easy_setopt(hCurl, CURLOPT_URL, url);
  hResult = curl_easy_perform(hCurl);

  curl_easy_getinfo(hCurl, CURLINFO_RESPONSE_CODE, &http_code);
  #ifdef DEBUG
  fprintf(stderr, "Response code: %ld\n", http_code);
  #endif

  char *redir_url = url_new[0] = NULL;
  curl_easy_getinfo(hCurl, CURLINFO_REDIRECT_URL, &redir_url);

  if (redir_url != NULL)
  {
    url_new[0] = (char *) malloc(strlen(redir_url)+1);
    strcpy(url_new[0], redir_url);
  }

  curl_easy_cleanup(hCurl);
  curl_global_cleanup();

  return http_code;
}

int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck)
{
  char *userpass = NULL;
  long http_code = 0;
  char *url_call = NULL;
  char *url_old = NULL;

  char *url_new = (char *)malloc(strlen(url)+1);
  strcpy(url_new, url);

  do
  {
    url_old = (char *)malloc(strlen(url_new)+1);
    strcpy(url_old, url_new);

    url_call = (char *)malloc(strlen(url_new)+1);
    strcpy(url_call, url_new);
    cleanbody();
    http_code = __geturl(url_call, NULL, cafile, sslcheck, &url_new);
    free(url_call);

    if (http_code == 401)
    {
      #ifdef DEBUG
      fprintf(stderr, "Adding Basic authentication directives.\n");
      #endif

      userpass = malloc(strlen(username)+strlen(password)+1);
      sprintf(userpass, "%s:%s", username, password);
      if (!userpass) goto cleanup;

      cleanbody();
      http_code = __geturl(url_old, userpass, cafile, sslcheck, &url_new);
    }
    else free(url_old);
  }
  while (url_new != NULL && strlen(url_new) > 0);

 cleanup:
  if (userpass)
  {
    memset(userpass, '\0', strlen(userpass));
    free(userpass);
  }

  free_cookies();
  return http_code == 200;
}

