@echo off
REM #########################################################################
REM Copyright 2015 ThoughtWorks, Inc.
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
REM #########################################################################

cd /d %~dp0

if not defined STOP_BEFORE_STARTUP set STOP_BEFORE_STARTUP=Y
if not defined AGENT_DIR set AGENT_DIR=%cd%
if not defined AGENT_MEM set AGENT_MEM=128m
if not defined AGENT_MAX_MEM set AGENT_MAX_MEM=256m
set GOCD_LEGACY_MODE=
if defined GO_SERVER set GOCD_LEGACY_MODE=true
if defined GO_SERVER_PORT set GOCD_LEGACY_MODE=true

if defined GOCD_LEGACY_MODE (
  echo "The environment variable GO_SERVER and GO_SERVER_PORT has been deprecated in favor of GO_SERVER_URL. Please set GO_SERVER_URL instead to a https url (https://example.com:8154/go)"
  set GO_LEGACY_MODE=
)

if not defined GO_SERVER_URL (
  if defined GO_SERVER (
    set GO_SERVER_URL=https://%GO_SERVER%:8154/go
  ) else (
    set GO_SERVER_URL=https://127.0.0.1:8154/go
  )
)

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

"%JAVA_CMD%" -jar "%AGENT_DIR%\agent-bootstrapper.jar" -serverUrl %GO_SERVER_URL% %AGENT_BOOTSTRAPPER_ARGS%
