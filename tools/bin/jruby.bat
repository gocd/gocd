@ECHO OFF
set TOOLS_BIN=%~dp0
set JRUBY_BASE=%~dp0\..\jruby
set GEM_HOME=
set GEM_PATH=
set PATH=%TOOLS_BIN%\;%JRUBY_BASE%\bin;%PATH%

set JRUBY_OPTS="-J-XX:+TieredCompilation -J-XX:TieredStopAtLevel=1 -J-Djruby.compile.invokedynamic=false -J-Djruby.compile.mode=OFF %JRUBY_OPTS%"
%JRUBY_BASE%\bin\jruby.bat %*

