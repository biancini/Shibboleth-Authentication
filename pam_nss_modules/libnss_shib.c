#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>
#include <libconfig.h>
#include <sys/param.h>
#include <sys/types.h>
#include <regex.h>

#include <pwd.h>
#include <grp.h>
#include <nss.h>
#include <errno.h>

#include <curl/curl.h>
#include <curl/easy.h>
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
void print_body(BODY *pointer) {
	if (pointer == NULL) return;

	fprintf(stderr, "-> Printing: ");
	BODY *cursor = pointer;
	while(cursor) {
		fprintf(stderr, "%s, ", cursor->row);
		cursor = cursor->next;
	}
	fprintf(stderr, "\n");
}
#endif

void cleanbody() {
	while(body) {
		BODY *next = body->next;
		free(body);
		body = next;
	}
}

static void readconfig_value(config_t *cfg, const char *valname, const char **value) {
	if (*value == NULL) {
		const char *val = NULL;
		if (CONFIG_TRUE != config_lookup_string(cfg, valname, &val)) goto on_err;

		*value = strdup(val);
		if (*value == NULL) goto on_err;

#ifdef DEBUG
		fprintf(stderr, "Read property [%s] => %s\n", valname, *value);
#endif
	}

	return;
on_err:
#ifdef DEBUG
	fprintf(stderr, "Error reading property [%s]\n", valname);
#endif

	if (*value) free((void*)*value);
	*value = NULL;
}

static void readconfig() {
	const char	*val = NULL;
#ifdef DEBUG
	fprintf(stderr, "Reading configuration file...");
#endif

	config_t cfg;

	config_init(&cfg);
	if (!config_read_file(&cfg, config_file))   {
		char *message = "Config file not found in /etc/libnss.conf.\n";
		syslog(LOG_ERR, "%s", message);

#ifdef DEBUG
		fprintf(stderr, "%s", message);
		fprintf(stderr, "%s\n", config_error_text(&cfg));
#endif

		config_destroy(&cfg);  
		return;
	}

	readconfig_value(&cfg, "url", &url);
	readconfig_value(&cfg, "cafile", &cafile);
	readconfig_value(&cfg, "sslcheck", &sslcheck);
	readconfig_value(&cfg, "username", &username);
	readconfig_value(&cfg, "password", &password);

	if (get_cookies() == NULL) {
		int		i = 0;
		regex_t	envVar;
		readconfig_value(&cfg, "cookie_num", &val);
		if (val == NULL) goto end_config;
		int num_cookies = atoi(val);

		if(regcomp(&envVar, "\\$\\{[a-zA-Z0-9_-]+\\}", REG_EXTENDED))
			goto end_config;

		for(i=1; i <= num_cookies; ++i) {
			char 	strkey[512],
				*cookie_name = NULL,
				*cookie_value = NULL;

			sprintf(strkey, "cookie_%d_name", i);
			readconfig_value(&cfg, strkey, (const char **)&cookie_name);
			sprintf(strkey, "cookie_%d_value", i);
			readconfig_value(&cfg, strkey, (const char **)&cookie_value);
			if(!cookie_value || !cookie_name)
				goto on_gen_err;
			/* expand cookie_name */
			char		*tmpVal = cookie_name,
					tmpBuf[2048];
			tmpBuf[0] = '\0';
			regmatch_t	match; 
			while(!regexec(&envVar, tmpVal, 1, &match, 0)) {
				if(-1 == match.rm_so)
					break;
				strncat(tmpBuf, tmpVal, match.rm_so);
				char            *pEnd = &tmpVal[match.rm_eo-1],
						tVal = *pEnd;
				*pEnd = '\0';
				const char	*envVal = getenv(tmpVal + match.rm_so + 2);
				*pEnd = tVal;
				tmpVal += match.rm_eo;
				if(envVal) strcat(tmpBuf, envVal);
			}
			free(cookie_name);
			cookie_name = strdup(tmpBuf);
			if(!cookie_name) goto on_gen_err;
			/* do the same for cookie_value */
			tmpVal = cookie_value;
			tmpBuf[0] = '\0';
			while(!regexec(&envVar, tmpVal, 1, &match, 0)) {
				if(-1 == match.rm_so)
					break;
				strncat(tmpBuf, tmpVal, match.rm_so);
				char            *pEnd = &tmpVal[match.rm_eo-1],
						tVal = *pEnd;
				*pEnd = '\0';
				const char	*envVal = getenv(tmpVal + match.rm_so + 2);
				*pEnd = tVal;
				tmpVal += match.rm_eo;
				if(envVal) strcat(tmpBuf, envVal);
			}
			free(cookie_value);
			cookie_value = strdup(tmpBuf);
			if(!cookie_value) goto on_gen_err;
			/* add to cookies */
			struct curl_slist *curcookie = (struct curl_slist *) malloc(sizeof(struct curl_slist));
			if(!curcookie) goto on_gen_err;
			curcookie->data = (char *) malloc(strlen(cookie_name)+strlen(cookie_value)+2);
			if(!curcookie->data) {
				free(curcookie);
				goto on_gen_err;
			}
			sprintf(curcookie->data, "%s=%s", cookie_name, cookie_value);
			curcookie->next = get_cookies();
			set_cookies(curcookie);

#ifdef DEBUG
			fprintf(stderr, "Read new cookie: [%s] => %s\n", cookie_name, cookie_value);
#endif

on_gen_err:
			if(cookie_value) free(cookie_value);
			if(cookie_name) free(cookie_name);
		}
		regfree(&envVar);
	}

end_config:
	config_destroy(&cfg);
	if(val) free((void*)val); 
}

