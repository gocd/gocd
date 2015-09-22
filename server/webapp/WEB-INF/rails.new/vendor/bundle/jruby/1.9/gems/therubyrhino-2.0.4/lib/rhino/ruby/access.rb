module Rhino
  module Ruby
    
    autoload :DefaultAccess, "rhino/ruby/default_access"
    autoload :AttributeAccess, "rhino/ruby/attribute_access"
    
    class AccessBase
      
      def has(object, name, scope)
        # try [](name) method :
        if object.respond_to?(:'[]') && object.method(:'[]').arity == 1
          unless internal?(name)
            value = object.[](name) { return true }
            return true unless value.nil?
          end
        end
        yield
      end
      
      def get(object, name, scope)
        # try [](name) method :
        if object.respond_to?(:'[]') && object.method(:'[]').arity == 1
          value = begin
            object[name]
          rescue LocalJumpError
            nil
          end unless internal?(name)
          return Rhino.to_javascript(value, scope) unless value.nil?
        end
        yield
      end
      
      def put(object, name, value)
        # try []=(name, value) method :
        if object.respond_to?(:'[]=') && object.method(:'[]=').arity == 2
          rb_value = Rhino.to_ruby(value)
          begin
            return object[name] = rb_value
          rescue LocalJumpError
          end unless internal?(name)
        end
        yield
      end
      
      private
      
      UNDERSCORES = '__'.freeze
      
      def internal?(name) # e.g. '__iterator__', '__proto__'
        name.is_a?(String) && 
          name[0..1] == UNDERSCORES && name[-2..-1] == UNDERSCORES
      end
      
    end
    
    module DeprecatedAccess
      
      def has(object, name, scope, &block)
        Rhino.warn "[DEPRECATION] `#{self.name}.has` is deprecated, please sub-class #{self.name} instead."
        instance.has(object, name, scope, &block)
      end

      def get(object, name, scope, &block)
        Rhino.warn "[DEPRECATION] `#{self.name}.get` is deprecated, please sub-class #{self.name} instead."
        instance.get(object, name, scope, &block)
      end

      def put(object, name, value, &block)
        Rhino.warn "[DEPRECATION] `#{self.name}.put` is deprecated, please sub-class #{self.name} instead."
        instance.put(object, name, value, &block)
      end

      private
      def instance
        @instance ||= self.new
      end
      
    end
    
  end
end