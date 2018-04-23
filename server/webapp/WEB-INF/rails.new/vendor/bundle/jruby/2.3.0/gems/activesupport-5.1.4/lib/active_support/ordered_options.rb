require "active_support/core_ext/object/blank"

module ActiveSupport
  # Usually key value pairs are handled something like this:
  #
  #   h = {}
  #   h[:boy] = 'John'
  #   h[:girl] = 'Mary'
  #   h[:boy]  # => 'John'
  #   h[:girl] # => 'Mary'
  #   h[:dog]  # => nil
  #
  # Using +OrderedOptions+, the above code could be reduced to:
  #
  #   h = ActiveSupport::OrderedOptions.new
  #   h.boy = 'John'
  #   h.girl = 'Mary'
  #   h.boy  # => 'John'
  #   h.girl # => 'Mary'
  #   h.dog  # => nil
  #
  # To raise an exception when the value is blank, append a
  # bang to the key name, like:
  #
  #   h.dog! # => raises KeyError: key not found: :dog
  #
  class OrderedOptions < Hash
    alias_method :_get, :[] # preserve the original #[] method
    protected :_get # make it protected

    def []=(key, value)
      super(key.to_sym, value)
    end

    def [](key)
      super(key.to_sym)
    end

    def method_missing(name, *args)
      name_string = name.to_s
      if name_string.chomp!("=")
        self[name_string] = args.first
      else
        bangs = name_string.chomp!("!")

        if bangs
          fetch(name_string.to_sym).presence || raise(KeyError.new("#{name_string} is blank."))
        else
          self[name_string]
        end
      end
    end

    def respond_to_missing?(name, include_private)
      true
    end
  end

  # +InheritableOptions+ provides a constructor to build an +OrderedOptions+
  # hash inherited from another hash.
  #
  # Use this if you already have some hash and you want to create a new one based on it.
  #
  #   h = ActiveSupport::InheritableOptions.new({ girl: 'Mary', boy: 'John' })
  #   h.girl # => 'Mary'
  #   h.boy  # => 'John'
  class InheritableOptions < OrderedOptions
    def initialize(parent = nil)
      if parent.kind_of?(OrderedOptions)
        # use the faster _get when dealing with OrderedOptions
        super() { |h, k| parent._get(k) }
      elsif parent
        super() { |h, k| parent[k] }
      else
        super()
      end
    end

    def inheritable_copy
      self.class.new(self)
    end
  end
end
