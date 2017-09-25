module Validatable
  class ChildValidation #:nodoc:
    attr_accessor :attribute, :map, :should_validate_proc
    
    def initialize(attribute, map, should_validate_proc)
      @attribute = attribute
      @map = map
      @should_validate_proc = should_validate_proc
    end
    
    def should_validate?(instance)
      instance.instance_eval &should_validate_proc
    end
  end
end