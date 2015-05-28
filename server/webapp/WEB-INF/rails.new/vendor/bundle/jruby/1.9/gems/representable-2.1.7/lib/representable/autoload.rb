module Representable
  autoload :Hash, 'representable/hash'

  module Hash
    autoload :AllowSymbols, 'representable/hash/allow_symbols'
    autoload :Collection, 'representable/hash/collection'
  end

  autoload :Decorator, 'representable/decorator'
end
