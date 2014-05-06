require 'test/spec'
require 'stringio'

require 'rack/request'
require 'rack/mock'

context "Rack::Request" do
  specify "wraps the rack variables" do
    req = Rack::Request.new(Rack::MockRequest.env_for("http://example.com:8080/"))

    req.body.should.respond_to? :gets
    req.scheme.should.equal "http"
    req.request_method.should.equal "GET"

    req.should.be.get
    req.should.not.be.post
    req.should.not.be.put
    req.should.not.be.delete
    req.should.not.be.head

    req.script_name.should.equal ""
    req.path_info.should.equal "/"
    req.query_string.should.equal ""

    req.host.should.equal "example.com"
    req.port.should.equal 8080

    req.content_length.should.equal "0"
    req.content_type.should.be.nil
  end

  specify "can figure out the correct host" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/", "HTTP_HOST" => "www2.example.org")
    req.host.should.equal "www2.example.org"

    req = Rack::Request.new \
      Rack::MockRequest.env_for("/", "SERVER_NAME" => "example.org", "SERVER_PORT" => "9292")
    req.host.should.equal "example.org"

    req = Rack::Request.new \
      Rack::MockRequest.env_for("/", "HTTP_HOST" => "localhost:81", "HTTP_X_FORWARDED_HOST" => "example.org:9292")
    req.host.should.equal "example.org"

    env = Rack::MockRequest.env_for("/", "SERVER_ADDR" => "192.168.1.1", "SERVER_PORT" => "9292")
    env.delete("SERVER_NAME")
    req = Rack::Request.new(env)
    req.host.should.equal "192.168.1.1"

    env = Rack::MockRequest.env_for("/")
    env.delete("SERVER_NAME")
    req = Rack::Request.new(env)
    req.host.should.equal ""
  end

  specify "can parse the query string" do
    req = Rack::Request.new(Rack::MockRequest.env_for("/?foo=bar&quux=bla"))
    req.query_string.should.equal "foo=bar&quux=bla"
    req.GET.should.equal "foo" => "bar", "quux" => "bla"
    req.POST.should.be.empty
    req.params.should.equal "foo" => "bar", "quux" => "bla"
  end

  specify "raises if rack.input is missing" do
    req = Rack::Request.new({})
    lambda { req.POST }.should.raise(RuntimeError)
  end

  specify "can parse POST data when method is POST and no Content-Type given" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/?foo=quux",
        "REQUEST_METHOD" => 'POST',
        :input => "foo=bar&quux=bla")
    req.content_type.should.be.nil
    req.media_type.should.be.nil
    req.query_string.should.equal "foo=quux"
    req.GET.should.equal "foo" => "quux"
    req.POST.should.equal "foo" => "bar", "quux" => "bla"
    req.params.should.equal "foo" => "bar", "quux" => "bla"
  end

  specify "limit the keys from the GET query string" do
    env = Rack::MockRequest.env_for("/?foo=bar")

    old, Rack::Utils.key_space_limit = Rack::Utils.key_space_limit, 1
    begin
      req = Rack::Request.new(env)
      lambda { req.GET }.should.raise(RangeError)
    ensure
      Rack::Utils.key_space_limit = old
    end
  end

  specify "limit the keys from the POST form data" do
    env = Rack::MockRequest.env_for("",
            "REQUEST_METHOD" => 'POST',
            :input => "foo=bar&quux=bla")

    old, Rack::Utils.key_space_limit = Rack::Utils.key_space_limit, 1
    begin
      req = Rack::Request.new(env)
      lambda { req.POST }.should.raise(RangeError)
    ensure
      Rack::Utils.key_space_limit = old
    end
  end

  specify "can parse POST data with explicit content type regardless of method" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/",
        "CONTENT_TYPE" => 'application/x-www-form-urlencoded;foo=bar',
        :input => "foo=bar&quux=bla")
    req.content_type.should.equal 'application/x-www-form-urlencoded;foo=bar'
    req.media_type.should.equal 'application/x-www-form-urlencoded'
    req.media_type_params['foo'].should.equal 'bar'
    req.POST.should.equal "foo" => "bar", "quux" => "bla"
    req.params.should.equal "foo" => "bar", "quux" => "bla"
  end

  specify "does not parse POST data when media type is not form-data" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/?foo=quux",
        "REQUEST_METHOD" => 'POST',
        "CONTENT_TYPE" => 'text/plain;charset=utf-8',
        :input => "foo=bar&quux=bla")
    req.content_type.should.equal 'text/plain;charset=utf-8'
    req.media_type.should.equal 'text/plain'
    req.media_type_params['charset'].should.equal 'utf-8'
    req.POST.should.be.empty
    req.params.should.equal "foo" => "quux"
    req.body.read.should.equal "foo=bar&quux=bla"
  end

  specify "can parse POST data on PUT when media type is form-data" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/?foo=quux",
        "REQUEST_METHOD" => 'PUT',
        "CONTENT_TYPE" => 'application/x-www-form-urlencoded',
        :input => "foo=bar&quux=bla")
    req.POST.should.equal "foo" => "bar", "quux" => "bla"
    req.body.read.should.equal "foo=bar&quux=bla"
  end

  specify "rewinds input after parsing POST data" do
    input = StringIO.new("foo=bar&quux=bla")
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/",
        "CONTENT_TYPE" => 'application/x-www-form-urlencoded;foo=bar',
        :input => input)
    req.params.should.equal "foo" => "bar", "quux" => "bla"
    input.read.should.equal "foo=bar&quux=bla"
  end

  specify "cleans up Safari's ajax POST body" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/",
        'REQUEST_METHOD' => 'POST', :input => "foo=bar&quux=bla\0")
    req.POST.should.equal "foo" => "bar", "quux" => "bla"
  end

  specify "can get value by key from params with #[]" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("?foo=quux")
    req['foo'].should.equal 'quux'
    req[:foo].should.equal 'quux'
  end

  specify "can set value to key on params with #[]=" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("?foo=duh")
    req['foo'].should.equal 'duh'
    req[:foo].should.equal 'duh'
    req.params.should.equal 'foo' => 'duh'

    req['foo'] = 'bar'
    req.params.should.equal 'foo' => 'bar'
    req['foo'].should.equal 'bar'
    req[:foo].should.equal 'bar'

    req[:foo] = 'jaz'
    req.params.should.equal 'foo' => 'jaz'
    req['foo'].should.equal 'jaz'
    req[:foo].should.equal 'jaz'
  end

  specify "values_at answers values by keys in order given" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("?foo=baz&wun=der&bar=ful")
    req.values_at('foo').should.equal ['baz']
    req.values_at('foo', 'wun').should.equal ['baz', 'der']
    req.values_at('bar', 'foo', 'wun').should.equal ['ful', 'baz', 'der']
  end

  specify "referrer should be extracted correct" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/", "HTTP_REFERER" => "/some/path")
    req.referer.should.equal "/some/path"

    req = Rack::Request.new \
      Rack::MockRequest.env_for("/")
    req.referer.should.equal "/"
  end

  specify "user agent should be extracted correct" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/", "HTTP_USER_AGENT" => "Mozilla/4.0 (compatible)")
    req.user_agent.should.equal "Mozilla/4.0 (compatible)"

    req = Rack::Request.new \
      Rack::MockRequest.env_for("/")
    req.user_agent.should.equal nil
  end

  specify "can cache, but invalidates the cache" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/?foo=quux",
        "CONTENT_TYPE" => "application/x-www-form-urlencoded",
        :input => "foo=bar&quux=bla")
    req.GET.should.equal "foo" => "quux"
    req.GET.should.equal "foo" => "quux"
    req.env["QUERY_STRING"] = "bla=foo"
    req.GET.should.equal "bla" => "foo"
    req.GET.should.equal "bla" => "foo"

    req.POST.should.equal "foo" => "bar", "quux" => "bla"
    req.POST.should.equal "foo" => "bar", "quux" => "bla"
    req.env["rack.input"] = StringIO.new("foo=bla&quux=bar")
    req.POST.should.equal "foo" => "bla", "quux" => "bar"
    req.POST.should.equal "foo" => "bla", "quux" => "bar"
  end

  specify "can figure out if called via XHR" do
    req = Rack::Request.new(Rack::MockRequest.env_for(""))
    req.should.not.be.xhr

    req = Rack::Request.new \
      Rack::MockRequest.env_for("", "HTTP_X_REQUESTED_WITH" => "XMLHttpRequest")
    req.should.be.xhr
  end

  specify "can parse cookies" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("", "HTTP_COOKIE" => "foo=bar;quux=h&m")
    req.cookies.should.equal "foo" => "bar", "quux" => "h&m"
    req.cookies.should.equal "foo" => "bar", "quux" => "h&m"
    req.env.delete("HTTP_COOKIE")
    req.cookies.should.equal({})
  end

  specify "parses cookies according to RFC 2109" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for('', 'HTTP_COOKIE' => 'foo=bar;foo=car')
    req.cookies.should.equal 'foo' => 'bar'
  end

  specify "provides setters" do
    req = Rack::Request.new(e=Rack::MockRequest.env_for(""))
    req.script_name.should.equal ""
    req.script_name = "/foo"
    req.script_name.should.equal "/foo"
    e["SCRIPT_NAME"].should.equal "/foo"

    req.path_info.should.equal "/"
    req.path_info = "/foo"
    req.path_info.should.equal "/foo"
    e["PATH_INFO"].should.equal "/foo"
  end

  specify "provides the original env" do
    req = Rack::Request.new(e=Rack::MockRequest.env_for(""))
    req.env.should.be e
  end

  specify "can restore the URL" do
    Rack::Request.new(Rack::MockRequest.env_for("")).url.
      should.equal "http://example.org/"
    Rack::Request.new(Rack::MockRequest.env_for("", "SCRIPT_NAME" => "/foo")).url.
      should.equal "http://example.org/foo/"
    Rack::Request.new(Rack::MockRequest.env_for("/foo")).url.
      should.equal "http://example.org/foo"
    Rack::Request.new(Rack::MockRequest.env_for("?foo")).url.
      should.equal "http://example.org/?foo"
    Rack::Request.new(Rack::MockRequest.env_for("http://example.org:8080/")).url.
      should.equal "http://example.org:8080/"
    Rack::Request.new(Rack::MockRequest.env_for("https://example.org/")).url.
      should.equal "https://example.org/"

    Rack::Request.new(Rack::MockRequest.env_for("https://example.com:8080/foo?foo")).url.
      should.equal "https://example.com:8080/foo?foo"
  end

  specify "can restore the full path" do
    Rack::Request.new(Rack::MockRequest.env_for("")).fullpath.
      should.equal "/"
    Rack::Request.new(Rack::MockRequest.env_for("", "SCRIPT_NAME" => "/foo")).fullpath.
      should.equal "/foo/"
    Rack::Request.new(Rack::MockRequest.env_for("/foo")).fullpath.
      should.equal "/foo"
    Rack::Request.new(Rack::MockRequest.env_for("?foo")).fullpath.
      should.equal "/?foo"
    Rack::Request.new(Rack::MockRequest.env_for("http://example.org:8080/")).fullpath.
      should.equal "/"
    Rack::Request.new(Rack::MockRequest.env_for("https://example.org/")).fullpath.
      should.equal "/"

    Rack::Request.new(Rack::MockRequest.env_for("https://example.com:8080/foo?foo")).fullpath.
      should.equal "/foo?foo"
  end

  specify "can handle multiple media type parameters" do
    req = Rack::Request.new \
      Rack::MockRequest.env_for("/",
        "CONTENT_TYPE" => 'text/plain; foo=BAR,baz=bizzle dizzle;BLING=bam')
      req.should.not.be.form_data
      req.media_type_params.should.include 'foo'
      req.media_type_params['foo'].should.equal 'BAR'
      req.media_type_params.should.include 'baz'
      req.media_type_params['baz'].should.equal 'bizzle dizzle'
      req.media_type_params.should.not.include 'BLING'
      req.media_type_params.should.include 'bling'
      req.media_type_params['bling'].should.equal 'bam'
  end

  specify "raise RangeError if the key space is exhausted" do
    input = <<EOF
