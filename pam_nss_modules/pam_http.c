#define _GNU_SOURCE
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>

#define PAM_SM_AUTH
#define PAM_SM_PASSWORD
#define PAM_SM_ACCOUNT
#define PAM_SM_SESSION
#include <security/pam_modules.h>

#include "netcurl.h"

typedef struct _SESSION {
	char *key;
	char *value;
	struct _SESSION *next;
} SESSION;

SESSION *session = NULL;

static char *getsessvalue(const char *key) {
	SESSION *cursor = session;
	while (cursor) {
		if (!strcasecmp(cursor->key, key)) {
			return cursor->value;
		}
		cursor = cursor->next;
	}

	return NULL;
}

void cleanbody() {
	while (session) {
		SESSION *next = session->next;
		free(session);
		session = next;
	}
}

size_t bodycallback(char *ptr, size_t size, size_t nmemb, void *userdata) {
	size_t ret = size*nmemb;
	char* pstr = (char *)malloc(size*nmemb+1);

	if(!pstr) return 0;
	strncpy(pstr, ptr, size*nmemb);
	pstr[size*nmemb] = '\0';

	int i = 0;
	char **rows = split_str(pstr, '\n');

	/* impossible, for this to happen curl should
	   invoke this function with ptr == NULL */  
	if (rows == NULL || rows[0] == NULL) {
		ret = 0;
		goto on_err;
	}

	for (i = 0; rows[i]; i++) {
		char *peq = NULL;
		replace_char(rows[i], '\r', '\0');
		if (NULL != (peq = strchr(rows[i], '='))) {
			char *key = rows[i],
			     *value = peq + 1;
			*peq = '\0';

#ifdef DEBUG
			fprintf(stderr, "Read session value: [%s] => %s\n", key, value);
#endif

			SESSION *cursess = (SESSION *) malloc(sizeof(SESSION) + strlen(key) + strlen(value) + 2);
			cursess->key = (char *) ((char *)cursess + sizeof(SESSION));
			cursess->value = (char *) ((char *)cursess + sizeof(SESSION) + strlen(key) + 1);
			strcpy(cursess->key, key);
			strcpy(cursess->value, value);
			cursess->next = session;
			session = cursess;
		}
	}

on_err:
	if(rows) free(rows);
	if(pstr) free(pstr);

	return ret;
}

/* pam arguments are normally of the form name=value.  This gets the
 * 'value' corresponding to the passed 'name' from the argument list. */
static const char *getarg(const char *name, int argc, const char **argv) {
	int len = strlen(name);
	while (argc) {
		if (strlen(*argv) > len && !strncmp(name, *argv, len) && (*argv)[len] == '=') {
			return *argv + len + 1;  /* 1 for the = */
		}
		argc--;
		argv++;
	}
	return NULL;
}

const char *get_password(pam_handle_t *pamh, const char *username, int flags, int use_first_pass, int try_first_pass) {
	int rc = 0;
	char *password = NULL;
	struct pam_message msg;
	const struct pam_message *msgp;
	struct pam_response *respp;
	struct pam_conv *item;
	char message[128];

	rc = pam_get_item(pamh, PAM_AUTHTOK, (const void **) &password);
	if (rc == PAM_SUCCESS && (use_first_pass || try_first_pass)) return password;

	if ((flags & PAM_SILENT) == PAM_SILENT) {
		syslog(LOG_ERR, "PAM_SILENT enabled but password requested, unable to continue.\n");
		return NULL;
	}

	if (pam_get_item(pamh, PAM_CONV, (const void**)&item) != PAM_SUCCESS) {
		syslog(LOG_ERR, "Couldn't get pam_conv.\n");
		return NULL;
	}

	msgp = &msg;
	snprintf(message, 128, "%s's password: ", username);

	msg.msg_style = PAM_PROMPT_ECHO_OFF;
	msg.msg = message;

	if (item->conv(1, &msgp, &respp, item->appdata_ptr)) {
		password = NULL;
	}
	else {
		password = respp[0].resp;
	}

	if (respp) free(respp);
	return password;
}

