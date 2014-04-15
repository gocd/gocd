module RSpec
  module Mocks
    # Provides recursive constant lookup methods useful for
    # constant stubbing.
    # @api private
    module RecursiveConstMethods
      # We only want to consider constants that are defined directly on a
      # particular module, and not include top-level/inherited constants.
      # Unfortunately, the constant API changed between 1.8 and 1.9, so
      # we need to conditionally define methods to ignore the top-level/inherited
      # constants.
      #
      # Given:
      #   class A; B = 1; end
      #   class C < A; end
      #
      # On 1.8:
      #   - C.const_get("Hash") # => ::Hash
      #   - C.const_defined?("Hash") # => false
      #   - C.constants # => ["A"]
      #   - None of these methods accept the extra `inherit` argument
      # On 1.9:
      #   - C.const_get("Hash") # => ::Hash
      #   - C.const_defined?("Hash") # => true
      #   - C.const_get("Hash", false) # => raises NameError
      #   - C.const_defined?("Hash", false) # => false
      #   - C.constants # => [:A]
      #   - C.constants(false) #=> []
      if Module.method(:const_defined?).arity == 1
        def const_defined_on?(mod, const_name)
          mod.const_defined?(const_name)
        end

        def get_const_defined_on(mod, const_name)
          if const_defined_on?(mod, const_name)
            return mod.const_get(const_name)
          end

          raise NameError, "uninitialized constant #{mod.name}::#{const_name}"
        end

        def constants_defined_on(mod)
          mod.constants.select { |c| const_defined_on?(mod, c) }
        end
      else
        def const_defined_on?(mod, const_name)
          mod.const_defined?(const_name, false)
        end

        def get_const_defined_on(mod, const_name)
          mod.const_get(const_name, false)
        end

        def constants_defined_on(mod)
          mod.constants(false)
        end
      end

      def recursive_const_get(const_name)
        const_name.split('::').inject(Object) { |mod, name| get_const_defined_on(mod, name) }
      end

      def recursive_const_defined?(const_name)
        const_name.split('::').inject([Object, '']) do |(mod, full_name), name|
          yield(full_name, name) if block_given? && !mod.is_a?(Module)
          return false unless const_defined_on?(mod, name)
          [get_const_defined_on(mod, name), [mod, name].join('::')]
        end
      end
    end

    # Provides information about constants that may (or may not)
    # have been stubbed by rspec-mocks.
    class Constant
      extend RecursiveConstMethods

      # @api private
      def initialize(name)
        @name = name
      end

      # @return [String] The fully qualified name of the constant.
      attr_reader :name

      # @return [Object, nil] The original value (e.g. before it
      #   was stubbed by rspec-mocks) of the constant, or
      #   nil if the constant was not previously defined.
      attr_accessor :original_value

      # @api private
      attr_writer :previously_defined, :stubbed

      # @return [Boolean] Whether or not the constant was defined
      #   before the current example.
      def previously_defined?
        @previously_defined
      end

      # @return [Boolean] Whether or not rspec-mocks has stubbed
      #   this constant.
      def stubbed?
        @stubbed
      end

      def to_s
        "#<#{self.class.name} #{name}>"
      end
      alias inspect to_s

      # @api private
      def self.unstubbed(name)
        const = new(name)
        const.previously_defined = recursive_const_defined?(name)
        const.stubbed = false
        const.original_value = recursive_const_get(name) if const.previously_defined?

        const
      end
      private_class_method :unstubbed

      # Queries rspec-mocks to find out information about the named constant.
      #
      # @param [String] name the name of the constant
      # @return [Constant] an object contaning information about the named
      #   constant.
      def self.original(name)
        stubber = ConstantStubber.find(name)
        stubber ? stubber.to_constant : unstubbed(name)
      end
    end

    # Provides a means to stub constants.
    class ConstantStubber
      extend RecursiveConstMethods

      # Stubs a constant.
      #
      # @param (see ExampleMethods#stub_const)
      # @option (see ExampleMethods#stub_const)
      # @return (see ExampleMethods#stub_const)
      #
      # @see ExampleMethods#stub_const
      # @note It's recommended that you use `stub_const` in your
      #  examples. This is an alternate public API that is provided
      #  so you can stub constants in other contexts (e.g. helper
      #  classes).
      def self.stub(constant_name, value, options = {})
        stubber = if recursive_const_defined?(constant_name, &raise_on_invalid_const)
          DefinedConstantReplacer
        else
          UndefinedConstantSetter
        end

        stubber = stubber.new(constant_name, value, options[:transfer_nested_constants])
        stubbers << stubber

        stubber.stub
        ensure_registered_with_mocks_space
        value
      end

      # Contains common functionality used by both of the constant stubbers.
      #
      # @api private
      class BaseStubber
        include RecursiveConstMethods

        attr_reader :original_value, :full_constant_name

        def initialize(full_constant_name, stubbed_value, transfer_nested_constants)
          @full_constant_name        = full_constant_name
          @stubbed_value             = stubbed_value
          @transfer_nested_constants = transfer_nested_constants
          @context_parts             = @full_constant_name.split('::')
          @const_name                = @context_parts.pop
        end

        def to_constant
          const = Constant.new(full_constant_name)
          const.stubbed = true
          const.previously_defined = previously_defined?
          const.original_value = original_value

          const
        end
      end

      # Replaces a defined constant for the duration of an example.
      #
      # @api private
      class DefinedConstantReplacer < BaseStubber
        def stub
          @context = recursive_const_get(@context_parts.join('::'))
          @original_value = get_const_defined_on(@context, @const_name)

          constants_to_transfer = verify_constants_to_transfer!

          @context.send(:remove_const, @const_name)
          @context.const_set(@const_name, @stubbed_value)

          transfer_nested_constants(constants_to_transfer)
        end

        def previously_defined?
          true
        end

        def rspec_reset
          @context.send(:remove_const, @const_name)
          @context.const_set(@const_name, @original_value)
        end

        def transfer_nested_constants(constants)
          constants.each do |const|
            @stubbed_value.const_set(const, get_const_defined_on(original_value, const))
          end
        end

        def verify_constants_to_transfer!
          return [] unless @transfer_nested_constants

          { @original_value => "the original value", @stubbed_value => "the stubbed value" }.each do |value, description|
            unless value.respond_to?(:constants)
              raise ArgumentError,
                "Cannot transfer nested constants for #{@full_constant_name} " +
                "since #{description} is not a class or module and only classes " +
                "and modules support nested constants."
            end
          end

          if @transfer_nested_constants.is_a?(Array)
            @transfer_nested_constants = @transfer_nested_constants.map(&:to_s) if RUBY_VERSION == '1.8.7'
            undefined_constants = @transfer_nested_constants - constants_defined_on(@original_value)

            if undefined_constants.any?
              available_constants = constants_defined_on(@original_value) - @transfer_nested_constants
              raise ArgumentError,
                "Cannot transfer nested constant(s) #{undefined_constants.join(' and ')} " +
                "for #{@full_constant_name} since they are not defined. Did you mean " +
                "#{available_constants.join(' or ')}?"
            end

            @transfer_nested_constants
          else
            constants_defined_on(@original_value)
          end
        end
      end

      # Sets an undefined constant for the duration of an example.
      #
      # @api private
      class UndefinedConstantSetter < BaseStubber
        def stub
          remaining_parts = @context_parts.dup
          @deepest_defined_const = @context_parts.inject(Object) do |klass, name|
            break klass unless const_defined_on?(klass, name)
            remaining_parts.shift
            get_const_defined_on(klass, name)
          end

          context = remaining_parts.inject(@deepest_defined_const) do |klass, name|
            klass.const_set(name, Module.new)
          end

          @const_to_remove = remaining_parts.first || @const_name
          context.const_set(@const_name, @stubbed_value)
        end

        def previously_defined?
          false
        end

        def rspec_reset
          @deepest_defined_const.send(:remove_const, @const_to_remove)
        end
      end

      # Ensures the constant stubbing is registered with
      # rspec-mocks space so that stubbed constants can
      # be restored when examples finish.
      #
      # @api private
      def self.ensure_registered_with_mocks_space
        return if defined?(@registered_with_mocks_space) && @registered_with_mocks_space
        ::RSpec::Mocks.space.add(self)
        @registered_with_mocks_space = true
      end

      # Resets all stubbed constants. This is called automatically
      # by rspec-mocks when an example finishes.
      #
      # @api private
      def self.rspec_reset
        @registered_with_mocks_space = false

        # We use reverse order so that if the same constant
        # was stubbed multiple times, the original value gets
        # properly restored.
        stubbers.reverse.each { |s| s.rspec_reset }

        stubbers.clear
      end

      # The list of constant stubbers that have been used for
      # the current example.
      #
      # @api private
      def self.stubbers
        @stubbers ||= []
      end

      def self.find(name)
        stubbers.find { |s| s.full_constant_name == name }
      end

      # Used internally by the constant stubbing to raise a helpful
      # error when a constant like "A::B::C" is stubbed and A::B is
      # not a module (and thus, it's impossible to define "A::B::C"
      # since only modules can have nested constants).
      #
      # @api private
      def self.raise_on_invalid_const
        lambda do |const_name, failed_name|
          raise "Cannot stub constant #{failed_name} on #{const_name} " +
                "since #{const_name} is not a module."
        end
      end
    end
  end
end

