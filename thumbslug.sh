#!/bin/sh

if [ ! -r /usr/share/java-utils/java-functions ]; then
    echo "Please 'yum install jpackage-utils'"
    exit 1
fi

if [ ! -r /etc/thumbslug/thumbslug.conf ] && [ $# -eq "0" ]; then
    echo "Can not read /etc/thumbslug/thumbslug.conf."
    echo "Please supply a conf file as an argument."
fi

buildr package

_prefer_jre="true"
. /usr/share/java-utils/java-functions

# The readlink trick gets us the name of the directory this script is in
PROJECT_DIR="$(dirname $(readlink -f $0))"
CLASSPATH="${PROJECT_DIR}/target/classes:${PROJECT_DIR}/target/resources"

# Configuration
MAIN_CLASS=org.candlepin.thumbslug.Main

# To provide more flags or options to the JVM, use the ADDITIONAL_FLAGS and
# ADDITIONAL_OPTIONS environment variables. See the /usr/share/java-utils/java-functions
# file to see the implementation. Arguments to thumbslug itself can just be provided
# as arguments to this script.
#
# For example, to enable JDWP:
# ADDITIONAL_FLAGS="-agentlib:jdwp=transport=dt_socket,address=8123,server=y,suspend=n" ./thumbslug.sh
BASE_FLAGS="-Ddaemonize=false"
BASE_OPTIONS=""
BASE_JARS="netty log4j jna commons-codec akuma oauth oauth-consumer"

# Set parameters
set_jvm
set_classpath $BASE_JARS
set_flags $BASE_FLAGS
set_options $BASE_OPTIONS

# Show some information about the JVM invokation
VERBOSE=1

# Let's start
run "$@"
