module RSpec
  module Matchers
    module BuiltIn
      class RaiseError
        def initialize(expected_error_or_message=Exception, expected_message=nil, &block)
          @block = block
          @actual_error = nil
          case expected_error_or_message
          when String, Regexp
            @expected_error, @expected_message = Exception, expected_error_or_message
          else
            @expected_error, @expected_message = expected_error_or_message, expected_message
          end
        end

        def matches?(given_proc, negative_expectation = false)
          if negative_expectation && (expecting_specific_exception? || @expected_message)
            what_to_deprecate = if expecting_specific_exception? && @expected_message
                                  "`expect { }.not_to raise_error(SpecificErrorClass, message)`"
                                elsif expecting_specific_exception?
                                  "`expect { }.not_to raise_error(SpecificErrorClass)`"
                                elsif @expected_message
                                  "`expect { }.not_to raise_error(message)`"
                                end

            RSpec.deprecate(
              what_to_deprecate,
              :replacement => "`expect { }.not_to raise_error` (with no args)"
            )
          end
          @raised_expected_error = false
          @with_expected_message = false
          @eval_block = false
          @eval_block_passed = false
          unless given_proc.respond_to?(:call)
            ::Kernel.warn "`raise_error` was called with non-proc object #{given_proc.inspect}"
            return false
          end
          begin
            given_proc.call
          rescue Exception => @actual_error
            if @actual_error == @expected_error || @expected_error === @actual_error
              @raised_expected_error = true
              @with_expected_message = verify_message
            end
          end

          unless negative_expectation
            eval_block if @raised_expected_error && @with_expected_message && @block
          end
        ensure
          return (@raised_expected_error & @with_expected_message) ? (@eval_block ? @eval_block_passed : true) : false
        end
        alias == matches?

        def does_not_match?(given_proc)
          !matches?(given_proc, :negative_expectation)
        end

        def eval_block
          @eval_block = true
          begin
            @block[@actual_error]
            @eval_block_passed = true
          rescue Exception => err
            @actual_error = err
          end
        end

        def verify_message
          case @expected_message
          when nil
            true
          when Regexp
            @expected_message =~ @actual_error.message
          else
            @expected_message == @actual_error.message
          end
        end

        def failure_message_for_should
          @eval_block ? @actual_error.message : "expected #{expected_error}#{given_error}"
        end

        def failure_message_for_should_not
          "expected no #{expected_error}#{given_error}"
        end

        def description
          "raise #{expected_error}"
        end

        private

        def expected_error
          case @expected_message
          when nil
            @expected_error.inspect
          when Regexp
            "#{@expected_error} with message matching #{@expected_message.inspect}"
          else
            "#{@expected_error} with #{@expected_message.inspect}"
          end
        end

        def format_backtrace(backtrace)
          formatter = Matchers.configuration.backtrace_formatter
          formatter.format_backtrace(backtrace)
        end

        def given_error
          return " but nothing was raised" unless @actual_error

          backtrace = format_backtrace(@actual_error.backtrace)
          [
            ", got #{@actual_error.inspect} with backtrace:",
            *backtrace
          ].join("\n  # ")
        end

        def expecting_specific_exception?
          @expected_error != Exception
        end
      end
    end
  end
end
