module Representable
  #
  # populator
      # skip_parse? --> return
      # deserialize (this is where additional logic can happen, e.g. Object-HAL's collection semantics).
      # parse_filter
      # set
  class Populator # rename to Deserializer?
    def initialize(binding)
      @binding = binding
    end

    # goal of this is to have this workflow apply-able to collections AND to items per collection, or for items in hashes.
    def call(fragment, doc)
      # the rest should be applied per item (collection) or per fragment (collection and property)
      if fragment == Binding::FragmentNotFound
        return unless @binding.has_default?
        value = @binding[:default]
      else
        # DISCUSS: should we return a Skip object instead of this block trick? (same in Binding#serialize?)
        value = deserialize(fragment) { return } # stop here if skip_parse?
      end

      value = @binding.parse_filter(value, doc)
        # parse_filter
        # set
      @binding.set(value)
    end

  private
    def deserialize(fragment)
      return yield if @binding.evaluate_option(:skip_parse, fragment) # TODO: move this into Deserializer.

      # use a Deserializer to transform fragment to/into object.
      deserializer.call(fragment) # CollectionDeserializer/HashDeserializer/etc.
    end

    def deserializer_class
      Deserializer
    end

    def deserializer
      deserializer_class.new(@binding)
    end


    # A separated collection deserializer/populator allows us better dealing with populating/modifying
    # collections of models. (e.g. replace, update, push, etc.).
    # That also gives us a place to apply options like :parse_filter, etc. per item.
    class Collection < self
    private
      def deserialize(fragment)
        return deserializer.call(fragment)
      end

      def deserializer
        Deserializer::Collection.new(@binding)
      end
    end

    class Hash < self
    private
      def deserializer_class
        Deserializer::Hash
      end
    end
  end
end