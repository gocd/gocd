require "representable/populator"
require "representable/deserializer"
require "representable/serializer"

module Representable
  # The Binding wraps the Definition instance for this property and provides methods to read/write fragments.

  # The flow when parsing is Binding#read_fragment -> Populator -> Deserializer.
  # Actual parsing the fragment from the document happens in Binding#read, everything after that is generic.
  #
  # Serialization: Serializer -> {frag}/[frag]/frag -> Binding#write
  class Binding
    class FragmentNotFound
    end

    def self.build(definition, *args)
      # DISCUSS: move #create_binding to this class?
      return definition.create_binding(*args) if definition[:binding]
      build_for(definition, *args)
    end

    def initialize(definition, represented, decorator, user_options={})  # TODO: remove default arg for user options.
      @definition   = definition

      setup!(represented, decorator, user_options) # this can be used in #compile_fragment/#uncompile_fragment in case we wanna reuse the Binding instance.
    end

    attr_reader :user_options, :represented # TODO: make private/remove.

    def as # DISCUSS: private?
      @as ||= evaluate_option(:as)
    end

    # Retrieve value and write fragment to the doc.
    def compile_fragment(doc)
      evaluate_option(:writer, doc) do
        value = render_filter(get, doc)
        write_fragment(doc, value)
      end
    end

    # Parse value from doc and update the model property.
    def uncompile_fragment(doc)
      evaluate_option(:reader, doc) do
        read_fragment(doc)
      end
    end

    def write_fragment(doc, value)
      value = default_for(value)

      return if skipable_empty_value?(value)

      render_fragment(value, doc)
    end

    def render_fragment(value, doc)
      # DISCUSS: should we return a Skip object instead of this block trick? (same in Populator?)
      fragment = serialize(value) { return } # render fragments of hash, xml, yaml.

      write(doc, fragment)
    end

    def read_fragment(doc)
      fragment = read(doc) # scalar, Array, or Hash (abstract format) or un-deserialised fragment(s).

      populator.call(fragment, doc)
    end

    def render_filter(value, doc)
      evaluate_option(:render_filter, value, doc) { value }
    end

    def parse_filter(value, doc)
      evaluate_option(:parse_filter, value, doc) { value }
    end


    def get
      evaluate_option(:getter) do
        exec_context.send(getter)
      end
    end

    def set(value)
      evaluate_option(:setter, value) do
        exec_context.send(setter, value)
      end
    end

    # DISCUSS: do we really need that?
    def representer_module_for(object, *args)
      evaluate_option(:extend, object) # TODO: pass args? do we actually have args at the time this is called (compile-time)?
    end

    # Evaluate the option (either nil, static, a block or an instance method call) or
    # executes passed block when option not defined.
    def evaluate_option(name, *args)
      unless proc = self[name]
        return yield if block_given?
        return
      end

      # TODO: it would be better if user_options was nil per default and then we just don't pass it into lambdas.
      options = self[:pass_options] ? Options.new(self, user_options, represented, decorator) : user_options

      proc.evaluate(exec_context, *(args<<options)) # from Uber::Options::Value.
    end

    def [](name)
      @definition[name]
    end
    # TODO: i don't want to define all methods here, but it is faster!
    # TODO: test public interface.
    def getter
      @definition.getter
    end
    def setter
      @definition.setter
    end
    def typed?
      @definition.typed?
    end
    def representable?
      @definition.representable?
    end
    def has_default?(*args)
      @definition.has_default?(*args)
    end
    def name
      @definition.name
    end
    def representer_module
      @definition.representer_module
    end
    # perf : 1.7-1.9
    #extend Forwardable
    #def_delegators :@definition, *%w([] getter setter typed? representable? has_default? name representer_module)
    # perf : 1.7-1.9
    # %w([] getter setter typed? representable? has_default? name representer_module).each do |name|
    #   define_method(name.to_sym) { |*args| @definition.send(name, *args) }
    # end

    def skipable_empty_value?(value)
      return true if array? and self[:render_empty] == false and value and value.size == 0  # TODO: change in 2.0, don't render emtpy.
      value.nil? and not self[:render_nil]
    end

    def default_for(value)
      return self[:default] if skipable_empty_value?(value)
      value
    end

    def array?
      @definition.array?
    end

  private
    def setup!(represented, decorator, user_options)
      @represented  = represented
      @decorator    = decorator
      @user_options = user_options

      setup_exec_context!
    end

    def setup_exec_context!
      context = represented
      context = self        if self[:exec_context] == :binding
      context = decorator   if self[:exec_context] == :decorator

      @exec_context = context
    end

    attr_reader :exec_context, :decorator

    def serialize(object, &block)
      serializer.call(object, &block)
    end

    def serializer_class
      Serializer
    end

    def serializer
      serializer_class.new(self)
    end

    def populator
      populator_class.new(self)
    end

    def populator_class
      Populator
    end


    # Options instance gets passed to lambdas when pass_options: true.
    # This is considered the new standard way and should be used everywhere for forward-compat.
    Options = Struct.new(:binding, :user_options, :represented, :decorator)


    # generics for collection bindings.
    module Collection
    private
      def populator_class
        Populator::Collection
      end

      def serializer_class
        Serializer::Collection
      end
    end

    # and the same for hashes.
    module Hash
    private
      def populator_class
        Populator::Hash
      end

      def serializer_class
        Serializer::Hash
      end
    end
  end


  class DeserializeError < RuntimeError
  end
end
