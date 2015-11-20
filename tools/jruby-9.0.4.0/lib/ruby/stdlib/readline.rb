require 'readline/version'

require 'jar-dependencies'
require_jar( 'jline', 'jline', Readline::Version::JLINE_VERSION )

require "readline.jar"

# boot extension
begin
  org.jruby.ext.readline.ReadlineService.new.load(JRuby.runtime, false)
rescue NameError => ne
  raise NameError, "unable to load readline subsystem: #{ne.message}", ne.backtrace
end
