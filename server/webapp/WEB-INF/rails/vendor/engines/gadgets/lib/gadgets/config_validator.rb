module Gadgets
  module ConfigValidator
    
    def self.included(controller_class)
      controller_class.before_filter :validate_gadget_configuration unless ENV['DISABLE_GADGET_SSL']
    end

    protected
    def validate_gadget_configuration
      unless Gadgets::Configuration.valid?
        error = Gadgets::Configuration.urls_not_configured_property_message
        flash.now[:error] = error
        render(:text => '', :layout => true, :status => :forbidden)
        return false
      end
      true
    end
  end
end