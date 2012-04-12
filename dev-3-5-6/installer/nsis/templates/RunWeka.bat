@echo off

REM Batch file for executing the RunWeka launcher class.
REM   RunWeka.bat <command>
REM Run with option "-h" to see available commands
REM
REM Author:  FracPete (fracpete at waikato dot ac dot nz)
REM Version: $Revision: 1.5 $

set _cmd=%1
set _java=javaw
if "%_cmd%"=="" set _cmd=default
if "%_cmd%"=="-h" set _java=java
%_java% -classpath . RunWeka -i .\RunWeka.ini -w .\weka.jar -c %_cmd% "%2"

