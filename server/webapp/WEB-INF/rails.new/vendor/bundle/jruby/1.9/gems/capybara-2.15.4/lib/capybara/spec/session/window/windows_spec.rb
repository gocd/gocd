# frozen_string_literal: true
Capybara::SpecHelper.spec '#windows', requires: [:windows] do
  before(:each) do
    @window = @session.current_window
    @session.visit('/with_windows')
    @session.find(:css, '#openTwoWindows').click

    @session.document.synchronize(3, errors: [Capybara::CapybaraError]) do
      raise Capybara::CapybaraError if @session.windows.size != 3
    end
  end
  after(:each) do
    (@session.windows - [@window]).each do |w|
      @session.switch_to_window w
      w.close
    end
    @session.switch_to_window(@window)
  end

  it 'should return objects of Capybara::Window class' do
    expect(@session.windows.map { |window| window.instance_of?(Capybara::Window) }).to eq([true] * 3)
  end

  it 'should switchable windows' do
    titles = @session.windows.map do |window|
      @session.within_window(window) { @session.title }
    end
    expect(titles).to match_array([
      'With Windows', 'Title of the first popup', 'Title of popup two'
    ])
  end
end
