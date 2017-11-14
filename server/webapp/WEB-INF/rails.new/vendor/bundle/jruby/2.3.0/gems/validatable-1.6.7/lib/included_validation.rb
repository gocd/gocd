module Validatable
  class IncludedValidation #:nodoc:
    attr_accessor :attribute
    
    def initialize(attribute)
      @attribute = attribute
    end
  end
end