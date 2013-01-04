#
# Regular cron jobs for the shibauth package
#
0 4	* * *	root	[ -x /usr/bin/shibauth_maintenance ] && /usr/bin/shibauth_maintenance
