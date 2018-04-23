class Rails::Controller::Testing::Railtie < Rails::Railtie
  initializer "rails_controller_testing" do
    Rails::Controller::Testing.install
  end
end
