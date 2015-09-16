module Rhino
  module Ruby
    class DefaultAccess < AccessBase
      
      def has(object, name, scope)
        if object.respond_to?(name.to_s) || 
           object.respond_to?(:"#{name}=")
          return true
        end
        super
      end
      
      def get(object, name, scope)
        if object.respond_to?(name_s = name.to_s)
          method = object.method(name_s)
          if method.arity == 0
            return Rhino.to_javascript(method.call, scope)
          else
            return Function.wrap(method.unbind)
          end
        elsif object.respond_to?(:"#{name}=")
          return nil
        end
        super
      end
      
      def put(object, name, value)
        if object.respond_to?(set_name = :"#{name}=")
          return object.send(set_name, Rhino.to_ruby(value))
        end
        super
      end
      
      extend DeprecatedAccess # backward compatibility for a while
      
    end
  end
end