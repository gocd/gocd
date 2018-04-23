module Validatable 
  class ValidatesPresenceOf < ValidationBase #:nodoc:
    def valid?(instance)
      return false if instance.send(self.attribute).nil?
      instance.send(self.attribute).respond_to?(:strip) ? instance.send(self.attribute).strip.length != 0 : true
    end
    
    def message(instance)
      super || "can't be empty"
    end
  end
end

