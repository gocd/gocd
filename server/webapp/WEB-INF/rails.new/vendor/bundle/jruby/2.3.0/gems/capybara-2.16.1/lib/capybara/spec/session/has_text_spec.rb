# frozen_string_literal: true
Capybara::SpecHelper.spec '#has_text?' do
  it "should be true if the given text is on the page at least once" do
    @session.visit('/with_html')
    expect(@session).to have_text('est')
    expect(@session).to have_text('Lorem')
    expect(@session).to have_text('Redirect')
    expect(@session).to have_text(:'Redirect')
  end

  it "should be true if scoped to an element which has the text" do
    @session.visit('/with_html')
    @session.within("//a[@title='awesome title']") do
      expect(@session).to have_text('labore')
    end
  end

  it "should be false if scoped to an element which does not have the text" do
    @session.visit('/with_html')
    @session.within("//a[@title='awesome title']") do
      expect(@session).not_to have_text('monkey')
    end
  end

  it "should ignore tags" do
    @session.visit('/with_html')
    expect(@session).not_to have_text('exercitation <a href="/foo" id="foo">ullamco</a> laboris')
    expect(@session).to have_text('exercitation ullamco laboris')
  end

  it "should ignore extra whitespace and newlines" do
    @session.visit('/with_html')
    expect(@session).to have_text('text with whitespace')
  end

  it "should ignore whitespace and newlines in the search string" do
    @session.visit('/with_html')
    expect(@session).to have_text("text     with \n\n whitespace")
  end

  it "should be false if the given text is not on the page" do
    @session.visit('/with_html')
    expect(@session).not_to have_text('xxxxyzzz')
    expect(@session).not_to have_text('monkey')
  end

  it 'should handle single quotes in the text' do
    @session.visit('/with-quotes')
    expect(@session).to have_text("can't")
  end

  it 'should handle double quotes in the text' do
    @session.visit('/with-quotes')
    expect(@session).to have_text(%q{"No," he said})
  end

  it 'should handle mixed single and double quotes in the text' do
    @session.visit('/with-quotes')
    expect(@session).to have_text(%q{"you can't do that."})
  end

  it 'should be false if text is in the title tag in the head' do
    @session.visit('/with_js')
    expect(@session).not_to have_text('with_js')
  end

  it 'should be false if text is inside a script tag in the body' do
    @session.visit('/with_js')
    expect(@session).not_to have_text('a javascript comment')
    expect(@session).not_to have_text('aVar')
  end

  it "should be false if the given text is on the page but not visible" do
    @session.visit('/with_html')
    expect(@session).not_to have_text('Inside element with hidden ancestor')
  end

  it "should be true if :all given and text is invisible." do
    @session.visit('/with_html')
    expect(@session).to have_text(:all, 'Some of this text is hidden!')
  end

  it "should be true if `Capybara.ignore_hidden_elements = false` and text is invisible." do
    Capybara.ignore_hidden_elements = false
    @session.visit('/with_html')
    expect(@session).to have_text('Some of this text is hidden!')
  end

  it "should be true if the text in the page matches given regexp" do
    @session.visit('/with_html')
    expect(@session).to have_text(/Lorem/)
  end

  it "should be false if the text in the page doesn't match given regexp" do
    @session.visit('/with_html')
    expect(@session).not_to have_text(/xxxxyzzz/)
  end

  context "with exact: true option" do
    it "should be true if text matches exactly" do
      @session.visit('/with_html')
      expect(@session.find(:id, "h2one")).to have_text("Header Class Test One", exact: true)
    end

    it "should be false if text doesn't match exactly" do
      @session.visit('/with_html')
      expect(@session.find(:id, "h2one")).not_to have_text("Header Class Test On", exact: true)
    end
  end

  it "should escape any characters that would have special meaning in a regexp" do
    @session.visit('/with_html')
    expect(@session).not_to have_text('.orem')
  end

  it "should accept non-string parameters" do
    @session.visit('/with_html')
    expect(@session).to have_text(42)
  end

  it "should be true when passed nil" do
    # nil is converted to '' when to_s is invoked
    @session.visit('/with_html')
    expect(@session).to have_text(nil)
  end

  it "should wait for text to appear", requires: [:js] do
    Capybara.using_wait_time(3) do
      @session.visit('/with_js')
      @session.click_link('Click me')
      expect(@session).to have_text("Has been clicked")
    end
  end

  context "with between" do
    it "should be true if the text occurs within the range given" do
      @session.visit('/with_count')
      expect(@session).to have_text('count', between: 1..3)
      expect(@session).to have_text(/count/, between: 2..2)
    end

    it "should be false if the text occurs more or fewer times than range" do
      @session.visit('/with_count')
      expect(@session).not_to have_text('count', between: 0..1)
      expect(@session).not_to have_text('count', between: 3..10)
      expect(@session).not_to have_text(/count/, between: 2...2)
    end
  end

  context "with count" do
    it "should be true if the text occurs the given number of times" do
      @session.visit('/with_count')
      expect(@session).to have_text('count', count: 2)
    end

    it "should be false if the text occurs a different number of times than the given" do
      @session.visit('/with_count')
      expect(@session).not_to have_text('count', count: 0)
      expect(@session).not_to have_text('count', count: 1)
      expect(@session).not_to have_text(/count/, count: 3)
    end

    it "should coerce count to an integer" do
      @session.visit('/with_count')
      expect(@session).to have_text('count', count: '2')
      expect(@session).not_to have_text('count', count: '3')
    end
  end

  context "with maximum" do
    it "should be true when text occurs same or fewer times than given" do
      @session.visit('/with_count')
      expect(@session).to have_text('count', maximum: 2)
      expect(@session).to have_text(/count/, maximum: 3)
    end

    it "should be false when text occurs more times than given" do
      @session.visit('/with_count')
      expect(@session).not_to have_text('count', maximum: 1)
      expect(@session).not_to have_text('count', maximum: 0)
    end

    it "should coerce maximum to an integer" do
      @session.visit('/with_count')
      expect(@session).to have_text('count', maximum: '2')
      expect(@session).not_to have_text('count', maximum: '1')
    end
  end

  context "with minimum" do
    it "should be true when text occurs same or more times than given" do
      @session.visit('/with_count')
      expect(@session).to have_text('count', minimum: 2)
      expect(@session).to have_text(/count/, minimum: 0)
    end

    it "should be false when text occurs fewer times than given" do
      @session.visit('/with_count')
      expect(@session).not_to have_text('count', minimum: 3)
    end

    it "should coerce minimum to an integer" do
      @session.visit('/with_count')
      expect(@session).to have_text('count', minimum: '2')
      expect(@session).not_to have_text('count', minimum: '3')
    end
  end

  context "with wait", requires: [:js] do
    it "should find element if it appears before given wait duration" do
      Capybara.using_wait_time(0.1) do
        @session.visit('/with_js')
        @session.click_link('Click me')
        expect(@session).to have_text('Has been clicked', wait: 0.9)
      end
    end
  end

  it "should raise an error if an invalid option is passed" do
    @session.visit('/with_html')
    expect do
      expect(@session).to have_text('Lorem', invalid: true)
    end.to raise_error(ArgumentError)
  end
