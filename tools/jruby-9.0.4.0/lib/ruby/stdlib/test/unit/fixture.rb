module Test
  module Unit
    module Fixture
      class << self
        def included(base)
          base.extend(ClassMethods)

          [:setup, :cleanup, :teardown].each do |type|
            observer = lambda do |test_case, _, _, value, callback|
              if value.nil?
                test_case.fixture[type].unregister(callback)
              else
                test_case.fixture[type].register(callback, value)
              end
            end
            base.register_attribute_observer(type, &observer)
          end
        end
      end

      class Fixture
        attr_reader :setup
        attr_reader :cleanup
        attr_reader :teardown
        def initialize(test_case)
          @test_case = test_case
          @setup = HookPoint.new(:after => :append)
          @cleanup = HookPoint.new(:before => :prepend)
          @teardown = HookPoint.new(:before => :prepend)
        end

        def [](type)
          case type
          when :setup
            @setup
          when :cleanup
            @cleanup
          when :teardown
            @teardown
          end
        end

        def before_callbacks(type)
          target_test_cases.inject([]) do |callbacks, ancestor|
            callbacks | ancestor.fixture[type].before_callbacks
          end
        end

        def after_callbacks(type)
          target_test_cases.inject([]) do |callbacks, ancestor|
            callbacks | ancestor.fixture[type].after_callbacks
          end
        end

        private
        def target_test_cases
          @cached_target_test_cases ||= collect_target_test_cases
        end

        def collect_target_test_cases
          ancestors = @test_case.ancestors
          base_index = ancestors.index(::Test::Unit::Fixture)
          interested_ancestors = ancestors[0, base_index].find_all do |ancestor|
            ancestor.is_a?(Class)
          end
          interested_ancestors.reverse
        end
      end

      class HookPoint
        def initialize(default_options)
          @default_options = default_options
          @before_callbacks = []
          @after_callbacks = []
          @unregistered_callbacks = []
        end

        def register(method_name_or_callback, options=nil)
          options ||= {}
          unless valid_register_options?(options)
            message = "must be {:before => :prepend}, " +
              "{:before => :append}, {:after => :prepend} or " +
              "{:after => :append}: #{options.inspect}"
            raise ArgumentError, message
          end

          if options.empty?
            options = @default_options
          end
          before_how = options[:before]
          after_how = options[:after]
          if before_how
            @before_callbacks = add_callback(@before_callbacks,
                                             method_name_or_callback,
                                             before_how)
          else
            @after_callbacks = add_callback(@after_callbacks,
                                            method_name_or_callback,
                                            after_how)
          end
        end

        def unregister(method_name_or_callback)
          @unregistered_callbacks << method_name_or_callback
        end

        def before_callbacks
          @before_callbacks - @unregistered_callbacks
        end

        def after_callbacks
          @after_callbacks - @unregistered_callbacks
        end

        private
        def valid_register_options?(options)
          return true if options.empty?
          return false if options.size > 1

          key = options.keys.first
          [:before, :after].include?(key) and
            [:prepend, :append].include?(options[key])
        end

        def add_callback(callbacks, method_name_or_callback, how)
          case how
          when :prepend
            [method_name_or_callback] | callbacks
          when :append
            callbacks | [method_name_or_callback]
          end
        end
      end

      module ClassMethods
        def fixture
          @fixture ||= Fixture.new(self)
        end

        def setup(*method_names, &callback)
          register_fixture(:setup, *method_names, &callback)
        end

        def unregister_setup(*method_names_or_callbacks)
          unregister_fixture(:setup, *method_names_or_callbacks)
        end

        def cleanup(*method_names, &callback)
          register_fixture(:cleanup, *method_names, &callback)
        end

        def unregister_cleanup(*method_names_or_callbacks)
          unregister_fixture(:cleanup, *method_names_or_callbacks)
        end

        def teardown(*method_names, &callback)
          register_fixture(:teardown, *method_names, &callback)
        end

        def unregister_teardown(*method_names_or_callbacks)
          unregister_fixture(:teardown, *method_names_or_callbacks)
        end

        private
        def register_fixture(fixture, *method_names, &callback)
          options = {}
          options = method_names.pop if method_names.last.is_a?(Hash)
          callbacks = method_names
          callbacks << callback if callback
          attribute(fixture, options, *callbacks)
        end

        def unregister_fixture(fixture, *method_names_or_callbacks)
          attribute(fixture, nil, *method_names_or_callbacks)
        end
      end

      private
      def run_fixture(type, options={})
        [
          self.class.fixture.before_callbacks(type),
          type,
          self.class.fixture.after_callbacks(type),
        ].flatten.each do |method_name_or_callback|
          run_fixture_callback(method_name_or_callback, options)
        end
      end

      def run_fixture_callback(method_name_or_callback, options)
        if method_name_or_callback.respond_to?(:call)
          callback = lambda do
            instance_eval(&method_name_or_callback)
          end
        else
          return unless respond_to?(method_name_or_callback, true)
          callback = lambda do
            __send__(method_name_or_callback)
          end
        end

        begin
          callback.call
        rescue Exception
          raise unless options[:handle_exception]
          raise unless handle_exception($!)
        end
      end

      def run_setup
        run_fixture(:setup)
      end

      def run_cleanup
        run_fixture(:cleanup)
      end

      def run_teardown
        run_fixture(:teardown, :handle_exception => true)
      end
    end
  end
end
