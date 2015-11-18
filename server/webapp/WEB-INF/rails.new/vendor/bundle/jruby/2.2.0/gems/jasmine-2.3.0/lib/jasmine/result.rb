module Jasmine
  class Result

    def self.map_raw_results(raw_results)
      raw_results.map { |r| new(r) }
    end

    def initialize(attrs)
      @show_full_stack_trace = attrs["show_full_stack_trace"]
      @status = attrs["status"]
      @full_name = attrs["fullName"]
      @description = attrs["description"]
      @failed_expectations = map_failures(attrs["failedExpectations"])
      @suite_name = full_name.slice(0, full_name.size - description.size - 1)
      @pending_reason = attrs["pendingReason"]
    end

    def succeeded?
      status == 'passed'
    end

    def failed?
      status == 'failed'
    end

    def pending?
      status == 'pending'
    end

    def disabled?
      status == 'disabled'
    end

    attr_reader :full_name, :description, :failed_expectations, :suite_name, :pending_reason

    private
    attr_reader :status, :show_full_stack_trace

    def map_failures(failures)
      failures.map do |e|
        if e["stack"]
          if show_full_stack_trace
            stack = e["stack"]
          else
            stack = e["stack"].split("\n").slice(0, 7).join("\n")
          end
        else
          stack = "No stack trace present."
        end

        Failure.new(e["message"], stack)
      end
    end

    class Failure < Struct.new(:message, :stack); end
  end
end
