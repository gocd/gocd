module Spec
  module Example

    class ExampleGroupFactory
      module ClassMethods
        include Spec::Example::ArgsAndOptions

        def reset
          @example_group_types = nil
          default(ExampleGroup)
        end

        def example_group_creation_listeners
          @example_group_creation_listeners ||= []
        end

        def register_example_group(klass)
          example_group_creation_listeners.each do |listener|
            listener.register_example_group(klass)
          end
        end

        def create_shared_example_group(*args, &example_group_block) # :nodoc:
          ::Spec::Example::SharedExampleGroup.register(*args, &example_group_block)
        end

        def create_example_group(*args, &block)
          raise ArgumentError if args.empty? || block.nil?
          add_options(args)
          superclass = determine_superclass(args.last)
          superclass.describe(*args, &block)
        end

        # Registers an example group class +klass+ with the symbol +type+. For
        # example:
        #
        #   Spec::Example::ExampleGroupFactory.register(:farm, FarmExampleGroup)
        #
        # With that you can append a hash with :type => :farm to the describe
        # method and it will load an instance of FarmExampleGroup.
        #
        #   describe Pig, :type => :farm do
        #     ...
        #
        # If you don't use the hash explicitly, <tt>describe</tt> will
        # implicitly use an instance of FarmExampleGroup for any file loaded
        # from the <tt>./spec/farm</tt> directory.
        def register(key, example_group_class)
          @example_group_types[key.to_sym] = example_group_class
        end

        # Sets the default ExampleGroup class
        def default(example_group_class)
          Spec.__send__ :remove_const, :ExampleGroup if Spec.const_defined?(:ExampleGroup)
          Spec.const_set(:ExampleGroup, example_group_class)
          old = @example_group_types
          @example_group_types = Hash.new(example_group_class)
          @example_group_types.merge!(old) if old
        end

        def [](key)
          @example_group_types[key]
        end

      protected

        def determine_superclass(opts)
          if type = opts[:type]
            self[type]
          elsif opts[:location] =~ /spec(\\|\/)(#{@example_group_types.keys.sort_by{|k| k.to_s.length}.reverse.join('|')})/
            self[$2 == '' ? nil : $2.to_sym]
          else
            self[nil]
          end
        end

      end
      extend ClassMethods
      self.reset
    end
  end
end
