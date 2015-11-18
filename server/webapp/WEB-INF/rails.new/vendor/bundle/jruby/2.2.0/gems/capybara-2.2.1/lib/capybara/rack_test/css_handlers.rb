class Capybara::RackTest::CSSHandlers
  def disabled list
    list.find_all { |node| node.has_attribute? 'disabled' }
  end        
  def enabled list
    list.find_all { |node| !node.has_attribute? 'disabled' }
  end
end