--AaB03x\r
Content-Disposition: form-data; name="text"\r
Content-Type: text/plain; charset=US-ASCII\r
\r
contents\r
--AaB03x--\r
EOF

    env = Rack::MockRequest.env_for("/", 
                      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                      "CONTENT_LENGTH" => input.size,
                      :input => input)

    old, Rack::Utils.key_space_limit = Rack::Utils.key_space_limit, 1
    begin
      lambda { Rack::Utils::Multipart.parse_multipart(env) }.should.raise(RangeError)
    ensure
      Rack::Utils.key_space_limit = old
    end
  end

  specify "can parse multipart form data" do
    # Adapted from RFC 1867.
    input = <<EOF
--AaB03x\r
content-disposition: form-data; name="reply"\r
\r
yes\r
--AaB03x\r
content-disposition: form-data; name="fileupload"; filename="dj.jpg"\r
Content-Type: image/jpeg\r
Content-Transfer-Encoding: base64\r
\r
/9j/4AAQSkZJRgABAQAAAQABAAD//gA+Q1JFQVRPUjogZ2QtanBlZyB2MS4wICh1c2luZyBJSkcg\r
--AaB03x--\r
EOF
    req = Rack::Request.new Rack::MockRequest.env_for("/",
                      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                      "CONTENT_LENGTH" => input.size,
                      :input => input)

    req.POST.should.include "fileupload"
    req.POST.should.include "reply"

    req.should.be.form_data
    req.content_length.should.equal input.size
    req.media_type.should.equal 'multipart/form-data'
    req.media_type_params.should.include 'boundary'
    req.media_type_params['boundary'].should.equal 'AaB03x'

    req.POST["reply"].should.equal "yes"

    f = req.POST["fileupload"]
    f.should.be.kind_of Hash
    f[:type].should.equal "image/jpeg"
    f[:filename].should.equal "dj.jpg"
    f.should.include :tempfile
    f[:tempfile].size.should.equal 76
  end

  specify "can parse big multipart form data" do
    input = <<EOF
