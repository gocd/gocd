
module Rhino
  module To

    def to_ruby(object)
      case object
      when JS::Scriptable::NOT_FOUND, JS::Undefined then nil
      when JS::Wrapper           then object.unwrap
      when JS::NativeArray       then array_to_ruby(object)
      when JS::NativeDate        then Time.at(object.getJSTimeValue / 1000)
      # Rhino 1.7R4 added ConsString for optimized String + operations :
      when Java::JavaLang::CharSequence then object.toString
      else object
      end
    end

    def to_javascript(object, scope = nil)
      case object
      when NilClass              then object
      when String, Numeric       then object
      when TrueClass, FalseClass then object
      when JS::Scriptable        then object
      when Array                 then array_to_javascript(object, scope)
      when Hash                  then hash_to_javascript(object, scope)
      when Time                  then time_to_javascript(object, scope)
      when Method, UnboundMethod then Ruby::Function.wrap(object, scope)
      when Proc                  then Ruby::Function.wrap(object, scope)
      when Class                 then Ruby::Constructor.wrap(object, scope)
      else RubyObject.wrap(object, scope)
      end
    end

    def args_to_ruby(args)
      args.map { |arg| to_ruby(arg) }
    end

    def args_to_javascript(args, scope = nil)
      args.map { |arg| to_javascript(arg, scope) }.to_java
    end

    private

      def array_to_ruby(js_array)
        js_array.length.times.map { |i| to_ruby( js_array.get(i, js_array) ) }
      end

      def array_to_javascript(rb_array, scope = nil)
        # First convert all array elements to their javascript equivalents and
        # then invoke to_java below in order to create a Java array.  This allows
        # arrays with nested hashes to be converted properly.
        converted_rb_array = rb_array.map do |rb_element|
          to_javascript(rb_element, scope)
        end

        if scope && context = JS::Context.getCurrentContext
          context.newArray(scope, converted_rb_array.to_java)
        else
          JS::NativeArray.new(converted_rb_array.to_java)
        end
      end

      def hash_to_javascript(rb_hash, scope = nil)
        js_object =
          if scope && context = JS::Context.getCurrentContext
            context.newObject(scope)
          else
            JS::NativeObject.new
          end
        # JS::NativeObject implements Map put it's #put does :
        # throw new UnsupportedOperationException(); thus no []=
        rb_hash.each_pair do |key, val|
          js_val = to_javascript(val, scope)
          JS::ScriptableObject.putProperty(js_object, key.to_s, js_val)
        end
        js_object
      end

      def time_to_javascript(time, scope = nil)
        millis = time.to_f * 1000
        if scope && context = JS::Context.getCurrentContext
          JS::ScriptRuntime.newObject(context, scope, 'Date', [ millis ].to_java)
        else
          # the pure reflection way - god I love Java's private :
          js_klass = JS::NativeObject.java_class.to_java
          new = js_klass.getDeclaredConstructor; new.setAccessible(true)
          js_date = new.newInstance
          date = js_klass.getDeclaredField(:date); date.setAccessible(true)
          date.setDouble(js_date, millis)
          js_date
        end
      end

  end
end
