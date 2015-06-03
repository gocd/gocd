set JRUBY_BASE=%~dp0\..\jruby-1.7.11
set GEM_HOME=
set GEM_PATH=
set PATH=%JRUBY_BASE%\bin;%PATH%

%JRUBY_BASE%\bin\jruby.bat %*

