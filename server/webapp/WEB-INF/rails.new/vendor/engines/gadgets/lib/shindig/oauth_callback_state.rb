module Shindig
  module OauthCallbackState
    def self.encode(oauth_client)
      Base64.encode64({'oauth_authorize_url' => oauth_client.oauth_authorize_url}.to_json)
    end
  
    def self.decode(state_str)
      return nil if state_str.blank?
      state = ActiveSupport::JSON::decode(Base64.decode64(state_str))
      return state['oauth_authorize_url']
    end
  end
end