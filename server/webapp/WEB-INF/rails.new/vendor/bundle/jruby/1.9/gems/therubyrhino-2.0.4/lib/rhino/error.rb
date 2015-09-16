module Rhino

  class JSError < StandardError
    
    def initialize(native)
      @native = native # NativeException wrapping a Java Throwable
      if ( value = self.value(true) ) != nil
        super value.is_a?(Exception) ? "#{value.class.name}: #{value.message}" : value
      else
        super cause ? cause.details : @native
      end
    end
    
    def inspect
      "#<#{self.class.name}: #{message}>"
    end
    
    # Returns the error message, in case of a native JavaScript value, will
    # return that value converted to a String.
    def message
      super.to_s # since 1.9.x message is expected to allways be a string
    end
    
    # Returns the (nested) cause of this error if any, most likely a 
    # #Rhino::JS::JavaScriptException instance.
    def cause
      return @cause if defined?(@cause)
      if @native.respond_to?(:cause) && @native.cause
        @native.cause
      else
        @native.is_a?(JS::RhinoException) ? @native : nil
      end
    end
    
    # Attempts to unwrap the (native) JavaScript/Java exception.
    def unwrap
      return @unwrap if defined?(@unwrap)
      cause = self.cause
      if cause && cause.is_a?(JS::WrappedException) 
        e = cause.getWrappedException
        if e && e.is_a?(Java::OrgJrubyExceptions::RaiseException)
          @unwrap = e.getException
        else
          @unwrap = e
        end
      else
        @unwrap = nil
      end
    end
    
    # Return the thown (native) JavaScript value.
    def value(unwrap = false)
      return @value if defined?(@value) && ! unwrap
      @value = get_value unless defined?(@value)
      return @value.unwrap if unwrap && @value.respond_to?(:unwrap)
      @value
    end
    
    # The backtrace is constructed using #javascript_backtrace + the Ruby part.
    def backtrace
      if js_backtrace = javascript_backtrace
        js_backtrace.push(*super)
      else
        super
      end
    end
    
    # Returns the JavaScript back-trace part for this error (the script stack).
    def javascript_backtrace(keep_elements = false)
      if cause.is_a?(JS::RhinoException)
        cause.getScriptStack.map do |element| # ScriptStackElement[]
          keep_elements ? element : element.to_s
        end
      else
        nil
      end
    end
    
    Rhino::JS::RhinoException.useMozillaStackStyle(false)
    
    private
    
    def get_value
      if ( cause = self.cause ) && cause.respond_to?(:value)
        cause.value  # e.g. JavaScriptException.getValue
      elsif ( unwrap = self.unwrap ) && unwrap.respond_to?(:value)
        unwrap.value
      else
        nil
      end
    end
    
  end
  
end