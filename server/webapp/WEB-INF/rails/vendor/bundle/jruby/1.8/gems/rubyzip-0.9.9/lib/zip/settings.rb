module Zip
  class << self
    def options
      @options ||= {
        :on_exists_proc => false,
        :continue_on_exists_proc => false
      }
    end
  end
end
