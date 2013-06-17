#ifndef _PAM_BROWSER_H_
#define _PAM_BROWSER_H_

typedef struct _BODY
{
	char *row;
	struct _BODY *next;
} BODY;

void free_cookies();
BODY *get_cookies(void);
void set_cookies(BODY *newcookies);
void cleanbody(void);
size_t bodycallback(char *ptr, size_t size, size_t nmemb, void *userdata);
int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck);

#endif /*_PAM_BROWSER_H_*/

