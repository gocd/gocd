module RSpec
  module Mocks
    module AnyInstance
      # Given a class `TheClass`, `TheClass.any_instance` returns a `Recorder`,
      # which records stubs and message expectations for later playback on
      # instances of `TheClass`.
      #
      # Further constraints are stored in instances of [Chain](Chain).
      #
      # @see AnyInstance
      # @see Chain
      class Recorder
        # @private
        attr_reader :message_chains

        def initialize(klass)
          @message_chains = MessageChains.new
          @observed_methods = []
          @played_methods = {}
          @klass = klass
          @expectation_set = false
        end

        # Initializes the recording a stub to be played back against any
        # instance of this object that invokes the submitted method.
        #
        # @see Methods#stub
        def stub(method_name_or_method_map, &block)
          if method_name_or_method_map.is_a?(Hash)
            method_name_or_method_map.each do |method_name, return_value|
              stub(method_name).and_return(return_value)
            end
          else
            observe!(method_name_or_method_map)
            message_chains.add(method_name_or_method_map, StubChain.new(method_name_or_method_map, &block))
          end
        end

        # Initializes the recording a stub chain to be played back against any
        # instance of this object that invokes the method matching the first
        # argument.
        #
        # @see Methods#stub_chain
        def stub_chain(*method_names_and_optional_return_values, &block)
          normalize_chain(*method_names_and_optional_return_values) do |method_name, args|
            observe!(method_name)
            message_chains.add(method_name, StubChainChain.new(*args, &block))
          end
        end

        # Initializes the recording a message expectation to be played back
        # against any instance of this object that invokes the submitted
        # method.
        #
        # @see Methods#should_receive
        def should_receive(method_name, &block)
          @expectation_set = true
          observe!(method_name)
          message_chains.add(method_name, PositiveExpectationChain.new(method_name, &block))
        end

        def should_not_receive(method_name, &block)
          should_receive(method_name, &block).never
        end

        # Removes any previously recorded stubs, stub_chains or message
        # expectations that use `method_name`.
        #
        # @see Methods#unstub
        def unstub(method_name)
          unless @observed_methods.include?(method_name.to_sym)
            raise RSpec::Mocks::MockExpectationError, "The method `#{method_name}` was not stubbed or was already unstubbed"
          end
          message_chains.remove_stub_chains_for!(method_name)
          stop_observing!(method_name) unless message_chains.has_expectation?(method_name)
        end

        # @api private
        #
        # Used internally to verify that message expectations have been
        # fulfilled.
        def verify
          if @expectation_set && !message_chains.all_expectations_fulfilled?
            raise RSpec::Mocks::MockExpectationError, "Exactly one instance should have received the following message(s) but didn't: #{message_chains.unfulfilled_expectations.sort.join(', ')}"
          end
        ensure
          stop_all_observation!
          ::RSpec::Mocks.space.remove_any_instance_recorder_for(@klass)
        end

        # @private
        def stub!(*)
          raise "stub! is not supported on any_instance. Use stub instead."
        end

        # @private
        def unstub!(*)
          raise "unstub! is not supported on any_instance. Use unstub instead."
        end

        # @private
        def stop_all_observation!
          @observed_methods.each {|method_name| restore_method!(method_name)}
        end

        # @private
        def playback!(instance, method_name)
          RSpec::Mocks.space.ensure_registered(instance)
          message_chains.playback!(instance, method_name)
          @played_methods[method_name] = instance
          received_expected_message!(method_name) if message_chains.has_expectation?(method_name)
        end

        # @private
        def instance_that_received(method_name)
          @played_methods[method_name]
        end

        def build_alias_method_name(method_name)
          "__#{method_name}_without_any_instance__"
        end

        def already_observing?(method_name)
          @observed_methods.include?(method_name)
        end

        private

        def normalize_chain(*args)
          args.shift.to_s.split('.').map {|s| s.to_sym}.reverse.each {|a| args.unshift a}
          yield args.first, args
        end

        def received_expected_message!(method_name)
          message_chains.received_expected_message!(method_name)
          restore_method!(method_name)
          mark_invoked!(method_name)
        end

        def restore_method!(method_name)
          if public_protected_or_private_method_defined?(build_alias_method_name(method_name))
            restore_original_method!(method_name)
          else
            remove_dummy_method!(method_name)
          end
        end

        def restore_original_method!(method_name)
          alias_method_name = build_alias_method_name(method_name)
          @klass.class_eval do
            remove_method method_name
            alias_method  method_name, alias_method_name
            remove_method alias_method_name
          end
        end

        def remove_dummy_method!(method_name)
          @klass.class_eval do
            remove_method method_name
          end
        end

        def backup_method!(method_name)
          alias_method_name = build_alias_method_name(method_name)
          @klass.class_eval do
            alias_method alias_method_name, method_name
          end if public_protected_or_private_method_defined?(method_name)
        end

        def public_protected_or_private_method_defined?(method_name)
          @klass.method_defined?(method_name) || @klass.private_method_defined?(method_name)
        end

        def stop_observing!(method_name)
          restore_method!(method_name)
          @observed_methods.delete(method_name)
        end

        def observe!(method_name)
          stop_observing!(method_name) if already_observing?(method_name)
          @observed_methods << method_name
          backup_method!(method_name)
          @klass.class_eval(<<-EOM, __FILE__, __LINE__ + 1)
            def #{method_name}(*args, &blk)
              klass = ::RSpec::Mocks.method_handle_for(self, :#{method_name}).owner
              ::RSpec::Mocks.any_instance_recorder_for(klass).playback!(self, :#{method_name})
              self.__send__(:#{method_name}, *args, &blk)
            end
          EOM
        end

        def mark_invoked!(method_name)
          backup_method!(method_name)
          @klass.class_eval(<<-EOM, __FILE__, __LINE__ + 1)
            def #{method_name}(*args, &blk)
              method_name = :#{method_name}
              klass = ::RSpec::Mocks.method_handle_for(self, :#{method_name}).owner
              invoked_instance = ::RSpec::Mocks.any_instance_recorder_for(klass).instance_that_received(method_name)
              raise RSpec::Mocks::MockExpectationError, "The message '#{method_name}' was received by \#{self.inspect} but has already been received by \#{invoked_instance}"
            end
          EOM
        end
      end
    end
  end
end
