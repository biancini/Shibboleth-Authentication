CFLAGS += -Werror -Wall
all: check_user pam_http.so libnss_shib.so.2

clean:
	$(RM) check_user pam_http.so *.o libnss_shib.so.2
pam_http.so: pam_http.c
	$(CC) $(CFLAGS) -fPIC -shared -Xlinker -x -o $@ $< -lcurl
check_user: check_user.c
	$(CC) $(CFLAGS) -o $@ $< -lpam -lpam_misc
libnss_shib.so.2: libnss_shib.c
	$(CC) $(CFLAGS) -shared -Wl,-soname,libnss_shib.so.2 -o $@ $<

install:
	cp pam_http.so /lib64/security/pam_http.so
	cp pam_http.so /lib/security/pam_http.so
	cp libnss_shib.so.2 /lib/libnss_shib.so.2
	cp libnss_shib.so.2 /lib64/libnss_shib.so.2
