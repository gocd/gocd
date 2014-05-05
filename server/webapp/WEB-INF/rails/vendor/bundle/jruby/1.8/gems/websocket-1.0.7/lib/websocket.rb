# WebSocket protocol implementation in Ruby
# This module does not provide a WebSocket server or client, but is made for using
# in http servers or clients to provide WebSocket support.
# @author Bernard "Imanel" Potocki
# @see http://github.com/imanel/websocket-ruby main repository
module WebSocket
  class Error < RuntimeError; end

  # Default WebSocket version to use
  DEFAULT_VERSION = 13
  ROOT = File.expand_path(File.dirname(__FILE__))

  autoload :Frame,     "#{ROOT}/websocket/frame"
  autoload :Handler,   "#{ROOT}/websocket/handler"
  autoload :Handshake, "#{ROOT}/websocket/handshake"

  # Limit of frame size payload in bytes
  def self.max_frame_size
    @max_frame_size ||= 20 * 1024 * 1024 # 20MB
  end

  # Set limit of frame size payload in bytes
  def self.max_frame_size=(val)
    @max_frame_size = val
  end

end

# Try loading websocket-native if available
begin
  require "websocket-native"
rescue LoadError
end
