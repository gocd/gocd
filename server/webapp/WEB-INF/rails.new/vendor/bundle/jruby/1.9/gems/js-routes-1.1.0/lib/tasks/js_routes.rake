namespace :js do
  desc "Make a js file that will have functions that will return restful routes/urls."
  task routes: :environment do
    require "js-routes"

    JsRoutes.generate!
  end
end
