# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2
  module Provider
    class OauthClient < ModelBase
      
      validates_presence_of :name, :redirect_uri
      validates_format_of :redirect_uri, :with => Regexp.new("^(https|http)://.+$"), :if => proc { |client| !client.redirect_uri.blank? }
      validates_uniqueness_of :name
      
      columns :name, :client_id, :client_secret, :redirect_uri

      def create_token_for_user_id(user_id)
        OauthToken.create!(:user_id => user_id, :oauth_client_id => id)
      end
      
      def create_authorization_for_user_id(user_id)
        oauth_authorizations.each do |authorization|
          authorization.destroy if authorization.user_id == user_id
        end
        OauthAuthorization.create!(:user_id => user_id, :oauth_client_id => id)
      end
      
      def self.model_name
        ActiveSupport::ModelName.new('OauthClient')
      end

      def oauth_tokens
        OauthToken.find_all_with(:oauth_client_id, id)
      end
      
      def oauth_authorizations
        OauthAuthorization.find_all_with(:oauth_client_id, id)
      end
      
      def before_create
        self.client_id = ActiveSupport::SecureRandom.hex(32)
        self.client_secret = ActiveSupport::SecureRandom.hex(32)
      end
      
      def before_destroy
        oauth_tokens.each(&:destroy)
        oauth_authorizations.each(&:destroy)
      end

      def before_save
        self.name.strip! if self.name
        self.redirect_uri.strip! if self.redirect_uri
      end

    end
  end
end

