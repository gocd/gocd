@echo off
rem # This batch file intalls and uninstalls the JES server
rem # as an NT service.  Please edit the variables below
rem # for your environment before running this script!


rem # These values should be changed for your local system

set SERVICE_NAME=JavaEmailServer
set JES_HOME_DIR=c:\mail
set JDK_HOME_DIR=c:\jdk1.3.1
rem # This must be classic, hotspot, or server.
set JDK_MODE=server

rem # Determine the requested action
if "%1" == "install" goto install
if "%1" == "-install" goto install
if "%1" == "uninstall" goto uninstall
if "%1" == "-uninstall" goto uninstall
goto usage

:install
echo Installing Service: %SERVICE_NAME% with the following values:
echo JES_HOME_DIR=%JES_HOME_DIR%
echo JDK_HOME_DIR=%JDK_HOME_DIR%
echo JDK_MODE=%JDK_MODE%

@set LOCALCLASSPATH=%JDK_HOME_DIR%\lib\tools.jar
@for %%i in (%JES_HOME_DIR%\lib\*.jar) do call lcp.bat %%i
JavaService.exe -install %SERVICE_NAME% %JDK_HOME_DIR%\jre\bin\%JDK_MODE%\jvm.dll -Djava.class.path=%LOCALCLASSPATH% -start com.ericdaugherty.mail.server.Mail -params %JES_HOME_DIR% -stop com.ericdaugherty.mail.server.Mail -method shutdown -out %JES_HOME_DIR%\jes-service.log -err %JES_HOME_DIR%\jes-service-error.log -current %JES_HOME_DIR%\bin
goto eof

:uninstall
echo Removing Service: %SERVICE_NAME%
JavaService.exe -uninstall %SERVICE_NAME%
goto eof

:usage
echo Please edit this file before executing.
echo To install the service, type:
echo ntservice.bat install
echo To uninstall the service, type:
echo ntservice.bat uninstall

goto eof

:eof