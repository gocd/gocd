# Adding below ENVs because engine-routes need them
ENV['ADMIN_OAUTH_URL_PREFIX'] = "admin"
ENV['LOAD_OAUTH_SILENTLY'] = "yes"

# Load the Rails application.
require File.expand_path('../application', __FILE__)

# Initialize the Rails application.
Go::Application.initialize!
