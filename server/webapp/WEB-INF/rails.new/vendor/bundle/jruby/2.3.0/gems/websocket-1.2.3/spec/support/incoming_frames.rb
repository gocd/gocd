RSpec.shared_examples_for 'valid_incoming_frame' do
  let(:decoded_text_array) { Array(decoded_text) }
  let(:frame_type_array) { Array(frame_type) }

  its(:class) { is_expected.to eql(WebSocket::Frame::Incoming) }
  its(:data) { is_expected.to eql(encoded_text || '') }
  its(:version) { is_expected.to eql(version) }
  its(:type) { is_expected.to be_nil }
  its(:decoded?) { is_expected.to be false }
  its(:to_s) { is_expected.to eql(encoded_text || '') }

  it 'should have specified number of returned frames' do
    decoded_text_array.each_with_index do |da, index|
      n = subject.next
      expect(n).not_to be_nil, "Should return frame for #{da}, #{frame_type_array[index]}"
      expect(n.class).to eql(WebSocket::Frame::Incoming), "Should be WebSocket::Frame::Incoming, #{n} received instead"
    end
    expect(subject.next).to be_nil
    if error.is_a?(Class)
      expect(subject.error).to eql(error.new.message)
    else
      expect(subject.error).to eql(error)
    end
  end

  it 'should return valid decoded frame for each specified decoded texts' do
    decoded_text_array.each_with_index do |da, index|
      f = subject.next
      expect(f.decoded?).to be true
      expect(f.type).to eql(frame_type_array[index])
      expect(f.code).to eql(close_code) if defined?(close_code)
      expect(f.to_s).to eql(da)
    end
  end

  context 'with raising' do
    before(:each) { WebSocket.should_raise = true }
    after(:each) { WebSocket.should_raise = false }

    it 'should have specified number of returned frames' do
      expect do
        decoded_text_array.each_with_index do |da, index|
          n = subject.next
          expect(n).not_to be_nil, "Should return frame for #{da}, #{frame_type_array[index]}"
          expect(n.class).to eql(WebSocket::Frame::Incoming), "Should be WebSocket::Frame::Incoming, #{n} received instead"
        end
        expect(subject.next).to be_nil
        if error.is_a?(Class)
          expect(subject.error).to eql(error.new.message)
        else
          expect(subject.error).to eql(error)
        end
      end.to raise_error(error) if error
    end
  end
end
