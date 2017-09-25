class RailsDevTweaks::Railtie < Rails::Railtie

  config.dev_tweaks = RailsDevTweaks::Configuration.new

  config.before_initialize do |app|
    # We can't inspect the stack because it's deferred...  For now, just assume we have it when config.cache_clasess is
    # falsy; which should always be the case in the current version of rails anyway.
    unless app.config.cache_classes
      app.config.middleware.swap ActionDispatch::Reloader, RailsDevTweaks::GranularAutoload::Middleware
    end
  end

end
