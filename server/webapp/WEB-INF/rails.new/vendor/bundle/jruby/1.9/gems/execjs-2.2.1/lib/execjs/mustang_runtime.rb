require "execjs/runtime"

module ExecJS
  class MustangRuntime < Runtime
    class Context < Runtime::Context
      def initialize(runtime, source = "")
        source = encode(source)

        @v8_context = ::Mustang::Context.new
        @v8_context.eval(source)
      end

      def exec(source, options = {})
        source = encode(source)

        if /\S/ =~ source
          eval "(function(){#{source}})()", options
        end
      end

      def eval(source, options = {})
        source = encode(source)

        if /\S/ =~ source
          unbox @v8_context.eval("(#{source})")
        end
      end

      def call(properties, *args)
        unbox @v8_context.eval(properties).call(*args)
      rescue NoMethodError => e
        raise ProgramError, e.message
      end

      def unbox(value)
        case value
        when Mustang::V8::Array
          value.map { |v| unbox(v) }
        when Mustang::V8::Boolean
          value.to_bool
        when Mustang::V8::NullClass, Mustang::V8::UndefinedClass
          nil
        when Mustang::V8::Function
          nil
        when Mustang::V8::SyntaxError
          raise RuntimeError, value.message
        when Mustang::V8::Error
          raise ProgramError, value.message
        when Mustang::V8::Object
          value.inject({}) { |h, (k, v)|
            v = unbox(v)
            h[k] = v if v
            h
          }
        else
          value.respond_to?(:delegate) ? value.delegate : value
        end
      end
    end

    def name
      "Mustang (V8)"
    end

    def available?
      require "mustang"
      true
    rescue LoadError
      false
    end

    def deprecated?
      true
    end
  end
end
