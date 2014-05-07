require 'stringio'
require 'rack/methodoverride'
require 'rack/mock'

describe Rack::MethodOverride do
  def app
    Rack::Lint.new(Rack::MethodOverride.new(lambda {|e|
      [200, {"Content-Type" => "text/plain"}, []]
    }))
  end
  
  should "not affect GET requests" do
    env = Rack::MockRequest.env_for("/?_method=delete", :method => "GET")
    app.call env

    env["REQUEST_METHOD"].should.equal "GET"
  end

  should "modify REQUEST_METHOD for POST requests when _method parameter is set" do
    env = Rack::MockRequest.env_for("/", :method => "POST", :input => "_method=put")
    app.call env

    env["REQUEST_METHOD"].should.equal "PUT"
  end

  should "modify REQUEST_METHOD for POST requests when X-HTTP-Method-Override is set" do
    env = Rack::MockRequest.env_for("/",
            :method => "POST",
            "HTTP_X_HTTP_METHOD_OVERRIDE" => "PATCH"
          )
    app.call env

    env["REQUEST_METHOD"].should.equal "PATCH"
  end

  should "not modify REQUEST_METHOD if the method is unknown" do
    env = Rack::MockRequest.env_for("/", :method => "POST", :input => "_method=foo")
    app.call env

    env["REQUEST_METHOD"].should.equal "POST"
  end

  should "not modify REQUEST_METHOD when _method is nil" do
    env = Rack::MockRequest.env_for("/", :method => "POST", :input => "foo=bar")
    app.call env

    env["REQUEST_METHOD"].should.equal "POST"
  end

  should "store the original REQUEST_METHOD prior to overriding" do
    env = Rack::MockRequest.env_for("/",
            :method => "POST",
            :input  => "_method=options")
    app.call env

    env["rack.methodoverride.original_method"].should.equal "POST"
  end

  should "not modify REQUEST_METHOD when given invalid multipart form data" do
    input = <<EOF
--AaB03x\r
content-disposition: form-data; name="huge"; filename="huge"\r
EOF
    env = Rack::MockRequest.env_for("/",
                      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                      "CONTENT_LENGTH" => input.size.to_s,
                      :method => "POST", :input => input)
    begin
      app.call env
    rescue EOFError
    end

    env["REQUEST_METHOD"].should.equal "POST"
  end
end
