require 'spec_helper'

RSpec.describe 'Client draft 11 handshake' do
  let(:handshake) { WebSocket::Handshake::Client.new({ uri: 'ws://example.com/demo', origin: 'http://example.com', version: version }.merge(@request_params || {})) }

  let(:version) { 11 }
  let(:client_request) { client_handshake_11({ key: handshake.handler.send(:key), version: version }.merge(@request_params || {})) }
  let(:server_response) { server_handshake_11({ accept: handshake.handler.send(:accept) }.merge(@request_params || {})) }

  it_should_behave_like 'all client drafts'

  it 'should disallow client with invalid challenge' do
    @request_params = { accept: 'invalid' }
    handshake << server_response

    expect(handshake).to be_finished
    expect(handshake).not_to be_valid
    expect(handshake.error).to eql(:invalid_handshake_authentication)
  end
end