--AaB03x\r
content-disposition: form-data; name="huge"; filename="huge"\r
\r
#{"x"*32768}\r
--AaB03x\r
content-disposition: form-data; name="mean"; filename="mean"\r
\r
--AaB03xha\r
--AaB03x--\r
EOF
    req = Rack::Request.new Rack::MockRequest.env_for("/",
                      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                      "CONTENT_LENGTH" => input.size,
                      :input => input)

    req.POST["huge"][:tempfile].size.should.equal 32768
    req.POST["mean"][:tempfile].size.should.equal 10
    req.POST["mean"][:tempfile].read.should.equal "--AaB03xha"
  end

  specify "can detect invalid multipart form data" do
    input = <<EOF
--AaB03x\r
content-disposition: form-data; name="huge"; filename="huge"\r
EOF
    req = Rack::Request.new Rack::MockRequest.env_for("/",
                      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                      "CONTENT_LENGTH" => input.size,
                      :input => input)

    lambda { req.POST }.should.raise(EOFError)

    input = <<EOF
--AaB03x\r
content-disposition: form-data; name="huge"; filename="huge"\r
\r
foo\r
EOF
    req = Rack::Request.new Rack::MockRequest.env_for("/",
                      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                      "CONTENT_LENGTH" => input.size,
                      :input => input)

    lambda { req.POST }.should.raise(EOFError)

    input = <<EOF
