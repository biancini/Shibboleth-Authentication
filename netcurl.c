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
  if (strstr(str, delimiters) == NULL) return tokenArray;
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

int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck)
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
  fprintf(stderr, "Basic authentication string: %s\n", userpass);
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
 
  new_url = (char *)malloc(strlen(url)+1);
  strcpy(new_url, url);
  new_url[strlen(url)] = '\n';

  do
  {
    char *passcookies = NULL;
    if (cookies != NULL)
    {
      struct curl_slist *cursor	= cookies;
      while (cursor)
      {
        if (passcookies == NULL)
        {
          passcookies = (char *)malloc(strlen(cursor->data)+1);
          strcpy(passcookies, cursor->data);
          passcookies[strlen(cursor->data)] = '\0';
        }
        else
        {
          char *oldcookies = passcookies;
          passcookies = (char *)malloc(strlen(oldcookies)+strlen(cursor->data)+2);
          sprintf(passcookies, "%s;%s", oldcookies, cursor->data);
          passcookies[strlen(passcookies)+strlen(cursor->data)+1] = '\0';
          free(oldcookies);
        }

        cursor = cursor->next;  
      }
    }

    curl_easy_setopt(hCurl, CURLOPT_COOKIE, passcookies);
    char *nurl = (char *)malloc(strlen(new_url) + 1);
    strcpy(nurl, new_url);
    nurl[strlen(new_url)] = '\0';

    #ifdef DEBUG
    fprintf(stderr, "Redirecting to URL: %s\n", new_url);
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
      fprintf(stderr, "Response code: %ld\n", http_code);
      #endif

      if (http_code == 401) {
        #ifdef DEBUG
        fprintf(stderr, "Adding Basic authentication directives.\n");
        #endif

        curl_easy_setopt(hCurl, CURLOPT_WRITEFUNCTION, bodycallback);
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

 cleanup:
  curl_easy_cleanup(hCurl);
  curl_global_cleanup();

  if (new_url != NULL) free(new_url);
  memset(userpass, '\0', strlen(userpass));
  free(userpass);

  return hResult == 0;
}

