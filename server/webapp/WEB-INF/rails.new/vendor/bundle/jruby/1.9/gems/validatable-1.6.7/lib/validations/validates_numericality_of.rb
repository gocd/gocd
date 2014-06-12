module Validatable 
  class ValidatesNumericalityOf < ValidationBase #:nodoc:
    option :only_integer
    
    def valid?(instance)
      value = instance.send(self.attribute).to_s
      regex = self.only_integer ? /\A[+-]?\d+\Z/ : /^\d*\.{0,1}\d+$/
      not (value =~ regex).nil?
    end
    
    def message(instance)
      super || "must be a number"
    end
  end
end

