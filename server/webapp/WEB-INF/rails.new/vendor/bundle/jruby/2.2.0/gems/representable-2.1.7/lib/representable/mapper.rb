module Representable
  # Render and parse by looping over the representer's properties and dispatching to bindings.
  # Conditionals are handled here, too.
  class Mapper
    module Methods
      def initialize(bindings, represented, options) # TODO: get rid of represented dependency.
        @represented  = represented # the (extended) model.
        @bindings     = bindings
      end

      attr_reader :bindings

      def deserialize(doc, options)
        bindings.each do |bin|
          deserialize_property(bin, doc, options)
        end
        @represented
      end

      def serialize(doc, options)
        bindings.each do |bin|
          serialize_property(bin, doc, options)
        end
        doc
      end

    private
      def serialize_property(binding, doc, options)
        return if skip_property?(binding, options.merge(:action => :serialize))
        compile_fragment(binding, doc)
      end

      def deserialize_property(binding, doc, options)
        return if skip_property?(binding, options.merge(:action => :deserialize))
        uncompile_fragment(binding, doc)
      end

      # Checks and returns if the property should be included.
      def skip_property?(binding, options)
        return true if skip_excluded_property?(binding, options)  # no need for further evaluation when :exclude'ed
        return true if skip_protected_property(binding, options)

        skip_conditional_property?(binding)
      end

      def skip_excluded_property?(binding, options)
        return unless props = options[:exclude] || options[:include]
        res   = props.include?(binding.name.to_sym)
        options[:include] ? !res : res
      end

      def skip_conditional_property?(binding)
        return unless condition = binding[:if]

        not binding.evaluate_option(:if)
      end

      # DISCUSS: this could be just another :if option in a Pipeline?
      def skip_protected_property(binding, options)
        options[:action] == :serialize ? binding[:readable] == false : binding[:writeable] == false
      end

      def compile_fragment(bin, doc)
        bin.compile_fragment(doc)
      end

      def uncompile_fragment(bin, doc)
        bin.uncompile_fragment(doc)
      end
    end

    include Methods
  end
end