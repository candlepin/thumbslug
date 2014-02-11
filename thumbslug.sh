#!/bin/sh

if [ ! -r /usr/share/java-utils/java-functions ]; then
    echo "Please 'yum install jpackage-utils'"
    exit 1
fi

_prefer_jre="true"
. /usr/share/java-utils/java-functions

# The readlink trick gets us the name of the directory this script is in
PROJECT_DIR="$(dirname $(readlink -f $0))"
CLASSPATH="${PROJECT_DIR}/target/classes:${PROJECT_DIR}/target/resources"

# Configuration
MAIN_CLASS=org.candlepin.thumbslug.Main
BASE_FLAGS=""
BASE_OPTIONS=""
BASE_JARS="netty log4j jna commons-codec akuma oauth oauth-consumer"

# Set parameters
set_jvm
set_classpath $BASE_JARS
set_flags $BASE_FLAGS
set_options $BASE_OPTIONS

# Let's start
run "$@"
