# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  class Client < Oauth2Provider::ModelBase    
    validates_format_of :redirect_uri, :with => Regexp.new("^(https|http)://.+$"), :multiline => true, :if => proc { |client| !client.redirect_uri.blank? }
    validates :redirect_uri, presence: true
    validates :name, presence: true

    self.db_columns = {}  # read this -> http://martinciu.com/2011/07/difference-between-class_inheritable_attribute-and-class_attribute.html
    columns :id, :name, :client_id, :client_secret, :redirect_uri

    def create_token_for_user_id(user_id)
      Token.create!(:user_id => user_id, :client_id => id)
    end

    def create_authorization_for_user_id(user_id)
      authorizations.each do |authorization|
        authorization.destroy if authorization.user_id == user_id
      end
      Oauth2Provider::Authorization.create!(:user_id => user_id, :client_id => id)
    end

    def tokens
      Token.find_all_with(:client_id, id)
    end

    def authorizations
      Authorization.find_all_with(:client_id, id)
    end

    def before_create
      self.client_id = SecureRandom.hex(32)
      self.client_secret = SecureRandom.hex(32)
    end

    def before_destroy
      tokens.each(&:destroy)
      authorizations.each(&:destroy)
    end

    def before_save
      self.name.strip! if self.name
      self.redirect_uri.strip! if self.redirect_uri
    end
  end
end
