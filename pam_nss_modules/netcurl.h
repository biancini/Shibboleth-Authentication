#ifndef _NETCURL_H_
#define _NETCURL_H_

typedef struct _BODY
{
  char *row;
  struct _BODY *next;
} BODY;

struct curl_slist *get_cookies(void);
void set_cookies(struct curl_slist *newcookies);
int count_char_in_str(char *str, char find);
char *replace_char(char *str, char orig, char rep);
char *replace_str(const char *str,const char *orig,const char *rep);
char *extract_str(char *str, const char start, const char end);
char **split_str(char *str, const char *delimiters);
void cleanbody(void);
size_t bodycallback(char *ptr, size_t size, size_t nmemb, void *userdata);
int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck);

#endif /*_NETCURL_H_*/

