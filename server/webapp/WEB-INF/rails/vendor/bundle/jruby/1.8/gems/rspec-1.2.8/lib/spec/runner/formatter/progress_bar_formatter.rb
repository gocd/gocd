require 'spec/runner/formatter/base_text_formatter'
require 'spec/runner/formatter/no_op_method_missing'

module Spec
  module Runner
    module Formatter
      class ProgressBarFormatter < BaseTextFormatter
        include NOOPMethodMissing

        def example_failed(example, counter, failure)
          @output.print colorize_failure('F', failure)
          @output.flush
        end

        def example_passed(example)
          @output.print green('.')
          @output.flush
        end
      
        def example_pending(example, message, deprecated_pending_location=nil)
          super
          @output.print yellow('*')
          @output.flush
        end
        
        def start_dump
          @output.puts
          @output.flush
        end
      end
    end
  end
end
