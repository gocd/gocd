RSpec.shared_examples_for 'valid_outgoing_frame' do
  its(:class) { is_expected.to eql(WebSocket::Frame::Outgoing) }
  its(:version) { is_expected.to eql(version) }
  its(:type) { is_expected.to eql(frame_type) }
  its(:data) { is_expected.to eql(decoded_text) }
  its(:to_s) { is_expected.to eql(encoded_text) }

  context 'after parsing' do
    before(:each) { subject.to_s }
    its(:error) { is_expected.to eql(error) }
    its(:require_sending?) { is_expected.to eql(require_sending) }
  end
end
