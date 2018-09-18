# Use the same session store as Java. This is what makes us see the authentication context from Spring for example.
if defined?($servlet_context)
  require 'action_controller/session/java_servlet_store'
  Rails.application.config.session_store :java_servlet_store
end
