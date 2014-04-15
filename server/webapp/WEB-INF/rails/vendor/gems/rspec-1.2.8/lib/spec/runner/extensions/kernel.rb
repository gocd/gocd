module Kernel
  unless respond_to?(:debugger)
    # Start a debugging session if ruby-debug is loaded with the -u/--debugger option
    def debugger(steps=1)
      # If not then just comment and proceed
      $stderr.puts "debugger statement ignored, use -u or --debugger option on rspec to enable debugging"
    end
  end
end
