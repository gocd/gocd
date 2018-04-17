# frozen_string_literal: true
Capybara::SpecHelper.spec '#has_field' do
  before { @session.visit('/form') }

  it "should be true if the field is on the page" do
    expect(@session).to have_field('Dog')
    expect(@session).to have_field('form_description')
    expect(@session).to have_field('Region')
    expect(@session).to have_field(:'Region')
  end

  it "should be false if the field is not on the page" do
    expect(@session).not_to have_field('Monkey')
  end

  context 'with value' do
    it "should be true if a field with the given value is on the page" do
      expect(@session).to have_field('First Name', with: 'John')
      expect(@session).to have_field('First Name', with: /^Joh/)
      expect(@session).to have_field('Phone', with: '+1 555 7021')
      expect(@session).to have_field('Street', with: 'Sesame street 66')
      expect(@session).to have_field('Description', with: 'Descriptive text goes here')
    end

    it "should be false if the given field is not on the page" do
      expect(@session).not_to have_field('First Name', with: 'Peter')
      expect(@session).not_to have_field('First Name', with: /eter$/)
      expect(@session).not_to have_field('Wrong Name', with: 'John')
      expect(@session).not_to have_field('Description', with: 'Monkey')
    end

    it "should be true after the field has been filled in with the given value" do
      @session.fill_in('First Name', with: 'Jonas')
      expect(@session).to have_field('First Name', with: 'Jonas')
      expect(@session).to have_field('First Name', with: /ona/)
    end

    it "should be false after the field has been filled in with a different value" do
      @session.fill_in('First Name', with: 'Jonas')
      expect(@session).not_to have_field('First Name', with: 'John')
      expect(@session).not_to have_field('First Name', with: /John|Paul|George|Ringo/)
    end
  end

  context 'with type' do
    it "should be true if a field with the given type is on the page" do
      expect(@session).to have_field('First Name', type: 'text')
      expect(@session).to have_field('Html5 Email', type: 'email')
      expect(@session).to have_field('Html5 Multiple Email', type: 'email')
      expect(@session).to have_field('Html5 Tel', type: 'tel')
      expect(@session).to have_field('Description', type: 'textarea')
      expect(@session).to have_field('Languages', type: 'select')
    end

    it "should be false if the given field is not on the page" do
      expect(@session).not_to have_field('First Name', type: 'textarea')
      expect(@session).not_to have_field('Html5 Email', type: 'tel')
      expect(@session).not_to have_field('Html5 Multiple Email', type: 'tel')
      expect(@session).not_to have_field('Description', type: '')
      expect(@session).not_to have_field('Description', type: 'email')
      expect(@session).not_to have_field('Languages', type: 'textarea')
    end
  end

  context 'with multiple' do
    it "should be true if a field with the multiple attribute is on the page" do
      expect(@session).to have_field('Html5 Multiple Email', multiple: true)
    end

    it "should be false if a field without the multiple attribute is not on the page" do
      expect(@session).not_to have_field('Html5 Multiple Email', multiple: false)
    end
  end
end

