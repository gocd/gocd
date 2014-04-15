@set LOCALCLASSPATH=""
@for %%i in ("..\lib\*.jar") do call "lcp.bat" "%%i"

java -cp %LOCALCLASSPATH% com.ericdaugherty.mail.server.Mail ..