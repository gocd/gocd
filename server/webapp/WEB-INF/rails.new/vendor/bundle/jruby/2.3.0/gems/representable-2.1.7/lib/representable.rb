require 'forwardable'

require 'representable/inheritable'
require 'representable/config'
require 'representable/definition'
require 'representable/mapper'
require 'representable/for_collection'
require 'representable/represent'
require 'representable/declarative'
require 'representable/apply'


require 'uber/callable'
require 'representable/pipeline'

module Representable
  attr_writer :representable_attrs

  def self.included(base)
    base.class_eval do
      extend Declarative
      extend ClassInclusions, ModuleExtensions
      extend ClassMethods
      extend Feature
      extend ForCollection
      extend Represent
      extend Apply
      # register_feature Representable # Representable gets included automatically when creating inline representer.
    end
  end

private
  # Reads values from +doc+ and sets properties accordingly.
  def update_properties_from(doc, options, format)
    # deserialize_for(bindings, mapper ? , options)
    representable_mapper(format, options).deserialize(doc, options)
  end

  # Compiles the document going through all properties.
  def create_representation_with(doc, options, format)
    representable_mapper(format, options).serialize(doc, options)
  end

  def representable_bindings_for(format, options)
    options = cleanup_options(options)  # FIXME: make representable-options and user-options  two different hashes.

    representable_attrs.collect {|attr| representable_binding_for(attr, format, options) }
    # representable_attrs.binding_cache[format] ||= representable_attrs.collect {|attr| representable_binding_for(attr, format, options) }
  end

  def representable_binding_for(attribute, format, options)
    format.build(attribute, represented, self, options)
  end

  def cleanup_options(options) # TODO: remove me. this clearly belongs in Representable.
    options.reject { |k,v| [:include, :exclude].include?(k) }
  end

  def representable_attrs
    @representable_attrs ||= self.class.representable_attrs # DISCUSS: copy, or better not? what about "freezing"?
  end

  def representable_mapper(format, options)
    bindings = representable_bindings_for(format, options)
    Mapper.new(bindings, represented, options) # TODO: remove self, or do we need it? and also represented!
  end

  def representation_wrap(*args)
    representable_attrs.wrap_for(self.class.name, represented, *args)
  end

  def represented
    self
  end

  module ClassInclusions
    def included(base)
      super
      base.inherit_module!(self)
    end

    def inherited(base) # DISCUSS: this could be in Decorator? but then we couldn't do B < A(include X) for non-decorators, right?
      super
      base.representable_attrs.inherit!(representable_attrs) # this should be inherit_class!
    end
  end

  module ModuleExtensions
    # Copies the representable_attrs reference to the extended object.
    # Note that changing attrs in the instance will affect the class configuration.
    def extended(object)
      super
      object.representable_attrs=(representable_attrs) # yes, we want a hard overwrite here and no inheritance.
    end
  end


  module ClassMethods
    # Gets overridden by Decorator as inheriting representers via include in Decorator means a bit more work (manifesting).
    def inherit_module!(parent)
      representable_attrs.inherit!(parent.representable_attrs) # Module just inherits.
    end

    def prepare(represented)
      represented.extend(self)
    end
  end


  module Feature
    def feature(*mods)
      mods.each do |mod|
        include mod
        register_feature(mod)
      end
    end

  private
    def register_feature(mod)
      representable_attrs[:features][mod] = true
    end
  end
end

require 'representable/autoload'