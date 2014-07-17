# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2
  module Provider

    class Clock
  
      def self.fake_now=(time_now)
        @fake_now = time_now
      end
  
      def self.now
        @fake_now || Time.now
      end
  
      def self.reset
        @fake_now = nil
      end
  
    end
  end
end