module ExtraSpecAssertions
  def assert_redirected_with_flash(url, msg, flash_class, params = [])
    assert_redirect(url)
    params.each { |param| expect(response.redirect_url).to match(/#{param}/) }
    flash_guid = $1 if response.redirect_url =~ /[?&]fm=([\w-]+)?(&.+){0,}$/
    flash = controller.flash_message_service.get(flash_guid)
    expect(flash.to_s).to eq(msg)
    expect(flash.flashClass()).to eq(flash_class)
  end

  def assert_redirect(url)
    expect(response.status).to eq(302)
    expect(response.redirect_url).to match(%r{#{url}})
  end
end