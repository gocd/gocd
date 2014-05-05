module Validatable
  class ValidatesFormatOf < ValidationBase #:nodoc:
    required_option :with
  
    def valid?(instance)
      not (instance.send(self.attribute).to_s =~ self.with).nil?
    end
    
    def message(instance)
      super || "is invalid"
    end
  end
end