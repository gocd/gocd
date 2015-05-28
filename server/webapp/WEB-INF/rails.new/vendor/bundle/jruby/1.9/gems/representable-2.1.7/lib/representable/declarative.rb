module Representable
  module Declarative
    def representable_attrs
      @representable_attrs ||= build_config
    end

    def representation_wrap=(name)
      representable_attrs.wrap = name
    end

    def collection(name, options={}, &block)
      options[:collection] = true # FIXME: don't override original.
      property(name, options, &block)
    end

    def hash(name=nil, options={}, &block)
      return super() unless name  # allow Object.hash.

      options[:hash] = true
      property(name, options, &block)
    end

    # Allows you to nest a block of properties in a separate section while still mapping them to the outer object.
    def nested(name, options={}, &block)
      options = options.merge(
        :use_decorator => true,
        :getter        => lambda { |*| self },
        :setter        => lambda { |*| },
        :instance      => lambda { |*| self }
      ) # DISCUSS: should this be a macro just as :parse_strategy?

      property(name, options, &block)
    end

    def property(name, options={}, &block)
      representable_attrs.add(name, options) do |default_options| # handles :inherit.
        build_definition(name, default_options, &block)
      end
    end

    def build_inline(base, features, name, options, &block) # DISCUSS: separate module?
      Module.new do
        include Representable
        feature *features # Representable::JSON or similar.
        include base if base # base when :inherit, or in decorator.

        module_eval &block
      end
    end

  private
    # This method is meant to be overridden if you want to add or change DSL options.
    # The options hash is already initialized, add or change elements as you need.
    #
    # Example:
    #
    # def build_definition(name, options, &block)
    #   options[:pass_options] = true
    #   options[:render_filter] << Sanitizer.new if options[:sanitize]
    #   options[:setter] << lambda { |fragment, options| send("set_#{options.binding.name}=", fragment) }
    #   super # don't forget to call super
    # end
    def build_definition(name, options, &block)
      base = nil

      if options[:inherit] # TODO: move this to Definition.
        base = representable_attrs.get(name).representer_module
      end # FIXME: can we handle this in super/Definition.new ?

      if block
        options[:_inline] = true
        options[:extend]  = inline_representer_for(base, representable_attrs.features, name, options, &block)
      end
    end

    def inline_representer_for(base, features, name, options, &block)
      representer = options[:use_decorator] ? Decorator : self

      representer.build_inline(base, features.reverse, name, options, &block)
    end

    def build_config
      Config.new
    end
  end # Declarations
end