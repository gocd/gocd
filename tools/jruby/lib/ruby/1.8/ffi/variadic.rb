module FFI
  class VariadicInvoker
    def VariadicInvoker.new(library, function, arg_types, ret_type, options)
      invoker = self.__new(library, function, ret_type, options[:convention].to_s)
      invoker.init(arg_types, options[:type_map])
      invoker
    end
    def init(arg_types, type_map)
      @fixed = Array.new
      @type_map = type_map
      arg_types.each_with_index do |type, i|
        @fixed << FFI.find_type(type, @type_map) unless type == FFI::NativeType::VARARGS
      end
    end
    def call(*args, &block)
      param_types = Array.new(@fixed)
      param_values = Array.new
      @fixed.each_with_index do |t, i|
        param_values << args[i]
      end
      i = @fixed.length
      while i < args.length
        param_types << FFI.find_type(args[i], @type_map)
        param_values << args[i + 1]
        i += 2
      end
      invoke(param_types, param_values, &block)
    end
    def attach(mod, mname)
      #
      # Attach the invoker to this module as 'mname'.
      #
      invoker = self
      params = "*args"
      call = "call"
      mod.module_eval <<-code
      @@#{mname} = invoker
      def self.#{mname}(#{params})
        @@#{mname}.#{call}(#{params})
      end
      def #{mname}(#{params})
        @@#{mname}.#{call}(#{params})
      end
      code
      invoker
    end
  end
end