A request/response rewriting HTTP proxy. A Rack app. Subclass `Rack::Proxy` and provide your `rewrite_env` and `rewrite_response` methods.

Installation
-------

Add the following to your Gemfile:

```
gem 'rack-proxy', '~> 0.6.2'
```

Or install:

```
gem install rack-proxy
```

Example
-------

```ruby
class Foo < Rack::Proxy

  def rewrite_env(env)
    env["HTTP_HOST"] = "example.com"

    env
  end

  def rewrite_response(triplet)
    status, headers, body = triplet

    headers["X-Foo"] = "Bar"

    triplet
  end

end
```

### Disable SSL session verification when proxying a server with e.g. self-signed SSL certs

```ruby
class TrustingProxy < Rack::Proxy

  def rewrite_env(env)
    env["rack.ssl_verify_none"] = true

    env
  end

end
```

The same can be achieved for *all* requests going through the `Rack::Proxy` instance by using

```ruby
Rack::Proxy.new(ssl_verify_none: true)
```

Using it as a middleware:
-------------------------

Example: Proxying only requests that end with ".php" could be done like this:

```ruby
require 'rack/proxy'
class RackPhpProxy < Rack::Proxy

  def perform_request(env)
    request = Rack::Request.new(env)
    if request.path =~ %r{\.php}
      env["HTTP_HOST"] = "localhost"
      env["REQUEST_PATH"] = "/php/#{request.fullpath}"
      super(env)
    else
      @app.call(env)
    end
  end
end
```

To use the middleware, please consider the following:

1) For Rails we could add a configuration in config/application.rb

```ruby
  config.middleware.use RackPhpProxy, {ssl_verify_none: true}
```

2) For Sinatra or any Rack-based application:

```ruby
class MyAwesomeSinatra < Sinatra::Base
   use  RackPhpProxy, {ssl_verify_none: true}
end
```

This will allow to run the other requests through the application and only proxy the requests that match the condition from the middleware.

See tests for more examples.

WARNING
-------

Doesn't work with fakeweb/webmock. Both libraries monkey-patch net/http code.

Todos
-----

-	Make the docs up to date with the current use case for this code: everything except streaming which involved a rather ugly monkey patch and only worked in 1.8, but does not work now.
