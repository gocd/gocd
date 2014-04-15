module ActionController
  module Rescue
    def use_rails_error_handling!
      Kernel.warn <<-WARNING
DEPRECATION NOTICE: controller.use_rails_error_handling! is
deprecated and will be removed from a future version of
rspec-rails.

Use rescue_action_in_public!, which is defined directly in
rails' testing framework, instead.
WARNING
      if Rails::VERSION::STRING =~ /^2\.0/
        @use_rails_error_handling = true
      else
        # anything but 0.0.0.0 - borrowed from rails own rescue_action_in_public!
        request.remote_addr = '208.77.188.166'
      end
    end
    
    def use_rails_error_handling?
      @use_rails_error_handling ||= false
    end

  protected
  
    if Rails::VERSION::STRING =~ /^2\.0/
      def rescue_action_in_public?
        request.respond_to?(:rescue_action_in_public?) and request.rescue_action_in_public?
      end
      
      def rescue_action_with_handler_with_fast_errors(exception)
        if (use_rails_error_handling? || rescue_action_in_public?) & !handler_for_rescue(exception)
          rescue_action_in_public(exception)
        else
          rescue_action_with_handler_without_fast_errors(exception)
        end
      end
      alias_method_chain :rescue_action_with_handler, :fast_errors
    end

  end
end
