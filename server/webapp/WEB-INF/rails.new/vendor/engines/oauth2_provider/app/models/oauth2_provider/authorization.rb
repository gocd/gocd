# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  class Authorization < Oauth2Provider::ModelBase

    EXPIRY_TIME = 1.hour
    
    self.db_columns = {}  # read this -> http://martinciu.com/2011/07/difference-between-class_inheritable_attribute-and-class_attribute.html
    columns :user_id, :client_id, :code, :expires_at => :integer, :id => :integer

    def client
      Client.find_by_id(:client_id)
    end

    def generate_access_token
      Token.find_all_with(:user_id, user_id).each do |token|
        token.destroy if token.client_id == client_id
      end
      client = Client.find_one(:id, client_id)
      token = client.create_token_for_user_id(user_id)
      self.destroy
      token
    end

    def expires_in
      (Time.at(expires_at.to_i) - Clock.now).to_i
    end

    def expired?
      expires_in <= 0
    end

    def before_create
      self.expires_at = (Clock.now + EXPIRY_TIME).to_i
      self.code = SecureRandom.hex(32)
    end

  end
end