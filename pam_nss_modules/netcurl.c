#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>

#include <curl/curl.h>
#include <curl/easy.h>
#include "stringlibs.h"
#include "pam_browser.h"

BODY *cookies = NULL;

void free_cookies() {
        while (cookies) {
                BODY *next = cookies->next;
                if (cookies->row != NULL) free(cookies->row);
                free(cookies);
                cookies = next;
        }
}

void set_cookies(BODY *newcookies) {
        cookies = newcookies;
}

BODY *get_cookies() {
        return cookies;
}

static size_t headercallback(void *ptr, size_t size, size_t nmemb, void *userdata) {
	char	*eqsign = NULL,
		*newstr = NULL;
	if (ptr == NULL) return -1;

	char *pstr = (char *)ptr;

	char headcookie[] = "Set-Cookie";
	if (strcasestr(pstr, headcookie)) {
		newstr = extract_str(pstr, ' ', ';');
		eqsign = strchr(newstr, '=');

		int found = 1;
		BODY *cursor = cookies;

		if(eqsign) {
			*eqsign = '\0'; 

			while (cursor) {
				if (strcasestr(cursor->row, newstr)) {
					free(cursor->row);
					*eqsign = '=';
					cursor->row = strdup(newstr);
					if(!cursor->row) goto on_err;
					found = 0;
				}
				cursor = cursor->next;
			}

			*eqsign = '=';
		}

		if (found != 0) {
			BODY *curcookie = (BODY *) malloc(sizeof(BODY));
			curcookie->row = strdup(newstr);
			if(!curcookie->row) goto on_err;
			curcookie->next = cookies;
			cookies = curcookie;
		}
	}

on_err:
	if(newstr) free(newstr);
	return nmemb * size;  
}

static long _geturl(const char *url, const char *userpass, const char *cafile, const char *sslcheck, char **url_new) {
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

	if (!strcasecmp(sslcheck, "true")) {
		hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYPEER, 1);
		hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYHOST, 2);
		if (cafile) hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_CAINFO, cafile);
	}
	else {
		hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYPEER, 0);
		hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_SSL_VERIFYHOST, 0);
	}

	char passcookies[2048];
	passcookies[0] = '\0';

	if (cookies != NULL) {
		BODY *cursor = cookies;
		while (cursor) {
			if (passcookies[0] != '\0') strncat(passcookies, ";", 2048);
			strncat(passcookies, cursor->row, 2048);

			cursor = cursor->next;  
		}
	}

	const size_t cookiesSz = strlen(passcookies);
	if (cookiesSz > 0) {
		if(cookiesSz == 2047) goto opt_err;
		hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_COOKIE, passcookies);
	}

#ifdef DEBUG
	fprintf(stderr, "Opening URL: %s\n", url);
	fprintf(stderr, "Passing the following cookies: %s\n", passcookies);
#endif

	if (userpass) {
#ifdef DEBUG
		fprintf(stderr, "Basic authentication string: %s\n", userpass);
#endif

		hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_HTTPAUTH, CURLAUTH_BASIC);
		hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_USERPWD, userpass);
	}
	hResult |= CURLE_OK != curl_easy_setopt(hCurl, CURLOPT_URL, url);
	if (hResult) goto opt_err;

	hResult = curl_easy_perform(hCurl);
	if (CURLE_OK != hResult) goto opt_err;
	if (CURLE_OK != curl_easy_getinfo(hCurl, CURLINFO_RESPONSE_CODE, &http_code)) goto opt_err;

#ifdef DEBUG
	fprintf(stderr, "Response code: %ld\n", http_code);
#endif

	char *redir_url = NULL;
	curl_easy_getinfo(hCurl, CURLINFO_REDIRECT_URL, &redir_url);

	if (url_new && CURLE_OK == curl_easy_getinfo(hCurl, CURLINFO_REDIRECT_URL, &redir_url) && redir_url) {
		if (*url_new) free(*url_new);
		*url_new = strdup(redir_url);
	}

opt_err:
	curl_easy_cleanup(hCurl);
	return http_code;
}

int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck) {
	char	*userpass = NULL;
	char	*url_lcl = strdup(url),
		*url_new = NULL;
	long	http_code = 0;

	curl_global_init(CURL_GLOBAL_ALL);
	while (1) {
		cleanbody();
		http_code = _geturl(url_lcl, userpass, cafile, sslcheck, &url_new);

		if (userpass) {
			memset(userpass, '\0', strlen(userpass));
			free(userpass);
			userpass = NULL;
			if (http_code == 401) goto cleanup;
		}

		if (http_code == 401) {
#ifdef DEBUG
			fprintf(stderr, "Adding Basic authentication directives.\n");
#endif

			userpass = (char *) malloc(strlen(username)+strlen(password)+2);
			if (!userpass) goto cleanup;
			sprintf(userpass, "%s:%s", username, password);
			if (url_new) {
				free(url_new);
				url_new = NULL;
			}
			continue;
		}

		if (url_new && strlen(url_new)) {
			free(url_lcl);
			url_lcl = url_new;
			url_new = NULL;

		}
		else break;
	}

cleanup:
	if(url_lcl) free(url_lcl);
	if(url_new) free(url_new);
	free_cookies();
	if (http_code != 200) cleanbody();
	curl_global_cleanup();
	return http_code == 200;
}