Capybara::SpecHelper.spec '#has_no_field' do
  before { @session.visit('/form') }

  it "should be false if the field is on the page" do
    expect(@session).not_to have_no_field('Dog')
    expect(@session).not_to have_no_field('form_description')
    expect(@session).not_to have_no_field('Region')
  end

  it "should be true if the field is not on the page" do
    expect(@session).to have_no_field('Monkey')
  end

  context 'with value' do
    it "should be false if a field with the given value is on the page" do
      expect(@session).not_to have_no_field('First Name', with: 'John')
      expect(@session).not_to have_no_field('Phone', with: '+1 555 7021')
      expect(@session).not_to have_no_field('Street', with: 'Sesame street 66')
      expect(@session).not_to have_no_field('Description', with: 'Descriptive text goes here')
    end

    it "should be true if the given field is not on the page" do
      expect(@session).to have_no_field('First Name', with: 'Peter')
      expect(@session).to have_no_field('Wrong Name', with: 'John')
      expect(@session).to have_no_field('Description', with: 'Monkey')
    end

    it "should be false after the field has been filled in with the given value" do
      @session.fill_in('First Name', with: 'Jonas')
      expect(@session).not_to have_no_field('First Name', with: 'Jonas')
    end

    it "should be true after the field has been filled in with a different value" do
      @session.fill_in('First Name', with: 'Jonas')
      expect(@session).to have_no_field('First Name', with: 'John')
    end
  end

  context 'with type' do
    it "should be false if a field with the given type is on the page" do
      expect(@session).not_to have_no_field('First Name', type: 'text')
      expect(@session).not_to have_no_field('Html5 Email', type: 'email')
      expect(@session).not_to have_no_field('Html5 Tel', type: 'tel')
      expect(@session).not_to have_no_field('Description', type: 'textarea')
      expect(@session).not_to have_no_field('Languages', type: 'select')
    end

    it "should be true if the given field is not on the page" do
      expect(@session).to have_no_field('First Name', type: 'textarea')
      expect(@session).to have_no_field('Html5 Email', type: 'tel')
      expect(@session).to have_no_field('Description', type: '')
      expect(@session).to have_no_field('Description', type: 'email')
      expect(@session).to have_no_field('Languages', type: 'textarea')
    end
  end
end

Capybara::SpecHelper.spec '#has_checked_field?' do
  before { @session.visit('/form') }

  it "should be true if a checked field is on the page" do
    expect(@session).to have_checked_field('gender_female')
    expect(@session).to have_checked_field('Hamster')
  end

  it "should be true for disabled checkboxes if disabled: true" do
    expect(@session).to have_checked_field('Disabled Checkbox', disabled: true)
  end

  it "should be false if an unchecked field is on the page" do
    expect(@session).not_to have_checked_field('form_pets_cat')
    expect(@session).not_to have_checked_field('Male')
  end

  it "should be false if no field is on the page" do
    expect(@session).not_to have_checked_field('Does Not Exist')
  end

  it "should be false for disabled checkboxes by default" do
    expect(@session).not_to have_checked_field('Disabled Checkbox')
  end

  it "should be false for disabled checkboxes if disabled: false" do
    expect(@session).not_to have_checked_field('Disabled Checkbox', disabled: false)
  end

  it "should be true for disabled checkboxes if disabled: :all" do
    expect(@session).to have_checked_field('Disabled Checkbox', disabled: :all)
  end

  it "should be true for enabled checkboxes if disabled: :all" do
    expect(@session).to have_checked_field('gender_female', disabled: :all)
  end

  it "should be true after an unchecked checkbox is checked" do
    @session.check('form_pets_cat')
    expect(@session).to have_checked_field('form_pets_cat')
  end

  it "should be false after a checked checkbox is unchecked" do
    @session.uncheck('form_pets_dog')
    expect(@session).not_to have_checked_field('form_pets_dog')
  end

  it "should be true after an unchecked radio button is chosen" do
    @session.choose('gender_male')
    expect(@session).to have_checked_field('gender_male')
  end

  it "should be false after another radio button in the group is chosen" do
    @session.choose('gender_male')
    expect(@session).not_to have_checked_field('gender_female')
  end
end

Capybara::SpecHelper.spec '#has_no_checked_field?' do
  before { @session.visit('/form') }

  it "should be false if a checked field is on the page" do
    expect(@session).not_to have_no_checked_field('gender_female')
    expect(@session).not_to have_no_checked_field('Hamster')
  end

  it "should be false for disabled checkboxes if disabled: true" do
    expect(@session).not_to have_no_checked_field('Disabled Checkbox', disabled: true)
  end

  it "should be true if an unchecked field is on the page" do
    expect(@session).to have_no_checked_field('form_pets_cat')
    expect(@session).to have_no_checked_field('Male')
  end

  it "should be true if no field is on the page" do
    expect(@session).to have_no_checked_field('Does Not Exist')
  end

  it "should be true for disabled checkboxes by default" do
    expect(@session).to have_no_checked_field('Disabled Checkbox')
  end

  it "should be true for disabled checkboxes if disabled: false" do
    expect(@session).to have_no_checked_field('Disabled Checkbox', disabled: false)
  end
