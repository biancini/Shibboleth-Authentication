typedef struct _SESSION
{
  char *key;
  char *value;
  struct _SESSION *next;
} SESSION;

typedef struct _BODY
{
  char *row;
  struct _BODY *next;
} BODY;

char **split_str(char *str, const char *delimiters);
size_t bodycallback(char *ptr, size_t size, size_t nmemb, void *userdata);
size_t headercallback(void *ptr, size_t size, size_t nmemb, void *userdata);
int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck);
char *getsessvalue(const char *key);
