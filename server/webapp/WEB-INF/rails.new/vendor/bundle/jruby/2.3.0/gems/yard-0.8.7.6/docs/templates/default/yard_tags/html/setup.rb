def init
  sections :list, [T('docstring')]
end

def tag_signature(tag)
  types = tag.types || []
  signature = "<strong>#{tag_link_name(tag)}</strong> "
  extra = nil
  if sig_tag = tag.object.tag('yard.signature')
    extra = sig_tag.text
  end
  extra = case types.first
  when 'with_name'
    "name description"
  when 'with_types'
    "[Types] description"
  when 'with_types_and_name'
    "name [Types] description"
  when 'with_title_and_text'
    "title\ndescription"
  when 'with_types_and_title'
    "[Types] title\ndescription"
  else
    "description"
  end if extra.nil?
  signature + h(extra).gsub(/\n/, "<br/>&nbsp;&nbsp;&nbsp;")
end