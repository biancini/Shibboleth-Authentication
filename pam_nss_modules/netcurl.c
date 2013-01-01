#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>

#include <curl/curl.h>
/*#include <curl/types.h>*/
#include <curl/easy.h>
#include "netcurl.h"

struct curl_slist *cookies = NULL;

void free_cookies();

void set_cookies(struct curl_slist *newcookies)
{
  if(cookies) free_cookies();
  cookies = newcookies;
}

struct curl_slist *get_cookies()
{
  return cookies;
}

void free_cookies()
{
  while (cookies)
  {
    struct curl_slist *next = cookies->next;
    if (cookies->data != NULL) free(cookies->data);
    free(cookies);
    cookies = next;
  }
}

int count_char_in_str(char *str, char find)
{
  int count = 0;
  int i = 0;

  if (str == NULL) return count;
  for (i = 0; str[i]; i++)
  {
    if (str[i] == find) count++;
  }

  return count;
}

char *replace_char(char *str, char orig, char rep)
{
  int i = 0;
  for (i = 0; str[i]; i++)
  {
    if (str[i] == orig) str[i] = rep;
  }

  return str;
}

char *replace_str(const char *str, const char *find, const char *rep)
{
  int 		count = 0;
  const char 	*ins = str,
		*tmp = NULL;
  char		*ret = NULL;
  const size_t	len_find = strlen(find),
		len_rep = strlen(rep);

  for(count = 0; 0 != (tmp = strstr(ins, find)); ++count)
   ins = tmp + len_find;

  char *tmp2 = ret = (char*)malloc(strlen(str) + (len_rep - len_find)*count + 1);
  if(!ret)
    return NULL;

  while(count--)
  {
    ins = strstr(str, find);
    const size_t cp_size = ins - str;
    tmp2 = strncpy(tmp2, str, cp_size) + cp_size;
    tmp2 = strcpy(tmp2, rep) + len_rep;
    str += cp_size + len_rep;
  }
  strcpy(tmp2, str);

  return ret;
}

char *extract_str(char *str, const char start, const char end)
{
    int len = 0;
    int write = -1;
    int i = 0;
    int j = 0;

    for (i = 0; str[i]; i++)
    {
      if (write == -1 && i > 0 && str[i-1] == start) write = 0;
      if (write == 0 && str[i] == end) write = 1;
      if (write == 0) len++;
    }

    char *newstr = (char *)malloc(len+1);
    write = -1;
    for (i = 0; str[i]; i++)
    {
      if (write == -1 && i > 0 && str[i-1] == start) write = 0;
      if (write == 0 && str[i] == end) write = 1;
      if (write == 0) newstr[j++] = str[i];
    }
    newstr[j] = '\0';

    return newstr;
}

char **split_str(char *str, const char *delimiters)
{
  char **tokenArray = (char **) malloc(sizeof(char *));
  int count = 1;
  int i = 0;

  tokenArray[0] = NULL;
  if (str == NULL) return tokenArray;
  tokenArray = (char **) realloc(tokenArray, 2*sizeof(char *));
  tokenArray[0] = &str[0];
  tokenArray[1] = NULL;

  for (i = 0; str[i]; i++)
  {
    if (str[i] == delimiters[0])
    {
      str[i] = '\0';

      if (str[i+1])
      {
        tokenArray = (char **) realloc(tokenArray, (count+2)*sizeof(char *));
        tokenArray[count] = &str[i+1];
        tokenArray[count+1] = NULL;
        count++;
      }
    }
  }

  return tokenArray;
}

static size_t headercallback(void *ptr, size_t size, size_t nmemb, void *userdata)
{
  char	*eqsign = NULL,
	*newstr = NULL;
  if (ptr == NULL) return -1;

  char *pstr = (char *)ptr;

  char headcookie[] = "Set-Cookie";
  if (strcasestr(pstr, headcookie))
  {
    newstr = extract_str(pstr, ' ', ';');
    eqsign = strchr(newstr, '=');

    int found = 1;
    struct curl_slist *cursor = cookies;

    if(eqsign) {
      *eqsign = '\0'; 

      while (cursor)
      {
        if (strcasestr(cursor->data, newstr))
        {
          free(cursor->data);
          *eqsign = '=';
          cursor->data = strdup(newstr);
          if(!cursor->data) goto on_err;
          found = 0;
        }
      }

      cursor = cursor->next;
      *eqsign = '=';
    }

    if (found != 0)
    {
      struct curl_slist *curcookie = (struct curl_slist *) malloc(sizeof(struct curl_slist));
      curcookie->data = strdup(newstr);
      if(!cursor->data) goto on_err;
      curcookie->next = cookies;
      cookies = curcookie;
    }
  }

on_err:
  if(newstr) free(newstr);

  return nmemb * size;  
}

