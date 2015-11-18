@ECHO OFF
set TOOLS_BIN=%~dp0
set JRUBY_BASE=%~dp0\..\jruby-9.0.4.0
set GEM_HOME=
set GEM_PATH=
set PATH=%TOOLS_BIN%\;%JRUBY_BASE%\bin;%PATH%

%JRUBY_BASE%\bin\jruby.bat %*

