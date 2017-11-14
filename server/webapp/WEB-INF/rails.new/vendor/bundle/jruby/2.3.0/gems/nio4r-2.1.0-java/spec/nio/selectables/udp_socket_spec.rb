# frozen_string_literal: true

require "spec_helper"

RSpec.describe UDPSocket do
  let(:udp_port) { 23_456 }

  let :readable_subject do
    sock = UDPSocket.new
    sock.bind("localhost", udp_port)

    peer = UDPSocket.new
    peer.send("hi there", 0, "localhost", udp_port)

    sock
  end

  let :unreadable_subject do
    sock = UDPSocket.new
    sock.bind("localhost", udp_port + 1)
    sock
  end

  let :writable_subject do
    pending "come up with a writable UDPSocket example"
  end

  let :unwritable_subject do
    pending "come up with a UDPSocket that's blocked on writing"
  end

  it_behaves_like "an NIO selectable"
end
