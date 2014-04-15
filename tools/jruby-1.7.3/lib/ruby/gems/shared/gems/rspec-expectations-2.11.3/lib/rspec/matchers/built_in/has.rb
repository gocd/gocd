module RSpec
  module Matchers
    module BuiltIn
      class Has
        def initialize(expected, *args)
          @expected, @args = expected, args
        end

        def matches?(actual)
          actual.__send__(predicate(@expected), *@args)
        end
        alias == matches?

        def failure_message_for_should
          "expected ##{predicate(@expected)}#{failure_message_args_description} to return true, got false"
        end

        def failure_message_for_should_not
          "expected ##{predicate(@expected)}#{failure_message_args_description} to return false, got true"
        end

        def description
          [method_description(@expected), args_description].compact.join(' ')
        end

        private

        def predicate(sym)
          "#{sym.to_s.sub("have_","has_")}?".to_sym
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
      end
    end
  end
end
