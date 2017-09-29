module RSpec
  module Matchers
    module BuiltIn
      class Has
        include MatchAliases

        def initialize(expected, *args)
          @expected, @args = expected, args
        end

        def matches?(actual)
          method = predicate

          if is_private_on?(actual)
            RSpec.deprecate "matching with #{@expected} on private method #{predicate}",
              :replacement => "`expect(object.send(#{predicate.inspect})).to be_true` or change the method's visibility to public"
          end

          result = actual.__send__(method, *@args)
          check_respond_to(actual, method)
          result
        end

        def failure_message_for_should
          "expected ##{predicate}#{failure_message_args_description} to return true, got false"
        end

        def failure_message_for_should_not
          "expected ##{predicate}#{failure_message_args_description} to return false, got true"
        end

        def description
          [method_description(@expected), args_description].compact.join(' ')
        end

        # @private
        def supports_block_expectations?
          false
        end

      private

        # support 1.8.7
        if String === methods.first
          def is_private_on? actual
            actual.private_methods.include? predicate.to_s
          end
        else
          def is_private_on? actual
            actual.private_methods.include? predicate
          end
        end

        def predicate
          "#{@expected.to_s.sub("have_","has_")}?".to_sym
        end

        def method_description(method)
          method.to_s.gsub('_', ' ')
        end

        def args_description
          return nil if @args.empty?
          @args.map { |arg| arg.inspect }.join(', ')
        end

        def failure_message_args_description
          desc = args_description
          "(#{desc})" if desc
        end

        def check_respond_to(actual, method)
          RSpec.deprecate(
            "Matching with #{@expected} on an object that doesn't respond to `#{method}`",
            :replacement => "`respond_to_missing?` or `respond_to?` on your object"
          ) unless actual.respond_to?(method)
        end
      end
    end
  end
end
