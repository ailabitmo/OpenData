@echo off

set classpath=..\fbase\bin;..\fajax\bin;..\fiwb\bin;bin

for %%d in (..\fbase\lib ..\fiwb\lib) do ^
for /D %%l in (%%d\*) do ^
for %%j in (%%l\*.jar) do call :append %%j

for %%j in (..\fiwb\lib\*.jar) do call :append %%j 
 
if exist jre set "JAVA_COMMAND_PREFIX=jre\bin\"
 
%JAVA_COMMAND_PREFIX%java ^
	-Dcom.fluidops.api.Bootstrap=com.fluidops.iwb.api.EndpointImpl ^
	-Dcom.fluidops.api.Parse=com.fluidops.iwb.api.CliParser ^
	-cp %classpath% ^
	com.fluidops.api.Cli2 %*
goto :eof
	
:append
echo %~1 | findstr /i webdav.*\.jar >nul:
if %errorlevel%==0 goto :eof
set classpath=%classpath%;%~1
goto :eof
