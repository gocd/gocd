# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  class Token < Oauth2Provider::ModelBase

    self.db_columns = {}  # read this -> http://martinciu.com/2011/07/difference-between-class_inheritable_attribute-and-class_attribute.html
    columns :user_id, :client_id, :access_token, :refresh_token, :expires_at => :integer, :id => :integer

    EXPIRY_TIME = 90.days

    def oauth_client
      Client.find_by_id(client_id)
    end

    def access_token_attributes
      {:access_token => access_token, :expires_in => expires_in, :refresh_token => refresh_token}
    end

    def expires_in
      (Time.at(expires_at.to_i) - Clock.now).to_i
    end

    def expired?
      expires_in <= 0
    end

    def refresh
      self.destroy
      oauth_client.create_token_for_user_id(user_id)
    end

    def before_create
      self.access_token = SecureRandom.hex(32)
      self.expires_at = (Clock.now + EXPIRY_TIME).to_i
      self.refresh_token = SecureRandom.hex(32)
    end
  end
end
