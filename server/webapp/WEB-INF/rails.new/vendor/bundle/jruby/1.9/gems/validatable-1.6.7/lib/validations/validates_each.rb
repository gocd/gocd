module Validatable
  class ValidatesEach < ValidationBase #:nodoc:
    required_option :logic
  
    def valid?(instance)
      instance.instance_eval(&logic)
      true # return true so no error is added. should look in the future at doing this different.
    end
    
    def message(instance)
      super || "is invalid"
    end
  end
end