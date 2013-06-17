#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <syslog.h>
#include <dlfcn.h>

#include <Python.h>
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

char *loadfile(char *file, int *size) {
        FILE *fp = NULL;
        long lSize = 0L;
        char *buffer = NULL;

        fp = fopen (file , "rb");
        if (!fp) {
                perror(file);
                return NULL;
        }

        fseek( fp , 0L , SEEK_END);
        lSize = ftell(fp);
        rewind( fp );

        buffer = calloc(1, lSize+1);
        if (!buffer) {
                fclose(fp);
                fputs("memory alloc fails", stderr);
                return NULL;
        }

        if (fread( buffer, lSize, 1, fp) != 1) {
                fclose(fp);
                free(buffer);
                fputs("entire read fails", stderr);
                return NULL;
        }

        size = (int *) lSize;
        fclose(fp);

        return buffer;
}

int geturl(const char *url, const char *username, const char *password, const char *cafile, const char *sslcheck) {
	PyObject *pModule = NULL, *pFunc = NULL;
        PyObject *pArgs = NULL, *pValue = NULL;
        int size = 0;

        char *pyscript = "/root/tests/prova.py",
             *funcname[] = {"geturl", "getrow"};

	dlopen("libpython2.7.so", RTLD_LAZY | RTLD_GLOBAL);
        Py_Initialize();
        pModule = PyImport_AddModule("__main__");
        char *buffer = loadfile(pyscript, &size);
        PyRun_SimpleString(buffer);

        if (pModule != NULL) {
                pFunc = PyObject_GetAttrString(pModule, funcname[0]);

                pArgs = PyTuple_New(3);
                pValue = PyString_FromString(url);
                PyTuple_SetItem(pArgs, 0, pValue);
                pValue = PyString_FromString(username);
                PyTuple_SetItem(pArgs, 1, pValue);
                pValue = PyString_FromString(password);
                PyTuple_SetItem(pArgs, 2, pValue);

                pValue = PyObject_CallObject(pFunc, pArgs);
                Py_DECREF(pArgs);
                if (pValue != NULL && PyInt_AsLong(pValue) == 0) {
                        pFunc = PyObject_GetAttrString(pModule, funcname[1]);

                        char *retval = NULL;
                        do {
                                pValue = PyObject_CallObject(pFunc, PyTuple_New(0));
                                if (pValue != NULL) {
                                        retval = PyString_AsString(pValue);
                                        if (retval != NULL && strlen(retval) > 0) {
#ifdef DEBUG
						fprintf(stderr, "Result of call: %s\n", retval);
#endif

						bodycallback(retval, sizeof(char), strlen(retval), NULL);
					}
                                }
                        } while (pValue != NULL && retval != NULL && strlen(retval) > 0);

                        Py_DECREF(pValue);
                }
                else {
                        Py_DECREF(pFunc);
                        //Py_DECREF(pModule);
                        PyErr_Print();
                        return -1;
                }
	} else {
                PyErr_Print();
                return -1;
        }

        Py_Finalize();
	return 1;
}

