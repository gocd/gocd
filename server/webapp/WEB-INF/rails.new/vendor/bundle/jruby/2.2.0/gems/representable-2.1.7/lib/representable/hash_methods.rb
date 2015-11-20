module Representable
  module HashMethods
    def create_representation_with(doc, options, format)
      bin   = representable_mapper(format, options).bindings.first
      hash  = filter_keys_for(represented, options)
      bin.render_fragment(hash, doc) # TODO: Use something along Populator, which does
    end

    def update_properties_from(doc, options, format)
      bin   = representable_mapper(format, options).bindings.first
      hash  = filter_keys_for(doc, options)

      value = Deserializer::Hash.new(bin).call(hash)
      # value = bin.deserialize_from(hash)
      represented.replace(value)
    end

  private
    def filter_keys_for(hash, options)
      return hash unless props = options[:exclude] || options[:include]
      hash.reject { |k,v| options[:exclude] ? props.include?(k.to_sym) : !props.include?(k.to_sym) }
    end
  end
end
