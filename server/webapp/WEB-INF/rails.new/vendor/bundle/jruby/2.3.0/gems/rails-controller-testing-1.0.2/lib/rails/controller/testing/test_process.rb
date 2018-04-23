require 'active_support/core_ext/hash/indifferent_access'

module Rails
  module Controller
    module Testing
      module TestProcess
        def assigns(key = nil)
          assigns = {}.with_indifferent_access
          @controller.view_assigns.each { |k, v| assigns.regular_writer(k, v) }
          key.nil? ? assigns : assigns[key]
        end
      end
    end
  end
end
