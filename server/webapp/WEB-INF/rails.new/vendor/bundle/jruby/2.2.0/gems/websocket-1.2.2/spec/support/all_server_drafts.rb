RSpec.shared_examples_for 'all server drafts' do
  def validate_request
    handshake << client_request

    expect(handshake.error).to be_nil
    expect(handshake).to be_finished
    expect(handshake).to be_valid
    expect(handshake.to_s).to eql(server_response)
  end

  it 'should be valid' do
    handshake << client_request

    expect(handshake.error).to be_nil
    expect(handshake).to be_finished
    expect(handshake).to be_valid
  end

  it 'should return valid version' do
    handshake << client_request

    expect(handshake.version).to eql(version)
  end

  it 'should return valid host' do
    @request_params = { host: 'www.test.cc' }
    handshake << client_request

    expect(handshake.host).to eql('www.test.cc')
  end

  it 'should return valid path' do
    @request_params = { path: '/custom' }
    handshake << client_request

    expect(handshake.path).to eql('/custom')
  end

  it 'should return valid query' do
    @request_params = { path: '/custom?aaa=bbb' }
    handshake << client_request

    expect(handshake.query).to eql('aaa=bbb')
  end

  it 'should return valid port' do
    @request_params = { port: 123 }
    handshake << client_request

    expect(handshake.port).to eql('123')
  end

  it 'should return valid response' do
    validate_request
  end

  it 'should allow custom path' do
    @request_params = { path: '/custom' }
    validate_request
  end

  it 'should allow query in path' do
    @request_params = { path: '/custom?test=true' }
    validate_request
  end

  it 'should allow custom port' do
    @request_params = { port: 123 }
    validate_request
  end

  it 'should recognize unfinished requests' do
    handshake << client_request[0..-10]

    expect(handshake).not_to be_finished
    expect(handshake).not_to be_valid
  end

  it 'should disallow requests with invalid request method' do
    handshake << client_request.gsub('GET', 'POST')

    expect(handshake).to be_finished
    expect(handshake).not_to be_valid
    expect(handshake.error).to eql(:get_request_required)
  end

  it 'should parse a rack request' do
    request = WEBrick::HTTPRequest.new(ServerSoftware: 'rspec')
    expect(request.parse(StringIO.new(client_request))).to be true
    rest    = client_request.slice((request.to_s.length..-1))

    handshake.from_rack(request.meta_vars.merge(
      'rack.input' => StringIO.new(rest)
    ))
    validate_request
  end

  it 'should parse a hash request' do
    request = WEBrick::HTTPRequest.new(ServerSoftware: 'rspec')
    expect(request.parse(StringIO.new(client_request))).to be true
    body = client_request.slice((request.to_s.length..-1))

    path = request.path
    query = request.query_string
    headers = request.header.reduce({}) do |hash, header|
      hash[header[0]] = header[1].first if header[0] && header[1]
      hash
    end

    handshake.from_hash({
      headers: headers,
      path: path,
      query: query,
      body: body
    })

    validate_request
  end
end
