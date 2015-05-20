module RSpec
  module Expectations
    class << self
      # @private
      def differ
        @differ ||= Differ.new
      end

      # Raises an RSpec::Expectations::ExpectationNotMetError with message.
      # @param [String] message
      # @param [Object] expected
      # @param [Object] actual
      #
      # Adds a diff to the failure message when `expected` and `actual` are
      # both present.
      def fail_with(message, expected=nil, actual=nil)
        if !message
          raise ArgumentError, "Failure message is nil. Does your matcher define the " +
                               "appropriate failure_message_for_* method to return a string?"
        end

        if actual && expected
          if all_strings?(actual, expected)
            if any_multiline_strings?(actual, expected)
              message << "\nDiff:" << differ.diff_as_string(coerce_to_string(actual), coerce_to_string(expected))
            end
          elsif no_procs?(actual, expected) && no_numbers?(actual, expected)
            message << "\nDiff:" << differ.diff_as_object(actual, expected)
          end
        end

        raise(RSpec::Expectations::ExpectationNotMetError.new(message))
      end

    private

      def no_procs?(*args)
        args.flatten.none? {|a| Proc === a}
      end

      def all_strings?(*args)
        args.flatten.all? {|a| String === a}
      end

      def any_multiline_strings?(*args)
        all_strings?(*args) && args.flatten.any? { |a| multiline?(a) }
      end

      def no_numbers?(*args)
        args.flatten.none? {|a| Numeric === a}
      end

      def coerce_to_string(string_or_array)
        return string_or_array unless Array === string_or_array
        diffably_stringify(string_or_array).join("\n")
      end

      def diffably_stringify(array)
        array.map do |entry|
          if Array === entry
            entry.inspect
          else
            entry.to_s.gsub("\n", "\\n")
          end
        end
      end

      if String.method_defined?(:encoding)
        def multiline?(string)
          string.include?("\n".encode(string.encoding))
        end
      else
        def multiline?(string)
          string.include?("\n")
        end
      end
    end
  end
end
