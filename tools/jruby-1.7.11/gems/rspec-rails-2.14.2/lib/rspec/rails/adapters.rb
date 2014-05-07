require 'delegate'
require 'active_support'
require 'active_support/concern'

module RSpec
  module Rails
    if ::Rails::VERSION::STRING >= '4.1.0'
      gem 'minitest'
      require 'minitest/assertions'
      Assertions = Minitest::Assertions
    else
      begin
        require 'test/unit/assertions'
      rescue LoadError
        # work around for Rubinius not having a std std-lib
        require 'rubysl-test-unit' if defined?(RUBY_ENGINE) && RUBY_ENGINE == 'rbx'
        require 'test/unit/assertions'
      end
      Assertions = Test::Unit::Assertions
    end

    # @api private
    class AssertionDelegator < Module
      # @api private
      def initialize(*assertion_modules)
        assertion_class = Class.new(SimpleDelegator) do
          include ::RSpec::Rails::Assertions
          include ::RSpec::Rails::MinitestCounters
          assertion_modules.each { |mod| include mod }
        end

        super() do
          # @api private
          define_method :build_assertion_instance do
            assertion_class.new(self)
          end

          # @api private
          def assertion_instance
            @assertion_instance ||= build_assertion_instance
          end

          assertion_modules.each do |mod|
            mod.public_instance_methods.each do |method|
              next if method == :method_missing || method == "method_missing"
              class_eval <<-EOM, __FILE__, __LINE__ + 1
                def #{method}(*args, &block)
                  assertion_instance.send(:#{method}, *args, &block)
                end
              EOM
            end
          end
        end
      end
    end

    # Adapts example groups for `Minitest::Test::LifecycleHooks`
    #
    # @api private
    module MinitestLifecycleAdapter
      extend ActiveSupport::Concern

      included do |group|
        group.before { after_setup }
        group.after  { before_teardown }

        group.around do |example|
          before_setup
          example.run
          after_teardown
        end
      end

      def before_setup
      end

      def after_setup
      end

      def before_teardown
      end

      def after_teardown
      end
    end

    # @api private
    module MinitestCounters
      # @api private
      def assertions
        @assertions ||= 0
      end

      # @api private
      def assertions=(assertions)
        @assertions = assertions
      end
    end

    # @api private
    module SetupAndTeardownAdapter
      extend ActiveSupport::Concern

      module ClassMethods
        # @api private
        #
        # Wraps `setup` calls from within Rails' testing framework in `before`
        # hooks.
        def setup(*methods)
          methods.each do |method|
            if method.to_s =~ /^setup_(with_controller|fixtures|controller_request_and_response)$/
              prepend_before { __send__ method }
            else
              before         { __send__ method }
            end
          end
        end

        # @api private
        #
        # Wraps `teardown` calls from within Rails' testing framework in
        # `after` hooks.
        def teardown(*methods)
          methods.each { |method| after { __send__ method } }
        end
      end

      # @api private
      def method_name
        @example
      end
    end

    # @private
    module MinitestAssertionAdapter
      extend ActiveSupport::Concern

      module ClassMethods
        # @api private
        #
        # Returns the names of assertion methods that we want to expose to
        # examples without exposing non-assertion methods in Test::Unit or
        # Minitest.
        def assertion_method_names
          ::RSpec::Rails::Assertions.public_instance_methods.select{|m| m.to_s =~ /^(assert|flunk|refute)/} +
            [:build_message]
        end

        # @api private
        def define_assertion_delegators
          assertion_method_names.each do |m|
            class_eval <<-CODE, __FILE__, __LINE__ + 1
              def #{m}(*args, &block)
                assertion_delegator.send :#{m}, *args, &block
              end
            CODE
          end
        end
      end

      # @api private
      class AssertionDelegator
        include ::RSpec::Rails::Assertions
        include ::RSpec::Rails::MinitestCounters
      end

      # @api private
      def assertion_delegator
        @assertion_delegator ||= AssertionDelegator.new
      end

      included do
        define_assertion_delegators
      end
    end

    # Backwards compatibility. It's unlikely that anyone is using this
    # constant, but we had forgotten to mark it as `@private` earlier
    #
    # @api private
    TestUnitAssertionAdapter = MinitestAssertionAdapter
  end
end
