class PreferencesController < ApplicationController
  include ApiV1::AuthenticationHelper

  before_action :check_user_and_401

  layout "single_page_app"

  def notifications
    @view_title = 'Preferences'
  end

end