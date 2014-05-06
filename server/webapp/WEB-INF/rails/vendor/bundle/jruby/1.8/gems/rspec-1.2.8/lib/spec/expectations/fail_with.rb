module Spec
  module Expectations
    class << self
      attr_accessor :differ
      
      # raises a Spec::Expectations::ExpectationNotMetError with message
      #
      # When a differ has been assigned and fail_with is passed
      # <code>expected</code> and <code>target</code>, passes them
      # to the differ to append a diff message to the failure message.
      def fail_with(message, expected=nil, target=nil) # :nodoc:
        if message.nil?
          raise ArgumentError, "Failure message is nil. Does your matcher define the " +
                               "appropriate failure_message_for_* method to return a string?"
        end
        if (Array === message) & (message.length == 3)
          ::Spec.warn(<<-NOTICE

*****************************************************************
DEPRECATION WARNING: you are using deprecated behaviour that will
be removed from a future version of RSpec.

* Support for matchers that return arrays from failure message
methods is deprecated.
* Instead, the matcher should return a string, and expose methods
for the expected() and actual() values.
*****************************************************************
NOTICE
          )
          message, expected, target = message[0], message[1], message[2]
        end
        unless (differ.nil? || expected.nil? || target.nil?)
          if expected.is_a?(String)
            message << "\n\n Diff:" << self.differ.diff_as_string(target.to_s, expected)
          elsif expected.is_a?(Hash) && target.is_a?(Hash)
            message << "\n\n Diff:" << self.differ.diff_as_hash(target, expected)
          elsif !target.is_a?(Proc)
            message << "\n\n Diff:" << self.differ.diff_as_object(target, expected)
          end
        end
        Kernel::raise(Spec::Expectations::ExpectationNotMetError.new(message))
      end
    end
  end
end