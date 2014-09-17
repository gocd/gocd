require "execjs/runtime"

module ExecJS
  class RubyRacerRuntime < Runtime
    class Context < Runtime::Context
      def initialize(runtime, source = "")
        source = encode(source)

        lock do
          @v8_context = ::V8::Context.new
          @v8_context.eval(source)
        end
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
          lock do
            begin
              unbox @v8_context.eval("(#{source})")
            rescue ::V8::JSError => e
              if e.value["name"] == "SyntaxError"
                raise RuntimeError, e.value.to_s
              else
                raise ProgramError, e.value.to_s
              end
            end
          end
        end
      end

      def call(properties, *args)
        lock do
          begin
            unbox @v8_context.eval(properties).call(*args)
          rescue ::V8::JSError => e
            if e.value["name"] == "SyntaxError"
              raise RuntimeError, e.value.to_s
            else
              raise ProgramError, e.value.to_s
            end
          end
        end
      end

      def unbox(value)
        case value
        when ::V8::Function
          nil
        when ::V8::Array
          value.map { |v| unbox(v) }
        when ::V8::Object
          value.inject({}) do |vs, (k, v)|
            vs[k] = unbox(v) unless v.is_a?(::V8::Function)
            vs
          end
        when String
          value.force_encoding('UTF-8')
        else
          value
        end
      end

      private
        def lock
          result, exception = nil, nil
          V8::C::Locker() do
            begin
              result = yield
            rescue Exception => e
              exception = e
            end
          end

          if exception
            raise exception
          else
            result
          end
        end
    end

    def name
      "therubyracer (V8)"
    end

    def available?
      require "v8"
      true
    rescue LoadError
      false
    end
  end
end
