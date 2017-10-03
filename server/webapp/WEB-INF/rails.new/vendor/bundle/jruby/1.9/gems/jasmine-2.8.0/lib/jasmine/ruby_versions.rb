def ruby_version_less_than(target_version)
  version_parts = RUBY_VERSION.split('.').map(&:to_i).zip(target_version)

  version_parts.each do |(current_part, target_part)|
    if current_part < target_part
      return true
    end
  end
  false
end

