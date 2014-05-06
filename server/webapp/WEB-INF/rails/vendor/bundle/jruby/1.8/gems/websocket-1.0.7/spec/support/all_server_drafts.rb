shared_examples_for 'all server drafts' do
  def validate_request
    handshake << client_request

    handshake.error.should be_nil
    handshake.should be_finished
    handshake.should be_valid
    handshake.to_s.should eql(server_response)
  end

  it "should be valid" do
    handshake << client_request

    handshake.error.should be_nil
    handshake.should be_finished
    handshake.should be_valid
  end

  it "should return valid version" do
    handshake << client_request

    handshake.version.should eql(version)
  end

  it "should return valid host" do
    @request_params = { :host => "www.test.cc" }
    handshake << client_request

    handshake.host.should eql('www.test.cc')
  end

  it "should return valid path" do
    @request_params = { :path => "/custom" }
    handshake << client_request

    handshake.path.should eql('/custom')
  end

  it "should return valid query" do
    @request_params = { :path => "/custom?aaa=bbb" }
    handshake << client_request

    handshake.query.should eql('aaa=bbb')
  end

  it "should return valid port" do
    @request_params = { :port => 123 }
    handshake << client_request

    handshake.port.should eql("123")
  end

  it "should return valid response" do
    validate_request
  end

  it "should allow custom path" do
    @request_params = { :path => "/custom" }
    validate_request
  end

  it "should allow query in path" do
    @request_params = { :path => "/custom?test=true" }
    validate_request
  end

  it "should allow custom port" do
    @request_params = { :port => 123 }
    validate_request
  end

  it "should recognize unfinished requests" do
    handshake << client_request[0..-10]

    handshake.should_not be_finished
    handshake.should_not be_valid
  end

  it "should disallow requests with invalid request method" do
    handshake << client_request.gsub('GET', 'POST')

    handshake.should be_finished
    handshake.should_not be_valid
    handshake.error.should eql(:get_request_required)
  end
end
