module Spec
  module Runner
    # Dummy implementation for Windows that just fails (Heckle is not supported on Windows)
    class HeckleRunner
      def initialize(filter)
        raise "Heckle is not supported on Windows or Ruby 1.9"
      end
    end
  end
end
