@echo off
setlocal
REM this batch file allows for up to 4 commandline parameters
REM transferred to the application

REM ADDRESS OF THE INSTALLATION DIRECTORY
REM replace this value if you move the batch file or
REM move or rename JPasswords installation itself
REM must end with a '\' or be void
set _PORTROOT=$root

REM look for the best JAVA Virtual Machine
REM prefers a local copy if available
set _JAVACMD=java.exe
if exist "%_PORTROOT%wjre\bin\java.exe" set _JAVACMD="%_PORTROOT%wjre\bin\java.exe"

REM start an executable in priority sequence
REM first prio is full-size programs
REM second prio is EXE programs
set _JPWSEXE="%_PORTROOT%jpws.exe"
if exist %_JPWSEXE% goto startexe

set _JPWSJAR="%_PORTROOT%jpws.jar"
if exist %_JPWSJAR% goto startjar

REM second prio is small-size programs
set _JPWSEXE="%_PORTROOT%jpws-s.exe"
if exist %_JPWSEXE% goto startexe

set _JPWSJAR="%_PORTROOT%jpws-s.jar"
if exist %_JPWSJAR% goto startjar
goto ende

:startexe
%_JPWSEXE% "%1" "%2" "%3" "%4"
goto ende

REM JAVA START COMMAND
REM if you encounter "out of memory" errors, replace '-Xmx256m' with '-Xmx512m'
:startjar
%_JAVACMD% -Xmx256m -jar %_JPWSJAR% "%1" "%2" "%3" "%4"
goto ende

:ende
endlocal
