require "rails_helper"

<% module_namespacing do -%>
RSpec.describe <%= class_name %><%= Rails.version.to_f >= 5.0 ? "Mailer" : "" %>, <%= type_metatag(:mailer) %> do
<% for action in actions -%>
  describe "<%= action %>" do
    let(:mail) { <%= class_name %><%= Rails.version.to_f >= 5.0 ? "Mailer" : "" %>.<%= action %> }

    it "renders the headers" do
      expect(mail.subject).to eq(<%= action.to_s.humanize.inspect %>)
      expect(mail.to).to eq(["to@example.org"])
      expect(mail.from).to eq(["from@example.com"])
    end

    it "renders the body" do
      expect(mail.body.encoded).to match("Hi")
    end
  end

<% end -%>
<% if actions.blank? -%>
  pending "add some examples to (or delete) #{__FILE__}"
<% end -%>
end
<% end -%>
