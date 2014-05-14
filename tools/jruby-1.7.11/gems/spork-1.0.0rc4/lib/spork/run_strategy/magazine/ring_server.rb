# ring_server.rb
require 'rinda/ring'
require './rinda_ring_finger_patch' if RUBY_VERSION > '1.9.1'
require 'rinda/tuplespace'

DRb.start_service

Rinda::RingServer.new(Rinda::TupleSpace.new)
puts "  -- Rinda Ring Server listening for connections...\n\n"
$stdout.flush
DRb.thread.join
