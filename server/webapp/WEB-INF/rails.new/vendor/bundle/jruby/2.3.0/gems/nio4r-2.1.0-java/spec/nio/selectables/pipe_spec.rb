# frozen_string_literal: true

require "spec_helper"

RSpec.describe "IO.pipe" do
  let(:pair) { IO.pipe }

  let :unreadable_subject do
    pair.first
  end

  let :readable_subject do
    pipe, peer = pair
    peer << "data"
    pipe
  end

  let :writable_subject do
    pair.last
  end

  let :unwritable_subject do
    _reader, pipe = pair

    # HACK: On OS X 10.8, this str must be larger than PIPE_BUF. Otherwise,
    #      the write is atomic and select() will return writable but write()
    #      will throw EAGAIN if there is too little space to write the string
    # TODO: Use FFI to lookup the platform-specific size of PIPE_BUF
    str = "JUNK IN THE TUBES" * 10_000
    begin
      pipe.write_nonblock str
      _, writers = select [], [pipe], [], 0
    rescue Errno::EPIPE
      break
    end while writers && writers.include?(pipe)

    pipe
  end

  it_behaves_like "an NIO selectable"
  it_behaves_like "an NIO selectable stream"
end
