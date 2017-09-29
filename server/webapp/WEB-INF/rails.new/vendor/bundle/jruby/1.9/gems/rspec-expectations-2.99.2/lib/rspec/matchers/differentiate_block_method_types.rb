module RSpec
  module Matchers
    # Evaluates a block in order to determine what methods, if any,
    # it defines as instance methods (using `def foo`) vs singleton
    # methods (using `def self.foo`).
    #
    # @api private
    class DifferentiateBlockMethodTypes
      def initialize(*block_args, &block)
        @block_args = block_args
        @block = block

        ignore_macro_methods

        capture_added_methods(singletons_singleton_class, singleton_methods)
        capture_added_methods(singleton_class, instance_methods)

        singleton_class.class_exec(*block_args, &block)
      end

      def singleton_methods
        @singleton_methods ||= []
      end

      def instance_methods
        @instance_methods ||= []
      end

    private

      def capture_added_methods(object, method_list)
        object.__send__(:define_method, :singleton_method_added) do |method_name|
          method_list << method_name
        end

        method_list.delete(:singleton_method_added)
      end

      unless method_defined?(:singleton_class)
        def singleton_class
          class << self; self; end
        end
      end

      def singletons_singleton_class
        class << singleton_class; self; end
      end

      def ignore_macro_methods
        def singleton_class.method_missing(*); self; end
      end
    end
  end
end

