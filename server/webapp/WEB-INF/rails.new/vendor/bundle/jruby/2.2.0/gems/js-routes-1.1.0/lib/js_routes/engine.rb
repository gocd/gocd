class JsRoutes
  class Engine < ::Rails::Engine
    JS_ROUTES_ASSET = 'js-routes'

    initializer 'js-routes.dependent_on_routes', after: "sprockets.environment" do
      routes = Rails.root.join('config', 'routes.rb').to_s

      if Rails.application.assets.respond_to?(:register_preprocessor)
        Rails.application.assets.register_preprocessor 'application/javascript', :'js-routes_dependent_on_routes' do |ctx,data|
          ctx.depend_on(routes) if ctx.logical_path == JS_ROUTES_ASSET
          data
        end
      end

      # only sprockets >= 3.0
      if  Rails.application.assets.respond_to?(:depend_on)
        Rails.application.assets.depend_on Rack::Utils.escape("file-digest://#{routes}")
      end
    end
  end
end
