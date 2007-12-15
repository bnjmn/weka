@echo off

REM Batch file for executing the RunWeka launcher class.
REM   RunWeka.bat <command>
REM Run with option "-h" to see available commands
REM
REM Notes: 
REM - If you're getting an OutOfMemory Exception, increase the value of
REM   "maxheap" in the RunWeka.ini file.
REM - If you need more jars available in Weka, either include them in your
REM   %CLASSPATH% environment variable or add them to the "cp" placeholder
REM   in the RunWeka.ini file.
REM
REM Author:  FracPete (fracpete at waikato dot ac dot nz)
REM Version: $Revision: 1.1.2.4 $

set _cmd=%1
set _java=javaw
if "%_cmd%"=="" set _cmd=default
if "%_cmd%"=="-h" set _java=java
%_java% -classpath . RunWeka -i .\RunWeka.ini -w .\weka.jar -c %_cmd% "%2"

