module Representable
  module Apply
    # Iterates over all property/collection definitions and yields the Definition instance.
    def apply(&block)
      representable_attrs.each do |dfn|
        block.call(dfn)
        dfn.representer_module.extend(Apply).apply(&block) if dfn.representer_module # nested.
      end

      self
    end
  end
end