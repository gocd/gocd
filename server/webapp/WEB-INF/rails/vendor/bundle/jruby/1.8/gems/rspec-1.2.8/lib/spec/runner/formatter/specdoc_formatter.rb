require 'spec/runner/formatter/base_text_formatter'

module Spec
  module Runner
    module Formatter
      class SpecdocFormatter < BaseTextFormatter
        def example_group_started(example_group)
          super
          output.puts
          output.puts example_group.description
          output.flush
        end
      
        def example_failed(example, counter, failure)
          output.puts(red("- #{example.description} (FAILED - #{counter})"))
          output.flush
        end
        
        def example_passed(example)
          message = "- #{example.description}"
          output.puts green(message)
          output.flush
        end
        
        def example_pending(example, message, deprecated_pending_location=nil)
          super
          output.puts yellow("- #{example.description} (PENDING: #{message})")
          output.flush
        end
      end
    end
  end
end
