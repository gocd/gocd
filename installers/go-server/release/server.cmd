@echo off
REM *************************GO-LICENSE-START********************************
REM Copyright 2014 ThoughtWorks, Inc.
REM
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM
REM    http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM *************************GO-LICENSE-END**********************************

if not defined SERVER_DIR set SERVER_DIR=%cd%
if not defined SERVER_MEM set SERVER_MEM=512m
if not defined SERVER_MAX_MEM set SERVER_MAX_MEM=1024m
if not defined SERVER_MAX_PERM_GEN set SERVER_MAX_PERM_GEN=400m
if not defined GO_SERVER_PORT set GO_SERVER_PORT=8153
if not defined GO_SERVER_SSL_PORT set GO_SERVER_SSL_PORT=8154
if not defined JAVA_CMD set JAVA_CMD=%GO_SERVER_JAVA_HOME%\bin\java.exe
if not defined GO_SERVER_SYSTEM_PROPERTIES set GO_SERVER_SYSTEM_PROPERTIES=
set JAVA_HOME=%GO_SERVER_JAVA_HOME%

if not exist "%SERVER_DIR%\tmp" (
    mkdir "%SERVER_DIR%\tmp"
)

if not exist "%JAVA_CMD%" set JAVA_CMD=java.exe

"%JAVA_CMD%" -Xms%SERVER_MEM% -Xmx%SERVER_MAX_MEM% -XX:MaxMetaspaceSize=%SERVER_MAX_PERM_GEN% %GO_SERVER_SYSTEM_PROPERTIES% -Duser.language=en -DJAVA_SYS_MON_TEMP_DIR="%SERVER_DIR%\tmp" -Djruby.rack.request.size.threshold.bytes=30000000 -Duser.country=US -Dcruise.config.dir="%SERVER_DIR%\config" -Dcruise.config.file="%SERVER_DIR%\config\cruise-config.xml" -Dcruise.server.port=%GO_SERVER_PORT% -Dcruise.server.ssl.port=%GO_SERVER_SSL_PORT% -jar "%SERVER_DIR%\go.jar"
