module RSpec
  module Matchers
    module BuiltIn
      # @api private
      #
      # Used _internally_ as a base class for matchers that ship with
      # rspec-expectations.
      #
      # ### Warning:
      #
      # This class is for internal use, and subject to change without notice.  We
      # strongly recommend that you do not base your custom matchers on this
      # class. If/when this changes, we will announce it and remove this warning.
      class BaseMatcher
        include RSpec::Matchers::Pretty
        include RSpec::Matchers::MatchAliases

        UNDEFINED = Object.new.freeze

        attr_reader :actual, :expected, :rescued_exception

        def initialize(expected = UNDEFINED)
          @expected = expected unless UNDEFINED.equal?(expected)
        end

        def matches?(actual)
          @actual = actual
          match(expected, actual)
        end

        def match_unless_raises(*exceptions)
          exceptions.unshift Exception if exceptions.empty?
          begin
            yield
            true
          rescue *exceptions => @rescued_exception
            false
          end
        end

        def failure_message_for_should
          assert_ivars :@actual
          "expected #{@actual.inspect} to #{name_to_sentence}#{to_sentence expected}"
        end

        def failure_message_for_should_not
          assert_ivars :@actual
          "expected #{@actual.inspect} not to #{name_to_sentence}#{to_sentence expected}"
        end

        def description
          defined?(@expected) ? "#{name_to_sentence} #{@expected.inspect}" : name_to_sentence
        end

        def diffable?
          false
        end

        # @api private
        # Most matchers are value matchers (i.e. meant to work with `expect(value)`)
        # rather than block matchers (i.e. meant to work with `expect { }`), so
        # this defaults to false. Block matchers must override this to return true.
        def supports_block_expectations?
          false
        end

      private

        def assert_ivars *ivars
          raise "#{self.class.name} needs to supply #{to_sentence ivars}" unless ivars.all? { |v| instance_variables.map(&:intern).include? v }
        end
      end
    end
  end
end
