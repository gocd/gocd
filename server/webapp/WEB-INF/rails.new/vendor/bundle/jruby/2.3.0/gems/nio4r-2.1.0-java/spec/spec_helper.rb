# frozen_string_literal: true

require "coveralls"
Coveralls.wear!

require "nio"
require "support/selectable_examples"
require "rspec/retry"

RSpec.configure do |config|
  config.disable_monkey_patching!
  config.verbose_retry = true
  config.display_try_failure_messages = true
end

$current_tcp_port = 10_000

def next_available_tcp_port
  loop do
    $current_tcp_port += 1

    begin
      sock = Timeout.timeout(1) { TCPSocket.new("localhost", $current_tcp_port) }
    rescue Errno::ECONNREFUSED
      break $current_tcp_port
    end

    sock.close
  end
end
