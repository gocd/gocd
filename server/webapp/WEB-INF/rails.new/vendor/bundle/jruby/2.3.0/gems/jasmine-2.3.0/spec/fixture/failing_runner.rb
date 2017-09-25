class FailingRunner
  def run
    raise
  end
end

Jasmine.configure do |config|
  config.runner = lambda { |_, _| FailingRunner.new }
end