--AaB03x\r
content-disposition: form-data; name="huge"; filename="huge"\r
\r
foo\r
EOF
    req = Rack::Request.new Rack::MockRequest.env_for("/",
                      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                      "CONTENT_LENGTH" => input.size,
                      :input => input)

    lambda { req.POST }.should.raise(EOFError)
  end

  specify "shouldn't try to interpret binary as utf8" do
    begin
      original_kcode = $KCODE
      $KCODE='UTF8'

      input = <<EOF
--AaB03x\r
content-disposition: form-data; name="fileupload"; filename="junk.a"\r
content-type: application/octet-stream\r
\r
#{[0x36,0xCF,0x0A,0xF8].pack('c*')}\r
--AaB03x--\r
EOF

      req = Rack::Request.new Rack::MockRequest.env_for("/",
                        "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                        "CONTENT_LENGTH" => input.size,
                        :input => input)

      lambda{req.POST}.should.not.raise(EOFError)
      req.POST["fileupload"][:tempfile].size.should.equal 4
    ensure
      $KCODE = original_kcode
    end
  end


  specify "should work around buggy 1.8.* Tempfile equality" do
    input = <<EOF
--AaB03x\r
content-disposition: form-data; name="huge"; filename="huge"\r
\r
foo\r
--AaB03x--
EOF

    rack_input = Tempfile.new("rackspec")
    rack_input.write(input)
    rack_input.rewind

    req = Rack::Request.new Rack::MockRequest.env_for("/",
                      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
                      "CONTENT_LENGTH" => input.size,
                      :input => rack_input)

    lambda {req.POST}.should.not.raise
    lambda {req.POST}.should.blaming("input re-processed!").not.raise
  end

  specify "does conform to the Rack spec" do
    app = lambda { |env|
      content = Rack::Request.new(env).POST["file"].inspect
      size = content.respond_to?(:bytesize) ? content.bytesize : content.size
      [200, {"Content-Type" => "text/html", "Content-Length" => size.to_s}, [content]]
    }

    input = <<EOF
