module ActionCable
  module Connection
    extend ActiveSupport::Autoload

    eager_autoload do
      autoload :Authorization
      autoload :Base
      autoload :ClientSocket
      autoload :Identification
      autoload :InternalChannel
      autoload :MessageBuffer
      autoload :Stream
      autoload :StreamEventLoop
      autoload :Subscriptions
      autoload :TaggedLoggerProxy
      autoload :WebSocket
    end
  end
end
