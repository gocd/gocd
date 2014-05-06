require 'spec/runner/formatter/base_text_formatter'

module Spec
  module Runner
    module Formatter
      class FailingExampleGroupsFormatter < BaseTextFormatter
        def example_failed(example, counter, failure)
          if @example_group
            @output.puts @example_group.description.gsub(/ \(druby.*\)/,"")

            @output.flush
            @example_group = nil
          end
        end
        
        def dump_failure(counter, failure)
        end

        def dump_summary(duration, example_count, failure_count, pending_count)
        end
        
      end
    end
  end
end
