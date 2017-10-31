class UrlBuilder
  def method_missing(method, *args)
    Rails.application.routes.url_helpers.send(method, *add_hostname(args))
  end

  def add_hostname(args)
    opts        = args.extract_options! || {}
    opts[:host] = 'test.host'
    [*args, opts]
  end
end