size_t bodycallback(char *ptr, size_t size, size_t nmemb, void *userdata) {
	char* pstr = (char *)ptr;

	replace_char(pstr, '\r', ':');
	char **rows = split_str(pstr, '\n');
	if (rows == NULL || rows[0] == NULL) {
		free(rows);
		return nmemb*size;
	}

	int i = 0;
	for (i = 0; rows[i]; i++) {
#ifdef DEBUG
		fprintf(stderr, "Read body line: %s\n", rows[i]);
#endif

		BODY *curbody = (BODY *) malloc(sizeof(BODY) +  strlen(rows[i]) + 1);
		curbody->row = (char *) ((char*)curbody + sizeof(BODY));
		strcpy(curbody->row, rows[i]);
		curbody->next = body;
		body = curbody;
	}

	free(rows);
	return nmemb*size;
}

static int setpasswdfromarray(char **array, struct passwd *result, char *buffer, size_t buflen) {
	size_t	lens[7];
	int i = 0;

	for (i = 0; i <= 6; ++i) {
		lens[i] = strlen(array[i]);
	}

	if (buflen < lens[0] + lens[1] + lens[4] + lens[5] + lens[6] + 5) {
		return 1;
	}

	char *buf_ptr = buffer;
	memcpy(buf_ptr, array[0], lens[0] + 1);
	result->pw_name = buf_ptr;
	buf_ptr += lens[0] + 1;
	memcpy(buf_ptr, array[1], lens[1] + 1);
	result->pw_passwd = buf_ptr;
	buf_ptr += lens[1] + 1;
	memcpy(buf_ptr, array[4], lens[4] + 1);
	result->pw_gecos = buf_ptr;
	buf_ptr += lens[4] + 1;
	memcpy(buf_ptr, array[5], lens[5] + 1);
	result->pw_dir = buf_ptr;
	buf_ptr += lens[5] + 1;
	memcpy(buf_ptr, array[6], lens[6] + 1);
	result->pw_shell = buf_ptr;
	buf_ptr += lens[6] + 1;

	result->pw_uid = atoi(array[2]);
	result->pw_gid = atoi(array[3]);

	return 0;
}

