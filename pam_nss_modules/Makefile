CFLAGS += -Werror -Wall
ARCH := $(shell getconf LONG_BIT)
LIB_32 := $(shell echo "/lib")
LIB_64 := $(shell echo "/lib64")

all: check_user pam_http.so libnss_shib.so.2

clean:
	$(RM) check_user pam_http.so *.o libnss_shib.so.2
pam_http.so: pam_http.c netcurl.c
	$(CC) $(CFLAGS) -fPIC -shared -Xlinker -x -o $@ pam_http.c netcurl.c -lcurl
check_user: check_user.c
	$(CC) $(CFLAGS) -o $@ $< -lpam -lpam_misc
libnss_shib.so.2: libnss_shib.c netcurl.c
	$(CC) $(CFLAGS) -fPIC -shared -Wl,-soname,libnss_shib.so.2 -Xlinker -x -o $@ libnss_shib.c netcurl.c -lcurl -lconfig

install:
	cp pam_http.so $(LIB_$(ARCH))/security/pam_http.so
	cp libnss_shib.so.2 $(LIB_$(ARCH))/libnss_shib.so.2
