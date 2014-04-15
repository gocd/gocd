if defined?(ActiveRecord)
  module Gadgets

    class ARDatasource

      class GadgetsOauthClientDto < ActiveRecord::Base
        set_table_name :gadgets_oauth_clients
      end

      class OauthAccessTokenDto < ActiveRecord::Base
        set_table_name :oauth_access_tokens
      end

      class OauthAuthorizationCodeDto < ActiveRecord::Base
        set_table_name :oauth_authorization_codes
      end

      # used in tests, use it to clear datasource
      def reset
      end
      
      def transaction(&block)
        ActiveRecord::Base.transaction(&block)
      end
      
      def find_gadgets_oauth_client_by_id(id)
        GadgetsOauthClientDto.find_by_id(id)
      end
      
      def find_all_gadgets_oauth_client
        GadgetsOauthClientDto.all
      end
      
      def find_all_oauth_access_token_by_user_id(user_id)
        OauthAccessTokenDto.find_all_by_user_id(user_id)
      end
      
      def find_all_oauth_authorization_code_by_user_id(user_id)
        OauthAuthorizationCodeDto.find_all_by_user_id(user_id)
      end

      def find_oauth_access_tokens_for_client_and_user_id(gadgets_oauth_client_id, user_id)
        OauthAccessTokenDto.find_by_gadgets_oauth_client_id_and_user_id(gadgets_oauth_client_id, user_id)
      end
      
      def find_all_oauth_access_token_by_gadgets_oauth_client_id(gadget_oauth_client_id)
        OauthAccessTokenDto.find_all_by_gadgets_oauth_client_id(gadget_oauth_client_id)
      end
      
      def find_all_oauth_authorization_code_by_gadgets_oauth_client_id(gadget_oauth_client_id)
        OauthAuthorizationCodeDto.find_all_by_gadgets_oauth_client_id(gadget_oauth_client_id)
      end

      def find_authorization_codes_for_client_and_user_id(gadget_oauth_client_id, user_id)
        OauthAuthorizationCodeDto.find_by_gadgets_oauth_client_id_and_user_id(gadget_oauth_client_id, user_id)
      end

      def find_gadgets_oauth_client_by_service_name(service_name)
        GadgetsOauthClientDto.find_by_service_name(service_name)
      end

      def find_gadgets_oauth_client_by_oauth_authorize_url(oauth_authorize_url)
        GadgetsOauthClientDto.find_by_oauth_authorize_url(oauth_authorize_url)
      end
      
      def delete_gadgets_oauth_client(id)
        GadgetsOauthClientDto.delete(id)
      end
      
      def delete_oauth_authorization_code(id)
        OauthAuthorizationCodeDto.delete(id)
      end
      
      def delete_oauth_access_token(id)
        OauthAccessTokenDto.delete(id)
      end
      
      def save_gadgets_oauth_client(attrs)
        save(GadgetsOauthClientDto, attrs)
      end

      def save_oauth_authorization_code(attrs)
        save(OauthAuthorizationCodeDto, attrs)
      end
      
      def save_oauth_access_token(attrs)
        save(OauthAccessTokenDto, attrs)
      end

      private

      def save(dto_klass, attrs)
        dto = dto_klass.find_by_id(attrs[:id]) unless attrs[:id].blank?
        if dto
          dto.update_attributes(attrs)
        else
          dto = dto_klass.create(attrs)
        end
        dto
      end
    end
  end
end