static int setgroupfromarray(char **array, struct group *result, char *buffer, size_t buflen) {
	size_t lens[4];
	int i = 0;

	int num_members = (strlen(array[3]) == 0) ? 1 : count_char_in_str(array[3], ',') + 2;
	for (i = 0; i <= 3; ++i) {
		lens[i] = strlen(array[i]);
	}
	
	if (buflen < lens[0] + lens[1] + lens[3] + 3 + (sizeof(char *) * num_members)) {
		return 1;
	}

	char *buf_ptr = buffer;
	memcpy(buf_ptr, array[0], lens[0] + 1);
	result->gr_name = buf_ptr;
	buf_ptr += lens[0] + 1;
	memcpy(buf_ptr, array[1], lens[1] + 1);
	result->gr_passwd = buf_ptr;
	buf_ptr += lens[1] + 1;

	result->gr_gid = atoi(array[2]);

	char *ptr[128];
	int j = 0;
	char **members = split_str(array[3], ',');

	for (j = 0; ; j++) {
		if (members == NULL || members[j] == NULL || strlen(members[j]) == 0) {
			ptr[j] = NULL;
			break;
		}
		memcpy(buf_ptr, members[j], strlen(members[j]) + 1);
		buf_ptr += strlen(members[j]) + 1;
	}

	memcpy(buf_ptr, ptr, sizeof(char *) * num_members);
	result->gr_mem = (char **)buf_ptr;
	free(members);

	return 0;
}

enum nss_status _nss_shib_getpwnam_r(const char *name, struct passwd *result, char *buffer, size_t buflen, int *errnop) {
#ifdef DEBUG
	fprintf(stderr, "\nEntering _nss_shib_getpwnam_r with name=%s.\n", name);
#endif

	enum nss_status ret = NSS_STATUS_UNAVAIL;

	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?passwd", url);
	if (!geturl(newurl, username, password, cafile, sslcheck) || body == NULL) {
		ret = NSS_STATUS_UNAVAIL;
		goto getpwnam_err;
	}

	BODY *cursor = body;
	while (cursor) {
		char *cur_row = cursor->row;
		int count_separator = count_char_in_str(cur_row, ':');
		char **array = split_str(cur_row, ':');

		if (array[0] != NULL && count_separator >= 5 && strcmp(array[0], name) == 0) {
			int setting = setpasswdfromarray(array, result, buffer, buflen);
			if (setting != 0) {
				if (setting == 1) {
					if(array) free(array);

					*errnop = ERANGE;
					ret = NSS_STATUS_TRYAGAIN;
				}
				else {
					ret = NSS_STATUS_UNAVAIL;
				}
				goto getpwnam_err;
			}

#ifdef DEBUG
			fprintf(stderr, "Found item for name=%s: [uid=%s, gid=%s]\n", name, array[2], array[3]);
#endif

			ret = NSS_STATUS_SUCCESS;
		}

		if(array) free(array);
		cursor = cursor->next;
	}

getpwnam_err:
	cleanbody();
	return ret;
}

enum nss_status _nss_shib_getpwuid_r(uid_t uid, struct passwd *result, char *buffer, size_t buflen, int *errnop) {
#ifdef DEBUG
	fprintf(stderr, "\nEntering _nss_shib_getpwuid_r with uid=%d.\n", uid);
#endif
	enum nss_status ret = NSS_STATUS_UNAVAIL;

	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?passwd", url);
	if (!geturl(newurl, username, password, cafile, sslcheck) || body == NULL) {
		ret = NSS_STATUS_UNAVAIL;
		goto getpwuid_err;
	}

	BODY *cursor = body;
	while (cursor) {
		char *cur_row = cursor->row;
		int count_separator = count_char_in_str(cur_row, ':');
		char **array = split_str(cur_row, ':');

		if (array[0] != NULL && count_separator >= 5 && atoi(array[2]) == uid) {
			int setting = setpasswdfromarray(array, result, buffer, buflen);
			if (setting != 0) {
				if (setting == 1) {
					if(array) free(array);

					*errnop = ERANGE;
					ret = NSS_STATUS_TRYAGAIN;
				}
				else {
					ret = NSS_STATUS_UNAVAIL;
				}
				goto getpwuid_err;
			}

#ifdef DEBUG
			fprintf(stderr, "Found item for uid=%d: [name=%s, gid=%s]\n", uid, array[0], array[3]);
#endif

			ret = NSS_STATUS_SUCCESS;
			break;
		}

		if(array) free(array);
		cursor = cursor->next;
	}

getpwuid_err:
	cleanbody();
	return ret;
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
	enum nss_status ret = NSS_STATUS_UNAVAIL;

	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?passwd", url);
	if (!geturl(newurl, username, password, cafile, sslcheck) || body == NULL) {
		ret = NSS_STATUS_UNAVAIL;
		goto getpwent_err;
	}