--AaB03x\r
content-disposition: form-data; name="reply"\r
\r
yes\r
--AaB03x\r
content-disposition: form-data; name="fileupload"; filename="dj.jpg"\r
Content-Type: image/jpeg\r
Content-Transfer-Encoding: base64\r
\r
/9j/4AAQSkZJRgABAQAAAQABAAD//gA+Q1JFQVRPUjogZ2QtanBlZyB2MS4wICh1c2luZyBJSkcg\r
--AaB03x--\r
EOF
    input.force_encoding("ASCII-8BIT") if input.respond_to? :force_encoding
    res = Rack::MockRequest.new(Rack::Lint.new(app)).get "/",
      "CONTENT_TYPE" => "multipart/form-data, boundary=AaB03x",
      "CONTENT_LENGTH" => input.size.to_s, "rack.input" => StringIO.new(input)

    res.should.be.ok
  end

  specify "should parse Accept-Encoding correctly" do
    parser = lambda do |x|
      Rack::Request.new(Rack::MockRequest.env_for("", "HTTP_ACCEPT_ENCODING" => x)).accept_encoding
    end

    parser.call(nil).should.equal([])

    parser.call("compress, gzip").should.equal([["compress", 1.0], ["gzip", 1.0]])
    parser.call("").should.equal([])
    parser.call("*").should.equal([["*", 1.0]])
    parser.call("compress;q=0.5, gzip;q=1.0").should.equal([["compress", 0.5], ["gzip", 1.0]])
    parser.call("gzip;q=1.0, identity; q=0.5, *;q=0").should.equal([["gzip", 1.0], ["identity", 0.5], ["*", 0] ])

    lambda { parser.call("gzip ; q=1.0") }.should.raise(RuntimeError)
  end

  specify 'should provide ip information' do
    app = lambda { |env|
      request = Rack::Request.new(env)
      response = Rack::Response.new
      response.write request.ip
      response.finish
    }

    mock = Rack::MockRequest.new(Rack::Lint.new(app))
    res = mock.get '/', 'REMOTE_ADDR' => '123.123.123.123'
    res.body.should.equal '123.123.123.123'

    res = mock.get '/',
      'REMOTE_ADDR' => '123.123.123.123',
      'HTTP_X_FORWARDED_FOR' => '234.234.234.234'

    res.body.should.equal '234.234.234.234'

    res = mock.get '/',
      'REMOTE_ADDR' => '123.123.123.123',
      'HTTP_X_FORWARDED_FOR' => '234.234.234.234,212.212.212.212'

    res.body.should.equal '212.212.212.212'
  end

  class MyRequest < Rack::Request
    def params
      {:foo => "bar"}
    end
  end

  specify "should allow subclass request to be instantiated after parent request" do
    env = Rack::MockRequest.env_for("/?foo=bar")

    req1 = Rack::Request.new(env)
    req1.GET.should.equal "foo" => "bar"
    req1.params.should.equal "foo" => "bar"

    req2 = MyRequest.new(env)
    req2.GET.should.equal "foo" => "bar"
    req2.params.should.equal :foo => "bar"
  end

  specify "should allow parent request to be instantiated after subclass request" do
    env = Rack::MockRequest.env_for("/?foo=bar")

    req1 = MyRequest.new(env)
    req1.GET.should.equal "foo" => "bar"
    req1.params.should.equal :foo => "bar"

    req2 = Rack::Request.new(env)
    req2.GET.should.equal "foo" => "bar"
    req2.params.should.equal "foo" => "bar"
  end
end
