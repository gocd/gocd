set JRUBY_BASE=%~dp0\..\jruby
set GEM_HOME=%JRUBY_BASE%
set GEM_PATH=%JRUBY_BASE%\lib\ruby\gems\1.8\

%JRUBY_BASE%\bin\jruby.bat %*

