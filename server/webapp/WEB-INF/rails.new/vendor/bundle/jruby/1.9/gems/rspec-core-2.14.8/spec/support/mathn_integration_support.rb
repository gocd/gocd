require 'support/in_sub_process'

module MathnIntegrationSupport
  include InSubProcess

  def with_mathn_loaded
    in_sub_process do
      require 'mathn'
      yield
    end
  end
end
