@echo off

rem add jruby and rubygem binstubs to PATH
set "PATH=${additionalJRubyPaths.join(File.pathSeparator)};%PATH%"

"${javaExecutable}" ^
<% print jvmArgs.collect { entry -> $/  "${entry}"/$ }.join(" ^\n") %> ^
<% print systemProperties.collect { entry -> $/  "-D${entry}"/$ }.join(" ^\n") %> ^
  -cp ^
  "${classpath.join(';')}" ^
  ${mainClassName} ^
  %*
