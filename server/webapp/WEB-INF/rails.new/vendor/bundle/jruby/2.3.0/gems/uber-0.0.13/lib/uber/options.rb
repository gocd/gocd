require 'uber/callable'

module Uber
  class Options < Hash
    def initialize(options)
      @static = options

      options.each do |k,v|
        self[k] = option = Value.new(v)
        @static = nil if option.dynamic?
      end
    end

    #   1.100000   0.060000   1.160000 (  1.159762) original
    #   0.120000   0.010000   0.130000 (  0.135803) return self
    #   0.930000   0.060000   0.990000 (  0.997095) without v.evaluate

    # Evaluates every element and returns a hash.  Accepts context and arbitrary arguments.
    def evaluate(context, *args)
      return @static unless dynamic?

      evaluate_for(context, *args)
    end

    # Evaluates a single value.
    def eval(key, *args)
      self[key].evaluate(*args)
    end

  private
    def evaluate_for(context, *args)
      {}.tap do |evaluated|
        each do |k,v|
          evaluated[k] = v.evaluate(context, *args)
        end
      end
    end

    def dynamic?
      not @static
    end


    class Value # TODO: rename to Value.
      def initialize(value, options={})
        @value, @dynamic = value, options[:dynamic]

        @proc     = proc?
        @callable = callable?
        @method   = method?

        return if options.has_key?(:dynamic)

        @dynamic = @proc || @callable || @method
      end

      def evaluate(context, *args)
        return @value unless dynamic?

        evaluate_for(context, *args)
      end

      def dynamic?
        @dynamic
      end

    private
      def evaluate_for(*args)
        return proc!(*args)     if @proc
        return callable!(*args) if @callable
        method!(*args)
         # TODO: change to context.instance_exec and deprecate first argument.
      end

      def method!(context, *args)
        context.send(@value, *args)
      end

      def proc!(context, *args)
        context.instance_exec(*args, &@value)
      end

      # Callable object is executed in its original context.
      def callable!(context, *args)
        @value.call(context, *args)
      end

      def proc?
        @value.kind_of?(Proc)
      end

      def callable?
        @value.is_a?(Uber::Callable)
      end

      def method?
        @value.is_a?(Symbol)
      end
    end
  end
end