	int i = 0;
	BODY *cursor = body;
	while (cursor) {
		if (i > last_rownum_pwd) {
			char *cur_row = cursor->row;
			int count_separator = count_char_in_str(cur_row, ':');
			char **array = split_str(cur_row, ':');

			if (array[0] != NULL && count_separator >= 6) {
				int setting = setpasswdfromarray(array, result, buffer, buflen);
				if (setting != 0) {
					if (setting == 1) {
						if(array) free(array);

						*errnop = ERANGE;
						ret = NSS_STATUS_TRYAGAIN;
					}
					else {
						ret = NSS_STATUS_UNAVAIL;
					}
					goto getpwent_err;
				}

#ifdef DEBUG
				fprintf(stderr, "Found item: [name=%s, uid=%s, gid=%s]\n", array[0], array[2], array[3]);
#endif

				last_rownum_pwd = i;
				ret = NSS_STATUS_SUCCESS;
			}

			if (array) free(array);
			if (ret == NSS_STATUS_SUCCESS) break;
		}

		i++;
		cursor = cursor->next;
	}

	if (ret != NSS_STATUS_SUCCESS) {
#ifdef DEBUG
				fprintf(stderr, "Item not found, end of file.\n");
#endif

		*errnop = ENOENT;
		ret = NSS_STATUS_NOTFOUND;
	}

getpwent_err:
	cleanbody();
	return ret;
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

	enum nss_status ret = NSS_STATUS_UNAVAIL;
	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?group", url);

	if (!geturl(newurl, username, password, cafile, sslcheck) || body == NULL) {
		ret = NSS_STATUS_UNAVAIL;
		goto getgrent_err;
	}

	int i = 0;
	BODY *cursor = body;
	while (cursor)
	{
		if (i > last_rownum_grp)
		{
			char *cur_row = cursor->row;
			int count_separator = count_char_in_str(cur_row, ':');
			char **array = split_str(cur_row, ':');

			if (array[0] != NULL && count_separator >= 3)
			{
				int setting = setgroupfromarray(array, result, buffer, buflen);
				if (setting != 0) {
					if (setting == 1) {
						if(array) free(array);

						*errnop = ERANGE;
						ret = NSS_STATUS_TRYAGAIN;
					}
					else {
						ret = NSS_STATUS_UNAVAIL;
					}
					goto getgrent_err;
				}

#ifdef DEBUG
				fprintf(stderr, "Found item: [grname=%s, gid=%s]\n", array[0], array[2]);
#endif

				last_rownum_grp = i;
				ret = NSS_STATUS_SUCCESS;
			}

			if (array) free(array);
			if (ret == NSS_STATUS_SUCCESS) break;
		}

		i++;
		cursor = cursor->next;
	} 

	if (ret != NSS_STATUS_SUCCESS) {
#ifdef DEBUG
				fprintf(stderr, "Item not found, end of file.\n");
#endif

		*errnop = ENOENT;
		ret = NSS_STATUS_NOTFOUND;
	}

getgrent_err:
	cleanbody(); 
	return ret;
}

enum nss_status _nss_shib_getgrnam_r(const char *name, struct group *result, char *buffer, size_t buflen, int *errnop)
{
#ifdef DEBUG
	fprintf(stderr, "\nEntering _nss_shib_getgrnam_r with grname=%s.\n", name);
#endif

	enum nss_status ret = NSS_STATUS_UNAVAIL;
	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?group", url);
	if (!geturl(newurl, username, password, cafile, sslcheck) || body == NULL) {
		ret = NSS_STATUS_UNAVAIL;
		goto getgrnam_err;
	}

