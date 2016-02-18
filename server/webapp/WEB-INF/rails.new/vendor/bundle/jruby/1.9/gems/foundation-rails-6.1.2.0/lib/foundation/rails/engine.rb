require 'rails'

module Foundation
  module Rails
    class Engine < ::Rails::Engine
      isolate_namespace Foundation::Rails
    end
  end
end
