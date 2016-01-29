@ECHO OFF
mvn --version
%~dp0\tools\bin\jruby.bat -S buildr %*

