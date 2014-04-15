module Validatable
  class ValidatesAcceptanceOf < ValidationBase #:nodoc:
    def valid?(instance)
      instance.send(self.attribute) == "true"
    end
    
    def message(instance)
      super || "must be accepted"
    end
  end
end