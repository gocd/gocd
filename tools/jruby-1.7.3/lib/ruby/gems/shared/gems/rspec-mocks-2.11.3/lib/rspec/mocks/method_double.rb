module RSpec
  module Mocks
    # @private
    class MethodDouble < Hash
      # @private
      attr_reader :method_name

      # @private
      def initialize(object, method_name, proxy)
        @method_name = method_name
        @object = object
        @proxy = proxy

        @stashed_method = StashedInstanceMethod.new(object_singleton_class, @method_name)
        @method_is_proxied = false
        store(:expectations, [])
        store(:stubs, [])
      end

      # @private
      def expectations
        self[:expectations]
      end

      # @private
      def stubs
        self[:stubs]
      end

      # @private
      def visibility
        if TestDouble === @object
          'public'
        elsif object_singleton_class.private_method_defined?(@method_name)
          'private'
        elsif object_singleton_class.protected_method_defined?(@method_name)
          'protected'
        else
          'public'
        end
      end

      # @private
      def object_singleton_class
        class << @object; self; end
      end

      # @private
      def configure_method
        RSpec::Mocks::space.add(@object) if RSpec::Mocks::space
        warn_if_nil_class
        @stashed_method.stash unless @method_is_proxied
        define_proxy_method
      end

      # @private
      def define_proxy_method
        return if @method_is_proxied

        object_singleton_class.class_eval <<-EOF, __FILE__, __LINE__ + 1
          def #{@method_name}(*args, &block)
            __mock_proxy.message_received :#{@method_name}, *args, &block
          end
          #{visibility_for_method}
        EOF
        @method_is_proxied = true
      end

      # @private
      def visibility_for_method
        "#{visibility} :#{method_name}"
      end

      # @private
      def restore_original_method
        return unless @method_is_proxied

        object_singleton_class.__send__(:remove_method, @method_name)
        @stashed_method.restore
        @method_is_proxied = false
      end

      # @private
      def verify
        expectations.each {|e| e.verify_messages_received}
      end

      # @private
      def reset
        reset_nil_expectations_warning
        restore_original_method
        clear
      end

      # @private
      def clear
        expectations.clear
        stubs.clear
      end

      # @private
      def add_expectation(error_generator, expectation_ordering, expected_from, opts, &implementation)
        configure_method
        expectation = if existing_stub = stubs.first
                        existing_stub.build_child(expected_from, 1, opts, &implementation)
                      else
                        MessageExpectation.new(error_generator, expectation_ordering, expected_from, @method_name, 1, opts, &implementation)
                      end
        expectations << expectation
        expectation
      end

      # @private
      def add_negative_expectation(error_generator, expectation_ordering, expected_from, &implementation)
        configure_method
        expectation = NegativeMessageExpectation.new(error_generator, expectation_ordering, expected_from, @method_name, &implementation)
        expectations.unshift expectation
        expectation
      end

      # @private
      def add_stub(error_generator, expectation_ordering, expected_from, opts={}, &implementation)
        configure_method
        stub = MessageExpectation.new(error_generator, expectation_ordering, expected_from, @method_name, :any, opts, &implementation)
        stubs.unshift stub
        stub
      end

      # @private
      def add_default_stub(*args, &implementation)
        return if stubs.any?
        add_stub(*args, &implementation)
      end

      # @private
      def remove_stub
        raise_method_not_stubbed_error if stubs.empty?
        expectations.empty? ? reset : stubs.clear
      end

      # @private
      def proxy_for_nil_class?
        @object.nil?
      end

      # @private
      def warn_if_nil_class
        if proxy_for_nil_class? & RSpec::Mocks::Proxy.warn_about_expectations_on_nil
          Kernel.warn("An expectation of :#{@method_name} was set on nil. Called from #{caller[4]}. Use allow_message_expectations_on_nil to disable warnings.")
        end
      end

      # @private
      def raise_method_not_stubbed_error
        raise MockExpectationError, "The method `#{method_name}` was not stubbed or was already unstubbed" 
      end

      # @private
      def reset_nil_expectations_warning
        RSpec::Mocks::Proxy.warn_about_expectations_on_nil = true if proxy_for_nil_class?
      end
    end
  end
end
