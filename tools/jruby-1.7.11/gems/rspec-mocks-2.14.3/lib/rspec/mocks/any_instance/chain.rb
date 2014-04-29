module RSpec
  module Mocks
    module AnyInstance
      class Chain
        def initialize(*args, &block)
          @expectation_args  = args
          @expectation_block = block
        end

        module Customizations
          # @macro [attach] record
          #   @method $1(*args, &block)
          #   Records the `$1` message for playback against an instance that
          #   invokes a method stubbed or mocked using `any_instance`.
          #
          #   @see RSpec::Mocks::MessageExpectation#$1
          #
          def self.record(method_name)
            class_eval(<<-EOM, __FILE__, __LINE__ + 1)
              def #{method_name}(*args, &block)
                record(:#{method_name}, *args, &block)
              end
            EOM
          end

          record :and_return
          record :and_raise
          record :and_throw
          record :and_yield
          record :and_call_original
          record :with
          record :once
          record :twice
          record :any_number_of_times
          record :exactly
          record :times
          record :never
          record :at_least
          record :at_most
        end

        include Customizations

        # @private
        def playback!(instance)
          message_expectation = create_message_expectation_on(instance)
          messages.inject(message_expectation) do |object, message|
            object.__send__(*message.first, &message.last)
          end
        end

        # @private
        def constrained_to_any_of?(*constraints)
          constraints.any? do |constraint|
            messages.any? do |message|
              message.first.first == constraint
            end
          end
        end

        # @private
        def expectation_fulfilled!
          @expectation_fulfilled = true
        end

        def never
          ErrorGenerator.raise_double_negation_error("expect_any_instance_of(MyClass)") if negated?
          super
        end

        private

        def negated?
          messages.any? { |(message, *_), _| message.to_sym == :never }
        end

        def messages
          @messages ||= []
        end

        def last_message
          messages.last.first.first unless messages.empty?
        end

        def record(rspec_method_name, *args, &block)
          verify_invocation_order(rspec_method_name, *args, &block)
          messages << [args.unshift(rspec_method_name), block]
          self
        end
      end
    end
  end
end
