module Gadgets
  module SslHelper

    def self.included(controller_class)
      controller_class.before_filter :mandatory_ssl unless ENV['DISABLE_GADGET_SSL']
    end

    protected
    def mandatory_ssl
      if !request.ssl?
        if !ssl_enabled?
          error = 'This page can only be accessed using HTTPS.'
          flash.now[:error] = error
          render(:text => '', :layout => true, :status => :forbidden)
          return false
        else
          redirect_to params.merge(ssl_base_url_as_url_options)
          return false
        end
      end
      true
    end

    private

    def ssl_base_url_as_url_options
      Gadgets::Configuration.ssl_base_url_as_url_options
    end

    def ssl_base_url
      Gadgets::Configuration.ssl_base_url
    end

    def ssl_enabled?
      !ssl_base_url.blank? && ssl_base_url_as_url_options[:protocol] == 'https'
    end

  end
end

