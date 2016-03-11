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

cd /d %~dp0

if not defined STOP_BEFORE_STARTUP set STOP_BEFORE_STARTUP=Y
if not defined AGENT_DIR set AGENT_DIR=%cd%
if not defined AGENT_MEM set AGENT_MEM=128m
if not defined AGENT_MAX_MEM set AGENT_MAX_MEM=256m
if not defined GO_SERVER set GO_SERVER=127.0.0.1
if not defined GO_SERVER_PORT set GO_SERVER_PORT=8153
if not defined GO_AGENT_SYSTEM_PROPERTIES set GO_AGENT_SYSTEM_PROPERTIES=

if defined JVM_DEBUG (
  set JVM_DEBUG=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006
) else (
  set JVM_DEBUG=
)

if defined GC_LOG (
  set GC_LOG=-verbose:gc -Xloggc:go-agent-gc.log -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGC
) else (
  set GC_LOG=
)

set AGENT_STARTUP_ARGS=-Xms%AGENT_MEM% -Xmx%AGENT_MAX_MEM% %JVM_DEBUG% %GC_LOG% %GO_AGENT_SYSTEM_PROPERTIES% -Dcruise.console.publish.interval=10

set JAVA_CMD=%GO_AGENT_JAVA_HOME%\bin\java.exe
set JAVA_HOME=%GO_AGENT_JAVA_HOME%

if not exist "%JAVA_CMD%" set JAVA_CMD=java.exe

if %STOP_BEFORE_STARTUP% == Y net stop "Go Agent"

"%JAVA_CMD%" -jar "%AGENT_DIR%\agent-bootstrapper.jar" %GO_SERVER% %GO_SERVER_PORT%
