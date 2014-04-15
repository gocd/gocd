@echo off
SET JAVA_EXEC=java.exe
SET JES_HOME=c:\jes

"%JAVA_EXEC%" -client -Ddns.server= -Ddns.search= -Dsun.net.spi.nameservice.provider.1=dns,dnsjava -Djava.security.policy=%JES_HOME%\jes.policy -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dlog4j.configuration=file:/%JES_HOME%/conf/log4j.properties -cp %JES_HOME%\jes.jar;%JES_HOME%\lib\log4j-1.2.15.jar com.ericdaugherty.mail.server.Mail %JES_HOME%