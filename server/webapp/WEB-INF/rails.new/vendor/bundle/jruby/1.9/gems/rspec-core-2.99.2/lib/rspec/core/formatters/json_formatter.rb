require 'rspec/core/formatters/base_formatter'
require 'json'

module RSpec
  module Core
    module Formatters

      class JsonFormatter < BaseFormatter

        attr_reader :output_hash

        def initialize(output)
          super
          @output_hash = {}
        end

        def message(message)
          (@output_hash[:messages] ||= []) << message
        end

        def dump_summary(duration, example_count, failure_count, pending_count)
          super(duration, example_count, failure_count, pending_count)
          @output_hash[:summary] = {
            :duration => duration,
            :example_count => example_count,
            :failure_count => failure_count,
            :pending_count => pending_count
          }
          @output_hash[:summary_line] = summary_line(example_count, failure_count, pending_count)
        end

        def summary_line(example_count, failure_count, pending_count)
          summary = pluralize(example_count, "example")
          summary << ", " << pluralize(failure_count, "failure")
          summary << ", #{pending_count} pending" if pending_count > 0
          summary
        end

        def stop
          super
          @output_hash[:examples] = examples.map do |example|
            {
              :description => example.description,
              :full_description => example.full_description,
              :status => example.execution_result[:status],
              # :example_group,
              # :execution_result,
              :file_path => example.metadata[:file_path],
              :line_number  => example.metadata[:line_number],
            }.tap do |hash|
              if e=example.exception
                hash[:exception] =  {
                  :class => e.class.name,
                  :message => e.message,
                  :backtrace => e.backtrace,
                }
              end
            end
          end
        end

        def close
          output.write @output_hash.to_json
          output.close if IO === output && output != $stdout
        end

      end
    end
  end
end
