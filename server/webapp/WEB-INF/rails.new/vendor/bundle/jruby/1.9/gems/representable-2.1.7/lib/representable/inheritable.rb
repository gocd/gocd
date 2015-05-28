module Representable
  # Objects marked cloneable will be cloned in #inherit!.
  module Cloneable
  end

  # Objects marked cloneable will be inherit!ed in #inherit! when available in parent and child.
  module Inheritable
    include Cloneable # all Inheritable are also Cloneable since #clone is one step of our inheritance.

    class Array < ::Array
      include Inheritable

      def inherit!(parent)
        push(*parent.clone)
      end
    end

    class Hash < ::Hash
      include Inheritable

      module InstanceMethods
        def inherit!(parent)
          #merge!(parent.clone)
          for key in (parent.keys + keys).uniq
            next unless parent_value = parent[key]

            self[key].inherit!(parent_value) and next if self[key].is_a?(Inheritable)
            self[key] = parent_value.clone and next if parent_value.is_a?(Cloneable)

            self[key] = parent_value # merge! behaviour
          end

          self
        end

        def clone
          self.class[ collect { |k,v| [k, clone_value(v)] } ]
        end

      private
        def clone_value(value)
          return value.clone if value.is_a?(Cloneable)
          value
        end
      end

      include InstanceMethods
    end
  end
end