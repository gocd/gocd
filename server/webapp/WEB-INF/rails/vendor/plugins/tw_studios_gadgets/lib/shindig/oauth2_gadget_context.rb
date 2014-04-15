module Shindig
  class Oauth2GadgetContext < GadgetContext
    def initialize(shindig_request)
      super()
      @shindig_request = shindig_request
      @st = shindig_request.security_token
    end
    
    def getContainer
      @st.container
    end
    
    def getToken
      @st
    end
    
    def getUrl
      Uri.parse(@st.app_url)
    end
    
    def getIgnoreCache
      @shindig_request.get_ignore_cache
    end
  end
end