#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>

#include "stringlibs.h"

int count_char_in_str(char *str, char find) {
	int count = 0;
	int i = 0;

	if (str == NULL) return count;
	for (i = 0; str[i]; i++) {
		if (str[i] == find) count++;
	}

	return count;
}

char *replace_char(char *str, char orig, char rep) {
	int i = 0;
	for (i = 0; str[i]; i++) {
		if (str[i] == orig) str[i] = rep;
	}

	return str;
}

char *replace_str(const char *str, const char *find, const char *rep) {
	int 		count = 0;
	const char 	*ins = str,
			*tmp = NULL;
	char		*ret = NULL;
	const size_t	len_find = strlen(find),
			len_rep = strlen(rep);

	for (count = 0; 0 != (tmp = strstr(ins, find)); ++count)
		ins = tmp + len_find;

	char *tmp2 = ret = (char*) malloc(strlen(str) + (len_rep - len_find) * count + 1);
	if (!ret) return NULL;

	while (count--) {
		ins = strstr(str, find);
		const size_t cp_size = ins - str;
		tmp2 = strncpy(tmp2, str, cp_size) + cp_size;
		tmp2 = strcpy(tmp2, rep) + len_rep;
		str += cp_size + len_rep;
	}
	strcpy(tmp2, str);

	return ret;
}

char *extract_str(char *str, const char start, const char end) {
	int len = 0;
	int write = -1;
	int i = 0;
	int j = 0;

	for (i = 0; str[i]; i++) {
		if (write == -1 && i > 0 && str[i-1] == start) write = 0;
		if (write == 0 && str[i] == end) write = 1;
		if (write == 0) len++;
	}

	char *newstr = (char *)malloc(len+1);
	write = -1;
	for (i = 0; str[i]; i++) {
		if (write == -1 && i > 0 && str[i-1] == start) write = 0;
		if (write == 0 && str[i] == end) write = 1;
		if (write == 0) newstr[j++] = str[i];
	}
	newstr[j] = '\0';

	return newstr;
}

char **split_str(char *str, const char delimiter) {
	int num_elems = count_char_in_str(str, delimiter);
	if (num_elems == 0) num_elems = 1;
	else num_elems += 2;

	char **tokenArray = (char **) malloc(sizeof(char *) * num_elems);
	int count = 1;
	int i = 0;

	tokenArray[0] = NULL;
	if (str == NULL) return tokenArray;
	tokenArray[0] = &str[0];
	tokenArray[1] = NULL;

	for (i = 0; str[i]; i++) {
		if (str[i] == delimiter) {
			str[i] = '\0';

			if (str[i+1]) {
				tokenArray[count] = &str[i+1];
				tokenArray[count+1] = NULL;
				count++;
			}
		}
	}

	return tokenArray;
}
