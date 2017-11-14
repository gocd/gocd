module Webpacker::Helper
  # Computes the full path for a given webpacker asset.
  # Return relative path using manifest.json and passes it to asset_url helper
  # This will use asset_path internally, so most of their behaviors will be the same.
  #
  # Example:
  #
  #   <%= asset_pack_path 'calendar.css' %> # => "/packs/calendar-1016838bab065ae1e122.css"
  def asset_pack_path(name, **options)
    asset_path(Webpacker.manifest.lookup!(name), **options)
  end
  # Creates a script tag that references the named pack file, as compiled by Webpack per the entries list
  # in config/webpack/shared.js. By default, this list is auto-generated to match everything in
  # app/javascript/packs/*.js. In production mode, the digested reference is automatically looked up.
  #
  # Example:
  #
  #   <%= javascript_pack_tag 'calendar', 'data-turbolinks-track': 'reload' %> # =>
  #   <script src="/packs/calendar-1016838bab065ae1e314.js" data-turbolinks-track="reload"></script>
  def javascript_pack_tag(*names, **options)
    javascript_include_tag(*sources_from_pack_manifest(names, type: :javascript), **options)
  end

  # Creates a link tag that references the named pack file, as compiled by Webpack per the entries list
  # in config/webpack/shared.js. By default, this list is auto-generated to match everything in
  # app/javascript/packs/*.js. In production mode, the digested reference is automatically looked up.
  #
  # Note: If the development server is running and hot module replacement is active, this will return nothing.
  # In that setup you need to configure your styles to be inlined in your JavaScript for hot reloading.
  #
  # Examples:
  #
  #   # In development mode with hot module replacement:
  #   <%= stylesheet_pack_tag 'calendar', 'data-turbolinks-track': 'reload' %> # =>
  #   nil
  #
  #   # In production mode:
  #   <%= stylesheet_pack_tag 'calendar', 'data-turbolinks-track': 'reload' %> # =>
  #   <link rel="stylesheet" media="screen" href="/packs/calendar-1016838bab065ae1e122.css" data-turbolinks-track="reload" />
  def stylesheet_pack_tag(*names, **options)
    unless Webpacker.dev_server.running? && Webpacker.dev_server.hot_module_replacing?
      stylesheet_link_tag(*sources_from_pack_manifest(names, type: :stylesheet), **options)
    end
  end

  private
    def sources_from_pack_manifest(names, type:)
      names.map { |name| Webpacker.manifest.lookup!(pack_name_with_extension(name, type: type)) }
    end

    def pack_name_with_extension(name, type:)
      "#{name}#{compute_asset_extname(name, type: type)}"
    end
end