end

Capybara::SpecHelper.spec '#has_unchecked_field?' do
  before { @session.visit('/form') }

  it "should be false if a checked field is on the page" do
    expect(@session).not_to have_unchecked_field('gender_female')
    expect(@session).not_to have_unchecked_field('Hamster')
  end

  it "should be true if an unchecked field is on the page" do
    expect(@session).to have_unchecked_field('form_pets_cat')
    expect(@session).to have_unchecked_field('Male')
  end

  it "should be true for disabled unchecked fields if disabled: true" do
    expect(@session).to have_unchecked_field('Disabled Unchecked Checkbox', disabled: true)
  end

  it "should be false if no field is on the page" do
    expect(@session).not_to have_unchecked_field('Does Not Exist')
  end

  it "should be false for disabled unchecked fields by default" do
    expect(@session).not_to have_unchecked_field('Disabled Unchecked Checkbox')
  end

  it "should be false for disabled unchecked fields if disabled: false" do
    expect(@session).not_to have_unchecked_field('Disabled Unchecked Checkbox', disabled: false)
  end

  it "should be false after an unchecked checkbox is checked" do
    @session.check('form_pets_cat')
    expect(@session).not_to have_unchecked_field('form_pets_cat')
  end

  it "should be true after a checked checkbox is unchecked" do
    @session.uncheck('form_pets_dog')
    expect(@session).to have_unchecked_field('form_pets_dog')
  end

  it "should be false after an unchecked radio button is chosen" do
    @session.choose('gender_male')
    expect(@session).not_to have_unchecked_field('gender_male')
  end

  it "should be true after another radio button in the group is chosen" do
    @session.choose('gender_male')
    expect(@session).to have_unchecked_field('gender_female')
  end

  it "should support locator-less usage" do
    expect(@session.has_unchecked_field?(disabled: true, id: "form_disabled_unchecked_checkbox"))
    expect(@session).to have_unchecked_field(disabled: true, id: "form_disabled_unchecked_checkbox" )
  end
end

Capybara::SpecHelper.spec '#has_no_unchecked_field?' do
  before { @session.visit('/form') }

  it "should be true if a checked field is on the page" do
    expect(@session).to have_no_unchecked_field('gender_female')
    expect(@session).to have_no_unchecked_field('Hamster')
  end

  it "should be false if an unchecked field is on the page" do
    expect(@session).not_to have_no_unchecked_field('form_pets_cat')
    expect(@session).not_to have_no_unchecked_field('Male')
  end

  it "should be false for disabled unchecked fields if disabled: true" do
    expect(@session).not_to have_no_unchecked_field('Disabled Unchecked Checkbox', disabled: true)
  end

  it "should be true if no field is on the page" do
    expect(@session).to have_no_unchecked_field('Does Not Exist')
  end

  it "should be true for disabled unchecked fields by default" do
    expect(@session).to have_no_unchecked_field('Disabled Unchecked Checkbox')
  end

  it "should be true for disabled unchecked fields if disabled: false" do
    expect(@session).to have_no_unchecked_field('Disabled Unchecked Checkbox', disabled: false)
  end

  it "should support locator-less usage" do
    expect(@session.has_no_unchecked_field?(disabled: false, id: "form_disabled_unchecked_checkbox")).to eq true
    expect(@session).to have_no_unchecked_field(disabled: false, id: "form_disabled_unchecked_checkbox" )
  end
end
