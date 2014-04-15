module Validatable
  module ClassMethods #:nodoc:
    
    def validate_children(instance, group)
      self.children_to_validate.each do |child_validation|
        next unless child_validation.should_validate?(instance)
        child = instance.send child_validation.attribute
        if (child.respond_to?(:valid_for_group?))
          child.valid_for_group?(group)
        else
          child.valid?
        end
        child.errors.each do |attribute, messages|
          if messages.is_a?(String)
            add_error(instance, child_validation.map[attribute.to_sym] || attribute, messages)
          else
            messages.each do |message|
              add_error(instance, child_validation.map[attribute.to_sym] || attribute, message)
            end
          end
        end
      end
    end
    
    def all_before_validations
      if self.superclass.respond_to? :all_before_validations
        return before_validations + self.superclass.all_before_validations 
      end
      before_validations
    end
    
    def before_validations
      @before_validations ||= []
    end

    def all_validations
      if self.respond_to?(:superclass) && self.superclass.respond_to?(:all_validations)
        return validations + self.superclass.all_validations 
      end
      validations
    end

    def validations
      @validations ||= []
    end
    
    def add_error(instance, attribute, msg)
      instance.errors.add(attribute, msg)
    end
    
    def validation_keys_include?(key)
      validations.map { |validation| validation.key }.include?(key)
    end
    
    def validations_to_include
      @validations_to_include ||= []
    end
    
    protected
    
    def add_validations(args, klass)
      options = args.last.is_a?(Hash) ? args.pop : {}
      args.each do |attribute|
        new_validation = klass.new self, attribute, options
        self.validations << new_validation
        self.create_valid_method_for_groups new_validation.groups
      end
    end
    
    def create_valid_method_for_groups(groups)
      groups.each do |group|
        self.class_eval do
          define_method "valid_for_#{group}?".to_sym do
            valid_for_group?(group)
          end
        end
      end
    end
    
    def children_to_validate
      @children_to_validate ||= []
    end
    
  end
end