require "virtus"

module Representable
  module Coercion
    class Coercer
      def initialize(type)
        @type = type
      end

      # This gets called when the :render_filter or :parse_filter option is evaluated.
      # Usually the Coercer instance is an element in a Pipeline to allow >1 filters per property.
      def call(value, doc, options)
        Virtus::Attribute.build(@type).coerce(value)
      end
    end


    def self.included(base)
      base.class_eval do
        extend ClassMethods
        register_feature Coercion
      end
    end


    module ClassMethods
      def build_definition(name, options, &block) # Representable::Declarative
        return super unless type = options[:type]

        options[:render_filter] << coercer = Coercer.new(type)
        options[:parse_filter]  << coercer

        super
      end
    end
  end
end