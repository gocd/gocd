# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

require 'ostruct'
module Oauth2
  module Provider
    class InMemoryDatasource
      
      class MyStruct < OpenStruct
        
        attr_accessor :id
        
        def initialize(id, attrs)
          self.id = id
          super(attrs)
        end
      end
      
      @@id = 0
      
      @@oauth_clients = []
      @@oauth_tokens = []
      @@oauth_authorizations = []
      
      def reset
        @@id = 0
        @@oauth_clients = []
        @@oauth_tokens = []
        @@oauth_authorizations = []
      end
      
      def find_oauth_client_by_id(id)
        @@oauth_clients.find{|i| i.id.to_s == id.to_s}
      end

      def find_oauth_client_by_client_id(client_id)
        @@oauth_clients.find{|i| i.client_id.to_s == client_id.to_s}
      end
      
      def find_oauth_client_by_name(name)
        @@oauth_clients.find{|i| i.name == name}
      end

      def find_oauth_client_by_redirect_uri(redirect_uri)
        @@oauth_clients.find{|i| i.redirect_uri == redirect_uri}
      end

      def find_all_oauth_client
        @@oauth_clients
      end

      def save_oauth_client(attrs)
        save(@@oauth_clients, attrs)
      end

      def delete_oauth_client(id)
        @@oauth_clients.delete_if {|i| i.id.to_s == id.to_s}
      end

      def find_all_oauth_authorization_by_oauth_client_id(client_id)
        @@oauth_authorizations.select {|i| i.oauth_client_id.to_s == client_id.to_s}
      end

      def find_oauth_authorization_by_id(id)
        @@oauth_authorizations.find{|i| i.id.to_s == id.to_s}
      end

      def find_oauth_authorization_by_code(code)
        @@oauth_authorizations.find{|i| i.code.to_s == code.to_s}
      end

      def save_oauth_authorization(attrs)
        save(@@oauth_authorizations, attrs)
      end

      def delete_oauth_authorization(id)
        @@oauth_authorizations.delete_if {|i| i.id.to_s == id.to_s}
      end

      def find_oauth_token_by_id(id)
        @@oauth_tokens.find{|i| i.id.to_s == id.to_s}
      end

      def find_all_oauth_token_by_oauth_client_id(client_id)
        @@oauth_tokens.select {|i| i.oauth_client_id.to_s == client_id.to_s}
      end

      def find_all_oauth_token_by_user_id(user_id)
        @@oauth_tokens.select {|i| i.user_id.to_s == user_id.to_s}
      end

      def find_oauth_token_by_access_token(access_token)
        @@oauth_tokens.find {|i| i.access_token.to_s == access_token.to_s}
      end

      def find_oauth_token_by_refresh_token(refresh_token)
        @@oauth_tokens.find {|i| i.refresh_token.to_s == refresh_token.to_s}
      end

      def save_oauth_token(attrs)
        save(@@oauth_tokens, attrs)
      end

      def delete_oauth_token(id)
        @@oauth_tokens.delete_if { |i| i.id.to_s == id .to_s}
      end
      
      private
      def save(collection, attrs)
        dto = collection.find {|i| i.id.to_s == attrs[:id].to_s}
        
        if dto
          attrs.each do |k, v|
            dto.send("#{k}=", v)
          end
        else
          dto = MyStruct.new(next_id, attrs)
          collection << dto
        end
        dto
      end
      
      def next_id
        @@id += 1
        @@id.to_s
      end
    end
  end
end