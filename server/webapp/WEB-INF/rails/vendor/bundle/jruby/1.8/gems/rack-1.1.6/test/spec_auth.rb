require 'rack'

context "Rack::Auth" do
  specify "should have all common authentication schemes" do
    Rack::Auth.schemes.should.include? 'basic'
    Rack::Auth.schemes.should.include? 'digest'
    Rack::Auth.schemes.should.include? 'bearer'
    Rack::Auth.schemes.should.include? 'token'
  end

  specify "should allow registration of new auth schemes" do
    Rack::Auth.schemes.should.not.include "test"
    Rack::Auth.add_scheme "test"
    Rack::Auth.schemes.should.include "test"
  end
end

context "Rack::Auth::AbstractRequest" do
  specify "should symbolize known auth schemes" do
    env = Rack::MockRequest.env_for('/')
    env['HTTP_AUTHORIZATION'] = 'Basic aXJyZXNwb25zaWJsZQ=='
    req = Rack::Auth::AbstractRequest.new(env)
    req.scheme.should.equal :basic


    env['HTTP_AUTHORIZATION'] = 'Digest aXJyZXNwb25zaWJsZQ=='
    req = Rack::Auth::AbstractRequest.new(env)
    req.scheme.should.equal :digest

    env['HTTP_AUTHORIZATION'] = 'Bearer aXJyZXNwb25zaWJsZQ=='
    req = Rack::Auth::AbstractRequest.new(env)
    req.scheme.should.equal :bearer

    env['HTTP_AUTHORIZATION'] = 'MAC aXJyZXNwb25zaWJsZQ=='
    req = Rack::Auth::AbstractRequest.new(env)
    req.scheme.should.equal :mac

    env['HTTP_AUTHORIZATION'] = 'Token aXJyZXNwb25zaWJsZQ=='
    req = Rack::Auth::AbstractRequest.new(env)
    req.scheme.should.equal :token

    env['HTTP_AUTHORIZATION'] = 'OAuth aXJyZXNwb25zaWJsZQ=='
    req = Rack::Auth::AbstractRequest.new(env)
    req.scheme.should.equal :oauth

    env['HTTP_AUTHORIZATION'] = 'OAuth2 aXJyZXNwb25zaWJsZQ=='
    req = Rack::Auth::AbstractRequest.new(env)
    req.scheme.should.equal :oauth2
  end

  specify "should not symbolize unknown auth schemes" do
    env = Rack::MockRequest.env_for('/')
    env['HTTP_AUTHORIZATION'] = 'magic aXJyZXNwb25zaWJsZQ=='
    req = Rack::Auth::AbstractRequest.new(env)
    req.scheme.should == "magic"
  end
end