end

Capybara::SpecHelper.spec '#has_no_text?' do
  it "should be false if the given text is on the page at least once" do
    @session.visit('/with_html')
    expect(@session).not_to have_no_text('est')
    expect(@session).not_to have_no_text('Lorem')
    expect(@session).not_to have_no_text('Redirect')
  end

  it "should be false if scoped to an element which has the text" do
    @session.visit('/with_html')
    @session.within("//a[@title='awesome title']") do
      expect(@session).not_to have_no_text('labore')
    end
  end

  it "should be true if scoped to an element which does not have the text" do
    @session.visit('/with_html')
    @session.within("//a[@title='awesome title']") do
      expect(@session).to have_no_text('monkey')
    end
  end

  it "should ignore tags" do
    @session.visit('/with_html')
    expect(@session).to have_no_text('exercitation <a href="/foo" id="foo">ullamco</a> laboris')
    expect(@session).not_to have_no_text('exercitation ullamco laboris')
  end

  it "should be true if the given text is not on the page" do
    @session.visit('/with_html')
    expect(@session).to have_no_text('xxxxyzzz')
    expect(@session).to have_no_text('monkey')
  end

  it 'should handle single quotes in the text' do
    @session.visit('/with-quotes')
    expect(@session).not_to have_no_text("can't")
  end

  it 'should handle double quotes in the text' do
    @session.visit('/with-quotes')
    expect(@session).not_to have_no_text(%q{"No," he said})
  end

  it 'should handle mixed single and double quotes in the text' do
    @session.visit('/with-quotes')
    expect(@session).not_to have_no_text(%q{"you can't do that."})
  end

  it 'should be true if text is in the title tag in the head' do
    @session.visit('/with_js')
    expect(@session).to have_no_text('with_js')
  end

  it 'should be true if text is inside a script tag in the body' do
    @session.visit('/with_js')
    expect(@session).to have_no_text('a javascript comment')
    expect(@session).to have_no_text('aVar')
  end

  it "should be true if the given text is on the page but not visible" do
    @session.visit('/with_html')
    expect(@session).to have_no_text('Inside element with hidden ancestor')
  end

  it "should be false if :all given and text is invisible." do
    @session.visit('/with_html')
    expect(@session).not_to have_no_text(:all, 'Some of this text is hidden!')
  end

  it "should be false if `Capybara.ignore_hidden_elements = false` and text is invisible." do
    Capybara.ignore_hidden_elements = false
    @session.visit('/with_html')
    expect(@session).not_to have_no_text('Some of this text is hidden!')
  end

  it "should be true if the text in the page doesn't match given regexp" do
    @session.visit('/with_html')
    expect(@session).to have_no_text(/xxxxyzzz/)
  end

  it "should be false if the text in the page  matches given regexp" do
    @session.visit('/with_html')
    expect(@session).not_to have_no_text(/Lorem/)
  end

  it "should escape any characters that would have special meaning in a regexp" do
    @session.visit('/with_html')
    expect(@session).to have_no_text('.orem')
  end

  it "should wait for text to disappear", requires: [:js] do
    @session.visit('/with_js')
    @session.click_link('Click me')
    expect(@session).to have_no_text("I changed it")
  end

  context "with wait", requires: [:js] do
    it "should not find element if it appears after given wait duration" do
      @session.visit('/with_js')
      @session.click_link('Click me')
      expect(@session).to have_no_text('Has been clicked', wait: 0.1)
    end
  end
end
