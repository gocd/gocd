# Load the Rails application.
require File.expand_path('../application', __FILE__)

ENV["ADMIN_OAUTH_URL_PREFIX"] = "/for-admin/"
ENV["USER_OAUTH_URL_PREFIX"] = "/for-user/"

# Initialize the Rails application.
Dummy::Application.initialize!
