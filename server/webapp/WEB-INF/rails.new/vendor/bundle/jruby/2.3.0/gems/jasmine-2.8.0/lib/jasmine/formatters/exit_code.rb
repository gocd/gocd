module Jasmine
  module Formatters
    class ExitCode
      def initialize
        @results = []
        @global_failure = false
      end

      def format(results)
        @results += results
      end

      def done(details)
        @global_failure = details.fetch('failedExpectations', []).size > 0
      end

      def succeeded?
        !@results.detect(&:failed?) && !@global_failure
      end
    end
  end
end

