# This class is inherited by the has_one and belongs_to association classes

module ActiveRecord::Associations::Builder # :nodoc:
  class SingularAssociation < Association #:nodoc:
    def self.valid_options(options)
      super + [:foreign_type, :dependent, :primary_key, :inverse_of, :required]
    end

    def self.define_accessors(model, reflection)
      super
      mixin = model.generated_association_methods
      name = reflection.name

      define_constructors(mixin, name) if reflection.constructable?

      mixin.class_eval <<-CODE, __FILE__, __LINE__ + 1
        def reload_#{name}
          association(:#{name}).force_reload_reader
        end
      CODE
    end

    # Defines the (build|create)_association methods for belongs_to or has_one association
    def self.define_constructors(mixin, name)
      mixin.class_eval <<-CODE, __FILE__, __LINE__ + 1
        def build_#{name}(*args, &block)
          association(:#{name}).build(*args, &block)
        end

        def create_#{name}(*args, &block)
          association(:#{name}).create(*args, &block)
        end

        def create_#{name}!(*args, &block)
          association(:#{name}).create!(*args, &block)
        end
      CODE
    end
  end
end
