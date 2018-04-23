# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  module SslHelper

    def self.included(controller_class)
      controller_class.before_action :mandatory_ssl
    end

    protected
    def mandatory_ssl
      return true if request.ssl?

      if ssl_enabled?
        redirect_to params.merge(ssl_base_url_as_url_options)
      else
        error = 'This page can only be accessed using HTTPS.'
        flash.now[:error] = error
        render(:plain => '', :layout => true, :status => :forbidden)
      end
      false
    end

    private

    def ssl_base_url_as_url_options
      Oauth2Provider::Configuration.ssl_base_url_as_url_options
    end

    def ssl_base_url
      Oauth2Provider::Configuration.ssl_base_url
    end

    def ssl_enabled?
      !ssl_base_url.blank? && ssl_base_url_as_url_options[:protocol] == 'https'
    end
  end
end
