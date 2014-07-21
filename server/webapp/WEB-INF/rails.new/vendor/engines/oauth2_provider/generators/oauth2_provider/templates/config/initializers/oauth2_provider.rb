# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2
  module Provider

    raise 'OAuth2 provider not configured yet!'
    # please go through the readme and configure this file before you can use this plugin!
    
    # A fix for the stupid "A copy of ApplicationController has been removed from the module tree but is still active!"
    # error message that is caused in rails >= v2.3.3
    #
    # This error is caused because the application controller is unloaded but, the controllers in the plugin are still
    # referring to the super class that is unloaded!
    #
    # Uncommenting these lines fixes the issue, but makes the ApplicationController not reloadable in dev mode.
    #
    # if RAILS_ENV == 'development'
    #   ActiveSupport::Dependencies.load_once_paths << File.join(RAILS_ROOT, 'app/controllers/application_controller')
    # end
    
    # make sure no authentication for OauthTokenController
    OauthTokenController.skip_before_filter(:login_required)
    
    # use host app's custom authorization filter to protect OauthClientsController
    OauthClientsController.before_filter(:ensure_admin_user)
    
    # use host app's custom authorization filter to protect admin actions on OauthUserTokensController
    OauthUserTokensController.before_filter(:ensure_admin_user, :only => [:revoke_by_admin])

  end
end
