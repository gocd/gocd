module Gadgets

  class Clock

    @@fake_time_now = nil
    
    def self.fake_now=(fake_now)
      @@fake_time_now = fake_now
    end

    def self.now
      @@fake_time_now || Time.now
    end

    def self.reset!
      @@fake_time_now = nil
    end

  end
end