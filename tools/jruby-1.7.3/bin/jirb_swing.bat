@ECHO OFF
IF NOT "%~f0" == "~f0" GOTO :WinNT
@"jruby" -S "jirb_swing" %1 %2 %3 %4 %5 %6 %7 %8 %9
GOTO :EOF
:WinNT
@"%~dp0jruby.exe" "%~dpn0" %*
