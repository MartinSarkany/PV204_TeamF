#!/bin/bash
# this batch file allows for up to 4 commandline parameters
# transferred to the application

# ADDRESS OF THE INSTALLATION DIRECTORY
# replace this value if you move the batch file or
# move or rename JPasswords installation itself
# must end with a '/' or be void
_PORTROOT="$root"

# look for the best JAVA Virtual Machine
# prefers a local copy if available
_JAVACMD=java
if [ -f "${_PORTROOT}ljre/bin/java" ]; then
   _JAVACMD=./${_PORTROOT}ljre/bin/java
fi
echo JAVACMD = $_JAVACMD

# look for the best JPASSWORDS jar file
# prefers the large and luxury program version
_JPWSJAR="${_PORTROOT}jpws.jar"
if [ ! -f "$_JPWSJAR" ]; then
    _JPWSJAR=${_PORTROOT}jpws-s.jar
fi
echo JPWSJAR = $_JPWSJAR

# JAVA START COMMAND
# if you encounter "out of memory" errors, replace '-Xmx256m' with '-Xmx512m'
"$_JAVACMD" -Xmx256m -jar "$_JPWSJAR" "$1" "$2" "$3" "$4"