	BODY *cursor = body;
	while (cursor)
	{
		char *cur_row = cursor->row;
		int count_separator = count_char_in_str(cur_row, ':');
		char **array = split_str(cur_row, ':');
		if (array[0] != NULL && count_separator >= 3 && strcmp(array[0], name) == 0) {
			int setting = setgroupfromarray(array, result, buffer, buflen);
			if (setting != 0) {
				if (setting == 1) {
					if(array) free(array);

					*errnop = ERANGE;
					ret = NSS_STATUS_TRYAGAIN;
				}
				else {
					ret = NSS_STATUS_UNAVAIL;
				}
				goto getgrnam_err;
			}

#ifdef DEBUG
			fprintf(stderr, "Found item for grname=%s: [gid=%s]\n", name, array[2]);
#endif

			ret = NSS_STATUS_SUCCESS;
		}

		if (array) free(array);
		cursor = cursor->next;
	}  

getgrnam_err:
	cleanbody();
	return ret;
}

enum nss_status _nss_shib_getgrgid_r(gid_t gid, struct group *result, char *buffer, size_t buflen, int *errnop)
{
#ifdef DEBUG
	fprintf(stderr, "\nEntering _nss_shib_getgrgid_r with gid=%d.\n", gid);
#endif

	int ret = NSS_STATUS_UNAVAIL;
	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?group", url);
	if (!geturl(newurl, username, password, cafile, sslcheck) || body == NULL) {
		ret = NSS_STATUS_UNAVAIL;
		goto getgrgid_err;
	}

	BODY *cursor = body;
	while (cursor)
	{
		char *cur_row = cursor->row;
		int count_separator = count_char_in_str(cur_row, ':');
		char **array = split_str(cur_row, ':');

		if (array[0] != NULL  && count_separator >= 3 && atoi(array[2]) == gid)
		{
			int setting = setgroupfromarray(array, result, buffer, buflen);
			if (setting != 0) {
				if (setting == 1) {
					if(array) free(array);

					*errnop = ERANGE;
					ret = NSS_STATUS_TRYAGAIN;
				}
				else {
					ret = NSS_STATUS_UNAVAIL;
				}
				goto getgrgid_err;
			}

#ifdef DEBUG
			fprintf(stderr, "Found item for gid=%d: [name=%s]\n", gid, array[0]);
#endif

			ret = NSS_STATUS_SUCCESS;
		}

		if(array) free(array);
		cursor = cursor->next;
	}  

getgrgid_err:
	cleanbody();
	return ret;
}

#ifdef DEBUG

#define GETPWNAM 1
#define GETPWUID 2
#define SETPWENT 3
#define GETPWENT 4
#define SETGRENT 5
#define ENDGRENT 6
#define GETGRENT 7
#define GETGRNAM 8
#define GETGRGID 9

static void printpasswd(struct passwd *result) {
	printf("Passwd with following characteristics:\n"
	       "  pw_name    => %s\n"
	       "  pw_passwd  => %s\n"
	       "  pw_uid     => %d\n"
	       "  pw_gid     => %d\n"
	       "  pw_gecos   => %s\n"
	       "  pw_dir     => %s\n"
	       "  pw_shell   => %s\n",
	       result->pw_name,
	       result->pw_passwd,
	       result->pw_uid,
	       result->pw_gid,
	       result->pw_gecos,
	       result->pw_dir,
	       result->pw_shell);
}

static void printgroup(struct group *result) {
	char members[1024];
	members[0] = '\0';
	
	int i = 0;
	char *curmem = result->gr_mem[i];
	while (1) {
		if (curmem == NULL) break;
		strcat(members, curmem);
		strcat(members, ";");
		curmem = result->gr_mem[++i];
	}

	printf("Group with following characteristics:\n"
	       "  gr_name    => %s\n"
	       "  gr_passwd  => %s\n"
	       "  gr_gid     => %d\n"
	       "  gr_members => %s\n",
	       result->gr_name,
	       result->gr_passwd,
	       result->gr_gid,
	       members);
}

