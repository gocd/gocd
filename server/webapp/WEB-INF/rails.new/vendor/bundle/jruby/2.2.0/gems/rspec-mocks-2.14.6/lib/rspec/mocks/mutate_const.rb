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
      #   - C.constants # => ["B"]
      #   - None of these methods accept the extra `inherit` argument
      # On 1.9:
      #   - C.const_get("Hash") # => ::Hash
      #   - C.const_defined?("Hash") # => true
      #   - C.const_get("Hash", false) # => raises NameError
      #   - C.const_defined?("Hash", false) # => false
      #   - C.constants # => [:B]
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
        normalize_const_name(const_name).split('::').inject(Object) do |mod, name|
          get_const_defined_on(mod, name)
        end
      end

      def recursive_const_defined?(const_name)
        normalize_const_name(const_name).split('::').inject([Object, '']) do |(mod, full_name), name|
          yield(full_name, name) if block_given? && !mod.is_a?(Module)
          return false unless const_defined_on?(mod, name)
          [get_const_defined_on(mod, name), [mod, name].join('::')]
        end
      end

      def normalize_const_name(const_name)
        const_name.sub(/\A::/, '')
      end
    end

    # Provides information about constants that may (or may not)
    # have been mutated by rspec-mocks.
    class Constant
      extend RecursiveConstMethods

      # @api private
      def initialize(name)
        @name = name
        @previously_defined = false
        @stubbed = false
        @hidden = false
      end

      # @return [String] The fully qualified name of the constant.
      attr_reader :name

      # @return [Object, nil] The original value (e.g. before it
      #   was mutated by rspec-mocks) of the constant, or
      #   nil if the constant was not previously defined.
      attr_accessor :original_value

      # @api private
      attr_writer :previously_defined, :stubbed, :hidden

      # @return [Boolean] Whether or not the constant was defined
      #   before the current example.
      def previously_defined?
        @previously_defined
      end

      # @return [Boolean] Whether or not rspec-mocks has mutated
      #   (stubbed or hidden) this constant.
      def mutated?
        @stubbed || @hidden
      end

      # @return [Boolean] Whether or not rspec-mocks has stubbed
      #   this constant.
      def stubbed?
        @stubbed
      end

      # @return [Boolean] Whether or not rspec-mocks has hidden
      #   this constant.
      def hidden?
        @hidden
      end

      def to_s
        "#<#{self.class.name} #{name}>"
      end
      alias inspect to_s

      # @api private
      def self.unmutated(name)
        const = new(name)
        const.previously_defined = recursive_const_defined?(name)
        const.stubbed = false
        const.hidden = false
        const.original_value = recursive_const_get(name) if const.previously_defined?

        const
      end
      private_class_method :unmutated

      # Queries rspec-mocks to find out information about the named constant.
      #
      # @param [String] name the name of the constant
      # @return [Constant] an object contaning information about the named
      #   constant.
      def self.original(name)
        mutator = ConstantMutator.find(name)
        mutator ? mutator.to_constant : unmutated(name)
      end
    end

    # Provides a means to stub constants.
    class ConstantMutator
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
        mutator = if recursive_const_defined?(constant_name, &raise_on_invalid_const)
          DefinedConstantReplacer
        else
          UndefinedConstantSetter
        end

        mutate(mutator.new(constant_name, value, options[:transfer_nested_constants]))
        value
      end

      # Hides a constant.
      #
      # @param (see ExampleMethods#hide_const)
      #
      # @see ExampleMethods#hide_const
      # @note It's recommended that you use `hide_const` in your
      #  examples. This is an alternate public API that is provided
      #  so you can hide constants in other contexts (e.g. helper
      #  classes).
      def self.hide(constant_name)
        return unless recursive_const_defined?(constant_name)

        mutate(ConstantHider.new(constant_name, nil, { }))
        nil
      end

      # Contains common functionality used by all of the constant mutators.
      #
      # @api private
      class BaseMutator
        include RecursiveConstMethods

        attr_reader :original_value, :full_constant_name

        def initialize(full_constant_name, mutated_value, transfer_nested_constants)
          @full_constant_name        = normalize_const_name(full_constant_name)
          @mutated_value             = mutated_value
          @transfer_nested_constants = transfer_nested_constants
          @context_parts             = @full_constant_name.split('::')
          @const_name                = @context_parts.pop
        end

        def to_constant
          const = Constant.new(full_constant_name)
          const.original_value = original_value

          const
        end
      end

      # Hides a defined constant for the duration of an example.
      #
      # @api private
      class ConstantHider < BaseMutator
        def mutate
          @context = recursive_const_get(@context_parts.join('::'))
          @original_value = get_const_defined_on(@context, @const_name)

          @context.__send__(:remove_const, @const_name)
        end

        def to_constant
          const = super
          const.hidden = true
          const.previously_defined = true

          const
        end

        def rspec_reset
          @context.const_set(@const_name, @original_value)
        end
      end

      # Replaces a defined constant for the duration of an example.
      #
      # @api private
      class DefinedConstantReplacer < BaseMutator
        def mutate
          @context = recursive_const_get(@context_parts.join('::'))
          @original_value = get_const_defined_on(@context, @const_name)

          constants_to_transfer = verify_constants_to_transfer!

          @context.__send__(:remove_const, @const_name)
          @context.const_set(@const_name, @mutated_value)

          transfer_nested_constants(constants_to_transfer)
        end

        def to_constant
          const = super
          const.stubbed = true
          const.previously_defined = true

          const
        end

        def rspec_reset
          @context.__send__(:remove_const, @const_name)
          @context.const_set(@const_name, @original_value)
        end

        def transfer_nested_constants(constants)
          constants.each do |const|
            @mutated_value.const_set(const, get_const_defined_on(original_value, const))
          end
        end

        def verify_constants_to_transfer!
          return [] unless @transfer_nested_constants

          { @original_value => "the original value", @mutated_value => "the stubbed value" }.each do |value, description|
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
      class UndefinedConstantSetter < BaseMutator
        def mutate
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
          context.const_set(@const_name, @mutated_value)
        end

        def to_constant
          const = super
          const.stubbed = true
          const.previously_defined = false

          const
        end

        def rspec_reset
          @deepest_defined_const.__send__(:remove_const, @const_to_remove)
        end
      end

      # Uses the mutator to mutate (stub or hide) a constant. Ensures that
      # the mutator is correctly registered so it can be backed out at the end
      # of the test.
      #
      # @api private
      def self.mutate(mutator)
        register_mutator(mutator)
        mutator.mutate
      end

      # Resets all stubbed constants. This is called automatically
      # by rspec-mocks when an example finishes.
      #
      # @api private
      def self.reset_all
        # We use reverse order so that if the same constant
        # was stubbed multiple times, the original value gets
        # properly restored.
        mutators.reverse.each { |s| s.rspec_reset }

        mutators.clear
      end

      # The list of constant mutators that have been used for
      # the current example.
      #
      # @api private
      def self.mutators
        @mutators ||= []
      end

      # @api private
      def self.register_mutator(mutator)
        mutators << mutator
      end

      def self.find(name)
        mutators.find { |s| s.full_constant_name == name }
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

    # Keeps backwards compatibility since we had released an rspec-mocks that
    # only supported stubbing. Later, we released the hide_const feature and
    # decided that the term "mutator" was a better term to wrap up the concept
    # of both stubbing and hiding.
    ConstantStubber = ConstantMutator
  end
end
