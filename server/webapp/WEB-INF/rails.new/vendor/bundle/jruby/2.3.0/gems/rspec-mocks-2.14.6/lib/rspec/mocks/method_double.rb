module RSpec
  module Mocks
    # @private
    class MethodDouble < Hash
      # @private
      attr_reader :method_name, :object

      # @private
      def initialize(object, method_name, proxy)
        @method_name = method_name
        @object = object
        @proxy = proxy

        @method_stasher = InstanceMethodStasher.new(object_singleton_class, @method_name)
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

      class ProcWithBlock < Struct.new(:object, :method_name)

        def call(*args, &block)
          self.object.__send__(:method_missing, self.method_name, *args, &block)
        end

      end

      # @private
      def original_method
        if @method_stasher.method_is_stashed?
          # Example: a singleton method defined on @object
          ::RSpec::Mocks.method_handle_for(@object, @method_stasher.stashed_method_name)
        elsif meth = original_unrecorded_any_instance_method
          # Example: a method that has been mocked through
          #   klass.any_instance.should_receive(:msg).and_call_original
          # any_instance.should_receive(:msg) causes the method to be
          # replaced with a proxy method, and then `and_call_original`
          # is recorded and played back on the object instance. We need
          # special handling here to get a handle on the original method
          # object rather than the proxy method.
          meth
        else
          # Example: an instance method defined on one of @object's ancestors.
          original_method_from_ancestry
        end
      rescue NameError
        # We have no way of knowing if the object's method_missing
        # will handle this message or not...but we can at least try.
        # If it's not handled, a `NoMethodError` will be raised, just
        # like normally.
        ProcWithBlock.new(@object,@method_name)
      end

      def original_unrecorded_any_instance_method
        return nil unless any_instance_class_recorder_observing_method?(@object.class)
        alias_name = ::RSpec::Mocks.any_instance_recorder_for(@object.class).build_alias_method_name(@method_name)
        @object.method(alias_name)
      end

      def any_instance_class_recorder_observing_method?(klass)
        return true if ::RSpec::Mocks.any_instance_recorder_for(klass).already_observing?(@method_name)
        superklass = klass.superclass
        return false if superklass.nil?
        any_instance_class_recorder_observing_method?(superklass)
      end

      our_singleton_class = class << self; self; end
      if our_singleton_class.ancestors.include? our_singleton_class
        # In Ruby 2.1, ancestors include the correct ancestors, including the singleton classes
        def original_method_from_ancestry
          # Lookup in the ancestry, skipping over the singleton class itself
          original_method_from_ancestor(object_singleton_class.ancestors.drop(1))
        end
      else
        # @private
        def original_method_from_ancestry
          original_method_from_ancestor(object_singleton_class.ancestors)
        rescue NameError
          raise unless @object.respond_to?(:superclass)

          # Example: a singleton method defined on @object's superclass.
          #
          # Note: we have to give precedence to instance methods
          # defined on @object's class, because in a case like:
          #
          # `klass.should_receive(:new).and_call_original`
          #
          # ...we want `Class#new` bound to `klass` (which will return
          # an instance of `klass`), not `klass.superclass.new` (which
          # would return an instance of `klass.superclass`).
          original_method_from_superclass
        end
      end

      def original_method_from_ancestor(ancestors)
        klass, *rest = ancestors
        klass.instance_method(@method_name).bind(@object)
      rescue NameError
        raise if rest.empty?
        original_method_from_ancestor(rest)
      end

      if RUBY_VERSION.to_f > 1.8
        # @private
        def original_method_from_superclass
          @object.superclass.
                  singleton_class.
                  instance_method(@method_name).
                  bind(@object)
        end
      else
        # Our implementation for 1.9 (above) causes an error on 1.8:
        # TypeError: singleton method bound for a different object
        #
        # This doesn't work quite right in all circumstances but it's the
        # best we can do.
        # @private
        def original_method_from_superclass
          ::Kernel.warn <<-WARNING.gsub(/^ +\|/, '')
            |
            |WARNING: On ruby 1.8, rspec-mocks is unable to bind the original
            |`#{@method_name}` method to your partial mock object (#{@object})
            |for `and_call_original`. The superclass's `#{@method_name}` is being
            |used instead; however, it may not work correctly when executed due
            |to the fact that `self` will be #{@object.superclass}, not #{@object}.
            |
            |Called from: #{caller[2]}
          WARNING

          @object.superclass.method(@method_name)
        end
      end
      # @private
      def object_singleton_class
        class << @object; self; end
      end

      # @private
      def configure_method
        @original_visibility = visibility_for_method
        @method_stasher.stash unless @method_is_proxied
        define_proxy_method
      end

      # @private
      def define_proxy_method
        return if @method_is_proxied

        object_singleton_class.class_eval <<-EOF, __FILE__, __LINE__ + 1
          def #{@method_name}(*args, &block)
            ::RSpec::Mocks.proxy_for(self).message_received :#{@method_name}, *args, &block
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
        @method_stasher.restore
        restore_original_visibility

        @method_is_proxied = false
      end

      # @private
      def restore_original_visibility
        return unless object_singleton_class.method_defined?(@method_name) || object_singleton_class.private_method_defined?(@method_name)
        object_singleton_class.class_eval(@original_visibility, __FILE__, __LINE__)
      end

      # @private
      def verify
        expectations.each {|e| e.verify_messages_received}
      end

      # @private
      def reset
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
        expectation = MessageExpectation.new(error_generator, expectation_ordering,
                                             expected_from, self, 1, opts, &implementation)
        expectations << expectation
        expectation
      end

      # @private
      def build_expectation(error_generator, expectation_ordering)
        expected_from = IGNORED_BACKTRACE_LINE
        MessageExpectation.new(error_generator, expectation_ordering, expected_from, self)
      end

      # @private
      def add_stub(error_generator, expectation_ordering, expected_from, opts={}, &implementation)
        configure_method
        stub = MessageExpectation.new(error_generator, expectation_ordering, expected_from,
                                      self, :any, opts, &implementation)
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
      def remove_single_stub(stub)
        stubs.delete(stub)
        restore_original_method if stubs.empty? && expectations.empty?
      end

      # @private
      def raise_method_not_stubbed_error
        raise MockExpectationError, "The method `#{method_name}` was not stubbed or was already unstubbed"
      end

      # @private
      IGNORED_BACKTRACE_LINE = 'this backtrace line is ignored'
    end
  end
end
