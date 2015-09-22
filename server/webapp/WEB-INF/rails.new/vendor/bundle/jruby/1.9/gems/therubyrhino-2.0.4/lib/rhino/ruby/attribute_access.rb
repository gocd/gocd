module Rhino
  module Ruby
    class AttributeAccess < AccessBase
      
      def has(object, name, scope)
        if object.respond_to?(name.to_s) || 
           object.respond_to?(:"#{name}=") # might have a writer but no reader
          return true
        end
        super
      end
      
      def get(object, name, scope)
        name_sym = name.to_s.to_sym
        if object.respond_to?(name_sym)
          method = object.method(name_sym)
          if method.arity == 0 && # check if it is an attr_reader
            ( object.respond_to?(:"#{name}=") || 
                object.instance_variables.find { |var| var.to_sym == :"@#{name}" } )
            return Rhino.to_javascript(method.call, scope)
          else
            return Function.wrap(method.unbind)
          end
        elsif object.respond_to?(:"#{name}=")
          return nil # it does have the property but is non readable
        end
        super
      end
      
      def put(object, name, value)
        if object.respond_to?(set_name = :"#{name}=")
          rb_value = Rhino.to_ruby(value)
          return object.send(set_name, rb_value)
        end
        super
      end
      
      extend DeprecatedAccess # backward compatibility for a while
      
    end
  end
end