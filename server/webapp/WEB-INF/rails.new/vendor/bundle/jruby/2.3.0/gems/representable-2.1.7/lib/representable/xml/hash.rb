require 'representable/xml'
require 'representable/hash_methods'

module Representable::XML
  module AttributeHash
    include Representable::XML
    include Representable::HashMethods

    def self.included(base)
      base.class_eval do
        include Representable
        extend ClassMethods
        representable_attrs.add(:_self, {:hash => true, :use_attributes => true})
      end
    end


    module ClassMethods
      def values(options)
        hash :_self, options.merge!(:use_attributes => true)
      end
    end
  end

  module Hash
    include Representable::XML
    include HashMethods

    def self.included(base)
      base.class_eval do
        include Representable
        extend ClassMethods
        representable_attrs.add(:_self, {:hash => true})
      end
    end


    module ClassMethods
      def values(options)
        hash :_self, options
      end
    end
  end
end