static int test_function(int function, const char *param) {
	int ret = 0;
	int errnop;
	char buffer[1024];
	struct passwd *resultp = NULL;
	struct group *resultg = NULL;
	enum nss_status code = NSS_STATUS_UNAVAIL;

	switch(function) {
	case GETPWNAM:
		resultp = (struct passwd *) malloc(sizeof(struct passwd));
		code = _nss_shib_getpwnam_r(param, resultp, buffer, 1024, &errnop);
		break;
	case GETPWUID:
		resultp = (struct passwd *) malloc(sizeof(struct passwd));
		code = _nss_shib_getpwuid_r(atoi(param), resultp, buffer, 1024, &errnop);
		break;
	case SETPWENT:
		//code = _nss_shib_setpwent();
		break;
	case GETPWENT:
		resultp = (struct passwd *) malloc(sizeof(struct passwd));
		code = _nss_shib_getpwent_r(resultp, buffer, 1024, &errnop);
		break;
	case SETGRENT:
		//code = _nss_shib_setgrent_r(resultg, (void *)param);
		break;
	case ENDGRENT:
		//code = _nss_shib_endgrent_r(NULL, (void *)param);
		break;
	case GETGRENT:
		resultg = (struct group *) malloc(sizeof(struct group));
		code = _nss_shib_getgrent_r(resultg, buffer, 1024, &errnop);
		break;
	case GETGRNAM:
		resultg = (struct group *) malloc(sizeof(struct group));
		code = _nss_shib_getgrnam_r(param, resultg, buffer, 1024, &errnop);
		break;
	case GETGRGID:
		resultg = (struct group *) malloc(sizeof(struct group));
		code = _nss_shib_getgrgid_r(atoi(param), resultg, buffer, 1024, &errnop);
		break;
	}

	if (code != NSS_STATUS_SUCCESS) {
		printf("Error while calling function.\n");
		ret = 0;
	}
	else {
		if (resultp) printpasswd(resultp);
		if (resultg) printgroup(resultg);
		ret = 1;
	}

	buffer[0] = '\0';
	if (resultp) free(resultp);
	if (resultg) free(resultg);

	return ret;
}

int main(int argc, char *argv[]) {
	char param[128];
        if (argc < 2) {
                fprintf(stderr, "Call specifying function and (optionally) a parameter.\n"
		                "Valid function names are:\n"
		                "- getpwnam\n"
		                "- getpwuid\n"
		                "- setpwent\n"
		                "- getpwent\n"
		                "- setgrent\n"
		                "- endgrent\n"
		                "- getgrent\n"
		                "- getgrnam\n"
		                "- getgrgid\n\n"
		                "  Example:\n   %s getpwnam username\n", argv[0]);
                return 1;
        }
	
	if (strcmp(argv[1], "getpwnam") == 0) {
		printf("\nTesting function _nss_shib_getpwnam_r:\n");
		if (argc >= 3) strcpy(param, argv[2]);
		else param[0] = '\0';
		test_function(GETPWNAM, param);
	}

	if (strcmp(argv[1], "getpwuid") == 0) {
		printf("\nTesting function _nss_shib_getpwuid_r:\n");
		if (argc >= 3) strcpy(param, argv[2]);
		else param[0] = '\0';
		test_function(GETPWUID, param);
	}

	if (strcmp(argv[1], "setpwent") == 0) {
		printf("\nTesting function _nss_shib_setpwent:\n");
		test_function(SETPWENT, NULL);
	}

	if (strcmp(argv[1], "getpwent") == 0) {
		printf("\nTesting function _nss_shib_getpwent_r:\n");
		while (test_function(GETPWENT, NULL)) {}
	}

	if (strcmp(argv[1], "setgrent") == 0) {
		printf("\nTesting function _nss_shib_setgrent_r:\n");
		test_function(SETGRENT, NULL);
	}

	if (strcmp(argv[1], "endgrent") == 0) {
		printf("\nTesting function _nss_shib_endgrent_r:\n");
		test_function(ENDGRENT, NULL);
	}

	if (strcmp(argv[1], "getgrent") == 0) {
		printf("\nTesting function _nss_shib_getgrent_r:\n");
		while (test_function(GETGRENT, NULL)) {}
	}

	if (strcmp(argv[1], "getgrnam") == 0) {
		printf("\nTesting function _nss_shib_getgrnam_r:\n");
		if (argc >= 3) strcpy(param, argv[2]);
		else param[0] = '\0';
		test_function(GETGRNAM, param);
	}

	if (strcmp(argv[1], "getgrid") == 0) {
		printf("\nTesting function _nss_shib_getgrgid_r:\n");
		if (argc >= 3) strcpy(param, argv[2]);
		else param[0] = '\0';
		test_function(GETGRGID, param);
	}

	return 0;
}

#endif
