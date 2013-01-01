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

void cleanbody()
{
	while(body)
	{
		BODY *next = body->next;
		free(body);
		body = next;
	}
}

static void readconfig_value(config_t *cfg, const char *valname, const char **value)
{
	if (*value == NULL)
	{
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

static void readconfig()
{
	const char	*val = NULL;
#ifdef DEBUG
	fprintf(stderr, "Reading configuration file...");
#endif

	config_t cfg;

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

	readconfig_value(&cfg, "url", &url);
	readconfig_value(&cfg, "cafile", &cafile);
	readconfig_value(&cfg, "sslcheck", &sslcheck);
	readconfig_value(&cfg, "username", &username);
	readconfig_value(&cfg, "password", &password);

	if (get_cookies() == NULL)
	{
		int		i = 0;
		regex_t	envVar;
		readconfig_value(&cfg, "cookie_num", &val);
		if (val == NULL) goto end_config;
		int num_cookies = atoi(val);

		if(regcomp(&envVar, "\\$\\{[a-zA-Z0-9_-]+\\}", REG_EXTENDED))
			goto end_config;

		for(i=1; i <= num_cookies; ++i)
		{
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
			while(!regexec(&envVar, tmpVal, 1, &match, 0))
			{
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
			if(!cookie_name)
				goto on_gen_err;
			/* do the same for cookie_value */
			tmpVal = cookie_value;
			tmpBuf[0] = '\0';
			while(!regexec(&envVar, tmpVal, 1, &match, 0))
			{
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
			if(!cookie_value)
				goto on_gen_err;
			/* add to cookies */
			struct curl_slist	*curcookie = (struct curl_slist *) malloc(sizeof(struct curl_slist));
			if(!curcookie)
				goto on_gen_err;
			curcookie->data = (char *) malloc(strlen(cookie_name)+strlen(cookie_value)+2);
			if(!curcookie->data)
			{
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

size_t bodycallback(char *ptr, size_t size, size_t nmemb, void *userdata)
{
	char* pstr = (char *)ptr;

	replace_char(pstr, '\r', ':');
	char **rows = split_str(pstr, "\n");
	if (rows == NULL || rows[0] == NULL)
	{
		free(rows);
		return nmemb*size;
	}

	int i = 0;
	for (i = 0; rows[i]; i++)
	{
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

enum nss_status _nss_shib_getpwnam_r(const char *name, struct passwd *result, char *buffer, size_t buflen, int *errnop)
{
#ifdef DEBUG
	fprintf(stderr, "\nEntering _nss_shib_getpwnam_r with name=%s.\n", name);
#endif

	int found = 0;

	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?passwd", url);
	if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
	if (body == NULL) return NSS_STATUS_UNAVAIL;

	BODY *cursor = body;
	while (cursor)
	{
		char *cur_row = strdup(cursor->row);
		int count_separator = count_char_in_str(cur_row, ':');
		char **array = split_str(cur_row, ":");

		if (array[0] != NULL && count_separator >= 5 && strcmp(array[0], name) == 0)
		{
			size_t len = strlen(array[0]) + strlen(array[1]) + strlen(array[4]) + strlen(array[5]) + strlen(array[6]) + 5;
			if (len > buflen)
			{
				if(array) free(array);
				if(cur_row) free(cur_row);

				*errnop = ERANGE;
				return NSS_STATUS_TRYAGAIN;
			}

			char *buf_ptr = buffer;
			memcpy(buf_ptr, array[0], strlen(array[0])+1);
			result->pw_name = buf_ptr;
			buf_ptr += strlen(array[0])+1;
			memcpy(buf_ptr, array[1], strlen(array[1])+1);
			result->pw_passwd = buf_ptr;
			buf_ptr += strlen(array[1])+1;
			memcpy(buf_ptr, array[4], strlen(array[4])+1);
			result->pw_gecos = buf_ptr;
			buf_ptr += strlen(array[4])+1;
			memcpy(buf_ptr, array[5], strlen(array[5])+1);
			result->pw_dir = buf_ptr;
			buf_ptr += strlen(array[5])+1;
			memcpy(buf_ptr, array[6], strlen(array[6])+1);
			result->pw_shell = buf_ptr;
			buf_ptr += strlen(array[6])+1;

			result->pw_uid = atoi(array[2]);
			result->pw_gid = atoi(array[3]);

#ifdef DEBUG
			fprintf(stderr, "Found item for name=%s: [uid=%s, gid=%s]\n", name, array[2], array[3]);
#endif

			found = 1;
		}

		if(array) free(array);
		if(cur_row) free(cur_row);
		if(found)
			break;
		cursor = cursor->next;
	}

	cleanbody();
	return (found == 1) ? NSS_STATUS_SUCCESS : NSS_STATUS_NOTFOUND;
}

enum nss_status _nss_shib_getpwuid_r(uid_t uid, struct passwd *result, char *buffer, size_t buflen, int *errnop)
{
#ifdef DEBUG
	fprintf(stderr, "\nEntering _nss_shib_getpwuid_r with uid=%d.\n", uid);
#endif

	int found = 0;

	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?passwd", url);
	if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
	if (body == NULL) return NSS_STATUS_UNAVAIL;

	BODY *cursor = body;
	while (cursor)
	{
		char *cur_row = strdup(cursor->row);
		int count_separator = count_char_in_str(cur_row, ':');
		char **array = split_str(cur_row, ":");

		if (array[0] != NULL && count_separator >= 5 && atoi(array[2]) == uid)
		{
			size_t len = strlen(array[0]) + strlen(array[1]) + strlen(array[4]) + strlen(array[5]) + strlen(array[6]) + 5;
			if (len > buflen)
			{
				if(array) free(array);
				if(cur_row) free(cur_row);

				*errnop = ERANGE;
				return NSS_STATUS_TRYAGAIN;
			}

			char *buf_ptr = buffer;
			memcpy(buf_ptr, array[0], strlen(array[0])+1);
			result->pw_name = buf_ptr;
			buf_ptr += strlen(array[0])+1;
			memcpy(buf_ptr, array[1], strlen(array[1])+1);
			result->pw_passwd = buf_ptr;
			buf_ptr += strlen(array[1])+1;
			memcpy(buf_ptr, array[4], strlen(array[4])+1);
			result->pw_gecos = buf_ptr;
			buf_ptr += strlen(array[4])+1;
			memcpy(buf_ptr, array[5], strlen(array[5])+1);
			result->pw_dir = buf_ptr;
			buf_ptr += strlen(array[5])+1;
			memcpy(buf_ptr, array[6], strlen(array[6])+1);
			result->pw_shell = buf_ptr;
			buf_ptr += strlen(array[6])+1;

			result->pw_uid = atoi(array[2]);
			result->pw_gid = atoi(array[3]);

#ifdef DEBUG
			fprintf(stderr, "Found item for uid=%d: [name=%s, gid=%s]\n", uid, array[0], array[3]);
#endif

			found = 1;
		}

		if(array) free(array);
		if(cur_row) free(cur_row);
		if(found)
			break;
		cursor = cursor->next;
	}

	cleanbody();
	return (found == 1) ? NSS_STATUS_SUCCESS : NSS_STATUS_NOTFOUND;
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
			int count_separator = count_char_in_str(cur_row, ':');
			char **array = split_str(cur_row, ":");
			if (array[0] != NULL && count_separator >= 6)
			{
				result->pw_name = (char *)malloc(strlen(array[0])+1);
				strcpy(result->pw_name, array[0]);
				result->pw_passwd = (char *)malloc(strlen(array[1])+1);
				strcpy(result->pw_passwd, array[1]);
				result->pw_uid = atoi(array[2]);
				result->pw_gid = atoi(array[3]);
				result->pw_gecos = (char *)malloc(strlen(array[4])+1);
				strcpy(result->pw_gecos, array[4]);
				result->pw_dir = (char *)malloc(strlen(array[5])+1);
				strcpy(result->pw_dir, array[5]);
				result->pw_shell = (char *)malloc(strlen(array[6])+1);
				strcpy(result->pw_shell, array[6]);

#ifdef DEBUG
				fprintf(stderr, "Found item: [name=%s, uid=%s, gid=%s]\n", array[0], array[2], array[3]);
#endif

				free(array);
				cleanbody();
				last_rownum_pwd = i;
				return NSS_STATUS_SUCCESS;
			}

			free(array);
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
			int count_separator = count_char_in_str(cur_row, ':');
			char **array = split_str(cur_row, ":");
			if (array[0] != NULL && count_separator >= 3)
			{
				result->gr_name = (char *) malloc(strlen(array[0])+1);
				strcpy(result->gr_name, array[0]);
				result->gr_passwd = (char *) malloc(strlen(array[1])+1);
				strcpy(result->gr_passwd, array[1]);
				result->gr_gid = atoi(array[2]);
				result->gr_mem = (char **) malloc(sizeof(char **));

				int j = 0;
				char **members = split_str(array[3], ",");
				for (j = 0; ; j++)
				{
					result->gr_mem = (char **) realloc(result->gr_mem, (j+1)*sizeof(char *));
					if (members == NULL || members[j] == NULL)
					{
						result->gr_mem[j] = NULL;
						break;
					}
					result->gr_mem[j] = malloc(strlen(members[j])+1);
					strcpy(result->gr_mem[j], members[j]);
				}

#ifdef DEBUG
				fprintf(stderr, "Found item: [grname=%s, gid=%s]\n", array[0], array[2]);
#endif

				free(array);
				cleanbody();
				last_rownum_grp = i;
				return NSS_STATUS_SUCCESS;
			}

			free(array);
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
		int count_separator = count_char_in_str(cur_row, ':');
		char **array = split_str(cur_row, ":");
		if (array[0] != NULL && count_separator >= 3)
		{
			if (strcmp(array[0], name) == 0)
			{
				result->gr_name = (char *) malloc(strlen(array[0])+1);
				strcpy(result->gr_name, array[0]);
				result->gr_passwd = (char *) malloc(strlen(array[1])+1);
				strcpy(result->gr_passwd, array[1]);
				result->gr_gid = atoi(array[2]);
				result->gr_mem = (char **) malloc(sizeof(char **));

				int j = 0;
				char **members = split_str(array[3], ",");
				for (j = 0; ;j++)
				{
					result->gr_mem = (char **) realloc(result->gr_mem, (j+1)*sizeof(char *));
					if (members == NULL || members[j] == NULL)
					{
						result->gr_mem[j] = NULL;
						break;
					}
					result->gr_mem[j] = malloc(strlen(members[j])+1);
					strcpy(result->gr_mem[j], members[j]);
				}

#ifdef DEBUG
				fprintf(stderr, "Found item for grname=%s: [gid=%s]\n", name, array[2]);
#endif

				free(array);
				cleanbody();
				return NSS_STATUS_SUCCESS;
			}
		}

		free(array);
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

	int found = 0;
	readconfig();
	char newurl[1024];
	sprintf(newurl, "%s?group", url);
	if (!geturl(newurl, username, password, cafile, sslcheck)) return NSS_STATUS_UNAVAIL;
	if (body == NULL) return NSS_STATUS_UNAVAIL;

	BODY *cursor = body;
	while (cursor)
	{
		char *cur_row = strdup(cursor->row);
		int count_separator = count_char_in_str(cur_row, ':');
		char **array = split_str(cur_row, ":");

		if (array[0] != NULL  && count_separator >= 3 && atoi(array[2]) == gid)
		{
			size_t len = strlen(array[0]) + strlen(array[1]) + strlen(array[3]) + 3;
			int num_members = count_char_in_str(array[3], ',') + 1;
			len += sizeof(char *)*num_members;

			if (len > buflen)
			{
				if(array) free(array);
				if(cur_row) free(cur_row);

				*errnop = ERANGE;
				return NSS_STATUS_TRYAGAIN;
			}

			char *buf_ptr = buffer;
			memcpy(buf_ptr, array[0], strlen(array[0])+1);
			result->gr_name = buf_ptr;
			buf_ptr += strlen(array[0])+1;
			memcpy(buf_ptr, array[1], strlen(array[1])+1);
			result->gr_passwd = buf_ptr;
			buf_ptr += strlen(array[1])+1;

			result->gr_gid = atoi(array[2]);

			char *ptr[128];
			int j = 0;
			char **members = split_str(array[3], ",");

			for (j = 0; ; j++)
			{
				if (members == NULL || members[j] == NULL)
				{
					ptr[j] = NULL;
					break;
				}
				memcpy(buf_ptr, members[j], strlen(members[j])+1);
				buf_ptr += strlen(members[j])+1;
			}

			memcpy(buf_ptr, ptr, sizeof(char *)*j);
			result->gr_mem = (char **)buf_ptr;

#ifdef DEBUG
			fprintf(stderr, "Found item for gid=%d: [name=%s]\n", gid, array[0]);
#endif

			cleanbody();
			found = 1;
		}

		if(cur_row) free(cur_row);
		if(array) free(array);
		if(found)
			break;
		cursor = cursor->next;
	}  

	cleanbody();
	return (found == 1) ? NSS_STATUS_SUCCESS : NSS_STATUS_NOTFOUND;
}
