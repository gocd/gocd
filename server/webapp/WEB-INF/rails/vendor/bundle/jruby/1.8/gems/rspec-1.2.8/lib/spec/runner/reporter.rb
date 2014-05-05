module Spec
  module Runner
    class Reporter
      attr_reader :options
      
      def initialize(options)
        @options = options
        @options.reporter = self
        @failures = []
        @pending_count = 0
        @example_count = 0
        @start_time = nil
        @end_time = nil
      end
      
      def example_group_started(example_group)
        @example_group = example_group
        formatters.each do |f|
          f.example_group_started(example_group)
        end
      end
      
      def example_started(example)
        formatters.each{|f| f.example_started(example)}
      end
      
      def example_finished(example, error=nil)
        @example_count += 1
        
        if error.nil?
          example_passed(example)
        elsif Spec::Example::ExamplePendingError === error
          example_pending(example, example.location, error.message)
        else
          example_failed(example, error)
        end
      end

      def example_failed(example, error)
        backtrace_tweaker.tweak_backtrace(error)
        failure = Failure.new(@example_group.description, example.description, error)
        @failures << failure
        formatters.each do |f|
          f.example_failed(example, @failures.length, failure)
        end
      end

      def start(number_of_examples)
        @start_time = Time.new
        formatters.each{|f| f.start(number_of_examples)}
      end
  
      def end
        @end_time = Time.new
      end
  
      # Dumps the summary and returns the total number of failures
      def dump
        formatters.each{|f| f.start_dump}
        dump_pending
        dump_failures
        formatters.each do |f|
          f.dump_summary(duration, @example_count, @failures.length, @pending_count)
          f.close
        end
        @failures.length
      end

      class Failure
        def initialize(group_description, example_description, exception)  # :nodoc:
          @example_name = "#{group_description} #{example_description}"
          @exception = exception
        end
        
        # The Exception object raised
        attr_reader :exception
        
        # Header messsage for reporting this failure, including the name of the
        # example and an indicator of the type of failure. FAILED indicates a
        # failed expectation. FIXED indicates a pending example that passes, and
        # no longer needs to be pending. RuntimeError indicates that a
        # RuntimeError occured.
        # 
        # == Examples
        #
        #   'A new account should have a zero balance' FAILED
        #   'A new account should have a zero balance' FIXED
        #   RuntimeError in 'A new account should have a zero balance'
        def header
          if expectation_not_met?
            "'#{@example_name}' FAILED"
          elsif pending_fixed?
            "'#{@example_name}' FIXED"
          else
            "#{@exception.class.name} in '#{@example_name}'"
          end
        end
        
        def pending_fixed? # :nodoc:
          @exception.is_a?(Spec::Example::PendingExampleFixedError)
        end

        def expectation_not_met?  # :nodoc:
          @exception.is_a?(Spec::Expectations::ExpectationNotMetError)
        end
      end

    private

      def formatters
        @options.formatters
      end

      def backtrace_tweaker
        @options.backtrace_tweaker
      end
  
      def dump_failures
        return if @failures.empty?
        @failures.inject(1) do |index, failure|
          formatters.each{|f| f.dump_failure(index, failure)}
          index + 1
        end
      end

      def dump_pending
        formatters.each{|f| f.dump_pending}
      end

      def duration
        return @end_time - @start_time unless (@end_time.nil? or @start_time.nil?)
        return "0.0"
      end
      
      def example_passed(example)
        formatters.each{|f| f.example_passed(example)}
      end

      EXAMPLE_PENDING_DEPRECATION_WARNING = <<-WARNING

*********************************************************************
DEPRECATION WARNING: RSpec's formatters have changed example_pending
to accept two arguments instead of three. Please see the rdoc
for Spec::Runner::Formatter::BaseFormatter#example_pending
for more information.
  
Please update any custom formatters to accept only two arguments
to example_pending. Support for example_pending with two arguments
and this warning message will be removed after the RSpec 2.0 release.
*********************************************************************      
WARNING
      
      def example_pending(example, ignore, message="Not Yet Implemented")
        @pending_count += 1
        formatters.each do |formatter|
          if formatter_uses_deprecated_example_pending_method?(formatter)
            Spec.warn EXAMPLE_PENDING_DEPRECATION_WARNING
            formatter.example_pending(example, message, example.location)
          else
            formatter.example_pending(example, message)
          end
        end
      end
      
      def formatter_uses_deprecated_example_pending_method?(formatter)
        formatter.method(:example_pending).arity == 3
      end
      
    end
  end
end
