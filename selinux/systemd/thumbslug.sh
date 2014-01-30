#!/bin/sh -e
# local install for thumbslug policy (if you're doing dev work or whatever)


DIRNAME=`dirname $0`
cd $DIRNAME
USAGE="$0 [ --update ]"
if [ `id -u` != 0 ]; then
echo 'You must be root to run this script'
exit 1
fi

if [ $# -eq 1 ]; then
	if [ "$1" = "--update" ] ; then
		time=`ls -l --time-style="+%x %X" thumbslug.te | awk '{ printf "%s %s", $6, $7 }'`
		rules=`ausearch --start $time -m avc --raw -se thumbslug`
		if [ x"$rules" != "x" ] ; then
			echo "Found avc's to update policy with"
			echo -e "$rules" | audit2allow -R
			echo "Do you want these changes added to policy [y/n]?"
			read ANS
			if [ "$ANS" = "y" -o "$ANS" = "Y" ] ; then
				echo "Updating policy"
				echo -e "$rules" | audit2allow -R >> thumbslug.te
				# Fall though and rebuild policy
			else
				exit 0
			fi
		else
			echo "No new avcs found"
			exit 0
		fi
	else
		echo -e $USAGE
		exit 1
	fi
elif [ $# -ge 2 ] ; then
	echo -e $USAGE
	exit 1
fi

echo "Building and Loading Policy"
set -x
make -f /usr/share/selinux/devel/Makefile || exit
/usr/sbin/semodule -i thumbslug.pp

/sbin/restorecon -F -R -v /usr/bin/thumbslug
/sbin/restorecon -F -R -v /usr/lib/systemd/system/thumbslug.service
/sbin/restorecon -F -R -v /var/lock/subsys/thumbslug
/sbin/restorecon -F -R -v /var/log/thumbslug/error.log
/sbin/restorecon -F -R -v /var/run/thumbslug/thumbslug.pid
/sbin/restorecon -F -R -v /var/log/thumbslug/access.log
/usr/sbin/semanage port -a -t thumbslug_port_t -p tcp 8088