PAM_EXTERN int pam_sm_authenticate(pam_handle_t *pamh, int flags, int argc, const char **argv) {
	const char *username = NULL;
	const char *password = NULL;
	const char *cafile = getarg("cafile", argc, argv);
	const char *sslcheck = getarg("sslcheck", argc, argv);
	const char *sess_username = getarg("sess_username", argc, argv);
	const char *url = getarg("url", argc, argv);
	int use_first_pass = 0, try_first_pass = 0;
	int rv = PAM_AUTH_ERR;
	int i = 0;

	if (!url) return PAM_AUTH_ERR;
	if (NULL != strstr(argv[i], "use_first_pass")) use_first_pass = 1;
	if (NULL != strstr(argv[i], "try_first_pass")) try_first_pass = 1;

#ifdef DEBUG
	fprintf(stderr, "\nCalling auth function with cafile=%s and sslcheck=%s.\n", cafile, sslcheck);
#endif

	if (pam_get_user(pamh, &username, 0) != PAM_SUCCESS) {
		syslog(LOG_ERR, "Couldn't get username.\n");
		return PAM_AUTH_ERR;
	}

	password = get_password(pamh, username, flags, use_first_pass, try_first_pass);
	if (password == NULL) {
		syslog(LOG_ERR, "Couldn't get password.\n");
		return PAM_AUTH_ERR;
	}
	
	if (geturl(url, (char*)username, password, cafile, sslcheck)) {
		rv = PAM_SUCCESS;
		
		if (sess_username != NULL) {
			char *uname = getsessvalue(sess_username);
			pam_set_item(pamh, PAM_USER, uname);
		}
	}

	if (use_first_pass == 0 && try_first_pass == 0) {
		memset((void*)password, '\0', strlen(password));
		free((void*)password);
	}

	return rv;
}

PAM_EXTERN int pam_sm_setcred(pam_handle_t *pamh, int flags, int argc, const char **argv) {
#ifdef DEBUG
	fprintf(stderr, "\nSetting values in user session.\n");
#endif

	char sess_value[1024];
	SESSION *cursor = session;
	while (cursor) {
		replace_char(cursor->key, '-', '_');
		sprintf(sess_value, "%s=%s", cursor->key, cursor->value);
		setenv(cursor->key, cursor->value, 1);

		if (pam_putenv(pamh, sess_value) != PAM_SUCCESS) {
#ifdef DEBUG
			fprintf(stderr, "Error setting environment variable in user session.\n");
#endif

			return PAM_AUTH_ERR;    
		}

		cursor = cursor->next;
	}

	return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_acct_mgmt(pam_handle_t *pamh, int flags, int argc, const char **argv) {
	const char *key = "authenticated";
	const char *value = "true";

#ifdef DEBUG
	fprintf(stderr, "\nChecking authetication rights in session.\n");
#endif

	char *authed = getsessvalue(key);
	if (authed != NULL && !strcasecmp(authed, value)) return PAM_SUCCESS;
	return PAM_AUTH_ERR;
}

PAM_EXTERN int pam_sm_open_session(pam_handle_t *pamh, int flags, int argc, const char **argv) {
#ifdef DEBUG
	fprintf(stderr, "\nOpening PAM session.\n");
#endif

	return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_close_session(pam_handle_t *pamh, int flags, int argc, const char **argv) {
#ifdef DEBUG
	if (flags != PAM_SILENT) fprintf(stderr, "\nClosing PAM session.\n");
#endif

	cleanbody();
	return PAM_SUCCESS;
}

PAM_EXTERN int pam_sm_chauthtok(pam_handle_t *pamh, int flags, int argc, const char **argv) {
	const char *message = "Unable to set user credentials on Shibboleth. Please contacy the IdP administrator for this task.\n";
	syslog(LOG_ERR, "%s", message);

	if ((flags & PAM_SILENT) != PAM_SILENT) {
		struct pam_conv *item;
		struct pam_message msg;
		const struct pam_message *msgp;

		if (pam_get_item(pamh, PAM_CONV, (const void**)&item) != PAM_SUCCESS) {
			syslog(LOG_ERR, "Couldn't get pam_conv\n");
			return PAM_AUTH_ERR;
		}

		msgp = &msg;
		msg.msg_style = PAM_ERROR_MSG;
		msg.msg = message;
		item->conv(1, &msgp, NULL, item->appdata_ptr);
	}

	return PAM_PERM_DENIED;
}


#ifdef PAM_STATIC

struct pam_module _pam_http_modstruct = {
	"pam_http",
	pam_sm_authenticate,
	pam_sm_setcred,
	pam_sm_acct_mgmt,
	pam_sm_open_session,
	pam_sm_close_session,
	pam_sm_chauthtok
};

#endif

#ifdef DEBUG

int main(int argc, char *argv[]) {
	int ritorno = 1;

	if (argc < 3) {
		fprintf(stderr, "Call specifying username and passowrd.\nExample:\n   %s username password\n", argv[0]);
		return ritorno;
	}

	const char *username = argv[1];
	const char *password = argv[2];
	const char *cafile = NULL;
	const char *sslcheck = "false";
	const char *sess_username = "uid";
	const char *url = "https://cloud-mi-03.mib.infn.it/secure/pam.php";

	fprintf(stdout, "\nCalling auth function with cafile=%s and sslcheck=%s.\n", cafile, sslcheck);

	if (geturl(url, (char*)username, password, cafile, sslcheck)) {
		if (sess_username != NULL) {
			char *uname = getsessvalue(sess_username);
			if (uname) fprintf(stdout, "Authenticated user %s.\n", uname);
			else fprintf(stdout, "Authenticated user %s (not found sess_username=%s).\n", username, sess_username);
			ritorno = 0;
		}
	}

	return ritorno;
}

#endif
