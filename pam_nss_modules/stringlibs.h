#ifndef _STRINGLIBS_H_
#define _STRINGLIBS_H_

int count_char_in_str(char *str, char find);
char *replace_char(char *str, char orig, char rep);
char *replace_str(const char *str,const char *orig,const char *rep);
char *extract_str(char *str, const char start, const char end);
char **split_str(char *str, const char delimiter);

#endif /*_STRINGLIBS_H_*/

