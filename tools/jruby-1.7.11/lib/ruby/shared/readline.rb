# load jline and our readline into classpath
begin
  require File.dirname(__FILE__) + "/readline/jline-2.11.jar"
  require File.dirname(__FILE__) + "/readline/readline.jar"
rescue LoadError
  # try to proceed as though classes are already in classloader
end

# boot extension
begin
  org.jruby.ext.readline.ReadlineService.new.load(JRuby.runtime, false)
rescue NameError => ne
  raise NameError, "unable to load readline subsystem: #{ne.message}", ne.backtrace
end
