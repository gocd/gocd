class RailsDevTweaks::GranularAutoload::Matchers::AssetMatcher

  def call(request)
    route_engine = request.headers['action_dispatch.routes']

    if route_engine.respond_to?(:router)
      mounted_app = journey_find_app(route_engine.router, request)
    else
      mounted_app = rack_find_app(route_engine.set.dup, request)
    end

    # What do we have?
    mounted_app.is_a? Sprockets::Base
  end

  def journey_find_app(router, request)
    router.recognize(request) do |route, *args|
      return route.app
    end
  end

  def rack_find_app(router, request)
    main_mount = router.recognize(request)

    # Unwind until we have an actual app
    while main_mount != nil
      if main_mount.is_a? Array
        main_mount = main_mount.first

      elsif main_mount.is_a? Rack::Mount::Route
        main_mount = main_mount.app

      elsif main_mount.is_a? Rack::Mount::Prefix
        # Bah, no accessor here
        main_mount = main_mount.instance_variable_get(:@app)

      # Well, we got something
      else
        break
      end
    end

    main_mount
  end

end
