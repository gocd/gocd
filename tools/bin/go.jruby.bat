set JRUBY_BASE=%~dp0\..\jruby-1.7.11
set GEM_HOME=%JRUBY_BASE%
set GEM_PATH=%JRUBY_BASE%\lib\ruby\gems\shared\
set PATH=%GEM_HOME%\bin;%PATH%

%JRUBY_BASE%\bin\jruby.bat %*

