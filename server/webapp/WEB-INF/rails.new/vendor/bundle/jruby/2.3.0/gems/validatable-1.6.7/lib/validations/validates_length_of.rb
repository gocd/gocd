module Validatable
  class ValidatesLengthOf < ValidationBase #:nodoc:
    option :minimum, :maximum, :is, :within, :allow_nil
    
    def message(instance)
      super || "is invalid"
    end
    
    def valid?(instance)
      valid = true
      value = instance.send(self.attribute)
      
      if value.nil?
        return true if allow_nil
        value = ""
      end
      
      valid &&= value.length <= maximum unless maximum.nil? 
      valid &&= value.length >= minimum unless minimum.nil?
      valid &&= value.length == is unless is.nil?
      valid &&= within.include?(value.length) unless within.nil?
      valid
    end
  end
end