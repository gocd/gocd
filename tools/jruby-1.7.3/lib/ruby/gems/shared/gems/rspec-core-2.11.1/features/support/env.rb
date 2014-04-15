require 'aruba/cucumber'

Before do
  if RUBY_PLATFORM =~ /java/
    # ideas taken from: http://blog.headius.com/2010/03/jruby-startup-time-tips.html
    set_env('JRUBY_OPTS', '-X-C') # disable JIT since these processes are so short lived
    set_env('JAVA_OPTS', '-d32') # force jRuby to use client JVM for faster startup times
    @aruba_timeout_seconds = 60
  else
    @aruba_timeout_seconds = 5
  end
end
