module Kernel
  unless defined?(debugger)
    # If not already defined by ruby-debug, this implementation prints helpful
    # message to STDERR when ruby-debug is not loaded.
    def debugger(*args)
      (RSpec.configuration.error_stream || $stderr).puts "\n***** debugger statement ignored, use -d or --debug option to enable debugging\n#{caller(0)[1]}"
    end 
  end
end
