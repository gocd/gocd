# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  class HttpsRequired < StandardError
  end

  module ApplicationControllerMethods

    def self.included(controller_class)
      controller_class.extend(ClassMethods)
    end
    
    module ClassMethods
      def oauth_allowed(options = {}, &block)
        raise 'options cannot contain both :only and :except' if options[:only] && options[:except]

        [:only, :except].each do |k|
          if values = options[k]
            options[k] = Array(values).map(&:to_s).to_set
          end
        end          
        write_inheritable_attribute(:oauth_options, options)
        write_inheritable_attribute(:oauth_options_proc, block)
      end
    end
    
    protected

    def user_id_for_oauth_access_token
      return nil unless oauth_allowed?

      if looks_like_oauth_request?
        raise HttpsRequired.new("HTTPS is required for OAuth Authorizations") unless request.ssl?
        token = OauthToken.find_one(:access_token, oauth_token_from_request_header)
        token.user_id if (token && !token.expired?)
      end
    end

    def oauth_token_from_request_header
      if request.headers["Authorization"] =~ /Token token="(.*)"/
        return $1
      end
    end

    def looks_like_oauth_request?
      !!oauth_token_from_request_header
    end

    def oauth_allowed?
      oauth_options_proc = self.class.read_inheritable_attribute(:oauth_options_proc)
      oauth_options = self.class.read_inheritable_attribute(:oauth_options)
      if oauth_options_proc && !oauth_options_proc.call(self)
        false
      else
        return false if oauth_options.nil?
        oauth_options.empty? ||
          (oauth_options[:only] && oauth_options[:only].include?(action_name)) ||
          (oauth_options[:except] && !oauth_options[:except].include?(action_name))
      end
    end

  end
end
