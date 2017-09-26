@ECHO OFF
set GO_ROOT=%~dp0\..\..\..
set JRUBY_BASE=%GO_ROOT%\tools\jruby
set SERVER_ROOT=%GO_ROOT%\server
set RAILS_ROOT=%SERVER_ROOT%\webapp\WEB-INF\rails.new
set GEM_HOME=%RAILS_ROOT%\vendor\bundle\jruby\1.9
set GEM_PATH=%JRUBY_BASE%\lib\ruby\gems\shared;%GEM_HOME%
set PATH=%JRUBY_BASE%\bin;%PATH%

set JRUBY_OPTS="-J-XX:+TieredCompilation -J-XX:TieredStopAtLevel=1 -J-Djruby.compile.invokedynamic=false -J-Djruby.compile.mode=OFF %JRUBY_OPTS%"
%JRUBY_BASE%\bin\jruby.bat %*