static long __geturl(const char *url, const char *userpass, const char *cafile, const char *sslcheck, char **url_new)
{
  CURLcode	hResult = 0;
  CURL		*hCurl = curl_easy_init();
  long 		http_code = 0;

  if (!hCurl) return http_code;

  hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_NOPROGRESS, 1);
  hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_FAILONERROR, 0);
  hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_FOLLOWLOCATION, 0);
  hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_HEADERFUNCTION, headercallback); 
  hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_WRITEFUNCTION, bodycallback);
  // seed SSL randomness from somewhere; this is really problematic
  // because libcurl wants to read 16 kilobytes of randomness.  (Why
  // does it think it needs 131072 bits?  Does it think someone might
  // spend 10^39334 universe-lifetimes to brute-force our SSL
  // connection?)
  hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_RANDOM_FILE, "/dev/urandom");

  if (!strcasecmp(sslcheck, "true"))
  {
    hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYPEER, 1);
    hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYHOST, 2);
    if (cafile)
      hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_CAINFO, cafile);
  }
  else
  {
    hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYPEER, 0);
    hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYHOST, 0);
  }
 
  char passcookies[2048];
  passcookies[0] = '\0';

  if (cookies != NULL)
  {
    struct curl_slist *cursor = cookies;
    while (cursor)
    {
      if (passcookies[0] != '\0') strncat(passcookies, ";", 2048);
      strncat(passcookies, cursor->data, 2048);

      cursor = cursor->next;  
    }
  }

  const size_t cookiesSz = strlen(passcookies);
  if (cookiesSz > 0)
  {
    if(cookiesSz == 2047) goto opt_err;
    hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_COOKIE, passcookies);
  }

  #ifdef DEBUG
  fprintf(stderr, "Opening URL: %s\n", url);
  fprintf(stderr, "Passing the following cookies: %s\n", passcookies);
  #endif

  if (userpass)
  {
    #ifdef DEBUG
    fprintf(stderr, "Basic authentication string: %s\n", userpass);
    #endif

    hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
    hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_USERPWD, userpass);
  }
  hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_URL, url);
  if(hResult)
    goto opt_err;

  hResult = curl_easy_perform(hCurl);
  if(CURLE_OK != hResult)
    goto opt_err;

  if(CURLE_OK != curl_easy_getinfo(hCurl, CURLINFO_RESPONSE_CODE, &http_code))
    goto opt_err;
  #ifdef DEBUG
  fprintf(stderr, "Response code: %ld\n", http_code);
  #endif

  char *redir_url = NULL;
  curl_easy_getinfo(hCurl, CURLINFO_REDIRECT_URL, &redir_url);

  if (url_new && CURLE_OK == curl_easy_getinfo(hCurl, CURLINFO_REDIRECT_URL, &redir_url) && redir_url)
  {
    *url_new = strdup(redir_url);
  }

opt_err:
  curl_easy_cleanup(hCurl);

  return http_code;
}

int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck)
{
  char	*userpass = NULL;
  char	*url_lcl = strdup(url),
	*url_new = NULL;
  long	http_code = 0;

  if(curl_global_init(CURL_GLOBAL_ALL))
    return 0;

  while(1)
  {
    cleanbody();
    http_code = __geturl(url_lcl, userpass, cafile, sslcheck, &url_new);

    if (http_code == 401 && !userpass)
    {
      #ifdef DEBUG
      fprintf(stderr, "Adding Basic authentication directives.\n");
      #endif

      userpass = (char *) malloc(strlen(username)+strlen(password)+2);
      if (!userpass) goto cleanup;
      sprintf(userpass, "%s:%s", username, password);
      if(url_new)
      {
        free(url_new);
        url_new = NULL;
      }
      continue;
    }

    if(url_new && strlen(url_new))
    {
      free(url_lcl);
      url_lcl = url_new;
      url_new = NULL;
      
    } else break;
  }

cleanup:
  if (userpass)
  {
    memset(userpass, '\0', strlen(userpass));
    free(userpass);
  }
  if(url_lcl) free(url_lcl);
  if(url_new) free(url_new);
  free_cookies();
  curl_global_cleanup();

  return http_code == 200;
}

