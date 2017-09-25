module Uber
  module InheritableAttr
    def inheritable_attr(name)
      instance_eval %Q{
        def #{name}=(v)
          @#{name} = v
        end

        def #{name}
          return @#{name} if instance_variable_defined?(:@#{name})
          @#{name} = InheritableAttribute.inherit_for(self, :#{name})
        end
      }
    end

    def self.inherit_for(klass, name)
      return unless klass.superclass.respond_to?(name)

      value = klass.superclass.send(name) # could be nil.
      Clone.(value) # this could be dynamic, allowing other inheritance strategies.
    end

    class Clone
      # The second argument allows injecting more types.
      def self.call(value, uncloneable=uncloneable())
        uncloneable.each { |klass| return value if value.kind_of?(klass) }
        value.clone
      end

      def self.uncloneable
        [Symbol, TrueClass, FalseClass, NilClass]
      end
    end
  end

  InheritableAttribute = InheritableAttr
end
