#include <nss.h>
#include <grp.h>
#include <stdio.h>
#include <stdlib.h>
#define BUFLEN 4096

int main(void)
{

	//geturl("https://cloud-mi-03.mib.infn.it/secure/pam.php", "andrea", "ciaoandrea", NULL, "false");
        //free_cookies();
        //cleanbody();
        //return 0;


	struct group grp, *grpp;
	char buf[BUFLEN];
	int i;

	setgrent();
	while (1) {
		//i = getgrent_r(&grp, buf, BUFLEN, &grpp);
		i = _nss_shib_getgrent_r(&grp, buf, BUFLEN, &grpp); 
		if (i != NSS_STATUS_SUCCESS) break;
		printf("%s (%d):", grpp->gr_name, grpp->gr_gid);
		for (i = 0; ; i++) {
			if (grpp->gr_mem[i] == NULL) break;
			printf(" %s", grpp->gr_mem[i]);
		}
		printf("\n");
	}
	endgrent();
	exit(EXIT_SUCCESS);
}

