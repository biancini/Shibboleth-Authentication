CFLAGS += -fPIC -Wall -lpthread -ldl -lutil -lm
ARCH := $(shell getconf LONG_BIT)
LIB_32 := $(shell echo "/lib/i386-linux-gnu")
LIB_64 := $(shell echo "/lib64/i386-linux-gnu")

ifeq ($(MECH),python)
	MECHFLAGS_C := $(shell /usr/bin/python-config --cflags)
	MECHFLAGS_L := $(shell /usr/bin/python-config --ldflags)

	MECHANISM := mechanize.c
else
	MECHFLAGS_C :=
	MECHFLAGS_L := -lcurl 

	MECHANISM := netcurl.c
endif
	
#MECHANISM := mechanize.c

# release target is the default one and it checks that the DEBUG variable is NOT set before executing all target
release: CFLAGS += -DNDEBUG
release: all

# debug taregt is used for debug reasons and checks that the DEBUG variable is set before executing all target
debug: CFLAGS += -DDEBUG -g
debug: all
	$(CC) $(CFLAGS) $(MECHFLAGS_C) -o nss_shib_test libnss_shib.c $(MECHANISM) stringlibs.c -lconfig $(MECHFLAGS_L)

.PHONY: release debug all clean install

# targets to build sources
all: check_user libnss_shib.so.2

clean:
	$(RM) check_user libnss_shib.so.2 nss_shib_test

check_user: check_user.c
	$(CC) $(CFLAGS) -o $@ check_user.c stringlibs.c -lpam -lpam_misc -lssl

check_user_ws: check_user_ws.c soapC.c soapClient.c
	$(CC) $(CFLAGS) -o $@ check_user_ws.c stringlibs.c soapC.c soapClient.c -lpam -lpam_misc -lssl -lgsoapssl -lgsoapck

libnss_shib.so.2: libnss_shib.c stringlibs.c $(MECHANISM)
	$(CC) $(CFLAGS) $(MECHFLAGS_C) -shared -Wl,-soname,$@ -o $@ libnss_shib.c $(MECHANISM) stringlibs.c -lconfig $(MECHFLAGS_L)

install:
	cp libnss_shib.so.2 $(LIB_$(ARCH))/libnss_shib.so.2
	cp pam_shibboleth.py /lib/security/pam_shibboleth.py
