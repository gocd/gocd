##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

require 'spec_helper'

describe MessageVerifier do
  describe :verify do
    before(:each) do
      @public_key = %Q(-----BEGIN RSA PUBLIC KEY-----
                      MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA2fEQ/cRwxjDpyB+LxwjW
                      3YivLQ67Dhfqm0DRYsqCwMnqL/tw08xWaGJa3H6lDbMqgwhEAKk4E9epIrYm1yKQ
                      U6LzkEn7zDZcL29ngoLonTmVZ0YyuTppcPQhnhIqy8NPfAeNp6ac5cBx3E6pGd6R
                      creq3UW+rFeglirU6LdylDQb6o0v74OPLtFaOzrIed2xynqJEDeq0FnhATLjKCHU
                      L8pOClq8AkFs+EssHomiBzICRiIvvlk0cE6a7ZRs1y5xYlUxWAy9canCkSkvEE5u
                      UGfsMNmioktmULZH6Vwd0fuKOhjGz2aa4JiggVV1O0gj1oMg9GXnatlVyVXg8PRu
                      hD1fOkB51yN8Up7TjLg7CcS2BaY8GZdie8HaeDYwgyoCa+ihOaA7jsCrJjMHao6r
                      JVCwue68YHLuJDtxier1ESWt2QSIP8IMIVAguNkwdvcn9sId78WnnMwqD09Hoyr4
                      Bn5Rj/P3lKWJGe5j3suHUAQeiI5nnS/+aSNHa5Uxxq/lpgzd/DFb2eOyib8O5GPY
                      DctbUWGUkQdj+PJY6kvx1BN7KjphdchGwV9N8ebAStfbOSiJIFw/dGPNuYQVpPJy
                      kri6psdhcgjNNxT07txS2eB8DyBYfc8A3oV7eIvX8P80Y84mUduxpBJZ5mW9dBmG
                      HSdbeCUajLTGwhLcHYuhmCcCAwEAAQ==
                      -----END RSA PUBLIC KEY-----)
    end

    it 'should check message authenticity' do
      message = %Q({\n  "latest-version": "15.2.0-2248",\n  "release-time": "2015-07-13 17:52:28 UTC"\n})
      signature = "ZrsZkpPEmXwKuEDZVwi6Me9Sq7t8E3Qt+3wuTMewTNoDRcCL1nfqwcjDhUk7\nD32Tiwl6ZN6J9uRUXcnFWRxrkn/yJe17Y45geO5k+6Rw4UUnN/QmgeNJKPNb\n2W7v/Ex8iagLSWM0CI02yekQmOA3VcJP/Dhpwmsq8hOfVUwWZbtobo020ryW\niFJqaOaGccWmx6c0MiybwuSOGkKRyIkpoCwHf/amXb/S8l4atSLPj/zb/FY3\ndxkmfm4ser56xzas+RmAVjvlUi5NyxZsI4UlPsG3yjy78KZY6fypgBmoQrSs\nAB7rhSkskRHPOyxB0ozNTfph54HVVCbHh2TU6SpdAm2YAWGBQMg9VJBswOB1\nJWSiyANi7CO9P5QJFwWDhuxi3WpY8dG854voH9HXKzR9IUby0UuFaEzD0vTj\n4ceeXzaMp1S599ZHhqoLVI1ksnMA+FEh//j1OvZn74gJhcCLiNY+sYJaCLKf\nVwtgYgEuzYv4KxAuQ0I9wiIKcpQcu0b9LClGnMOKnRjITLRPq8Fidh2ZleU3\nKiI/x733h/UUFsGxpokk0lYSSzlOuE8vlDNDYgOehNN61ewSUUNnferfPxkA\nY5YkQb3SjtyspY3ulDOMWyTw7RCxJtepTu+9I4SsAizrIDc4/iW8QoU4Y6oZ\n05GzWOLu7nofBLzj/joKh7w=\n"

      expect(MessageVerifier.verify(message, signature, @public_key)).to eq(true)
    end

    it 'should be false if message is tampered' do
      message = %Q({\n  "latest-version": "Tampered_Version",\n  "release-time": "2015-07-13 17:52:28 UTC"\n})
      signature = "ZrsZkpPEmXwKuEDZVwi6Me9Sq7t8E3Qt+3wuTMewTNoDRcCL1nfqwcjDhUk7\nD32Tiwl6ZN6J9uRUXcnFWRxrkn/yJe17Y45geO5k+6Rw4UUnN/QmgeNJKPNb\n2W7v/Ex8iagLSWM0CI02yekQmOA3VcJP/Dhpwmsq8hOfVUwWZbtobo020ryW\niFJqaOaGccWmx6c0MiybwuSOGkKRyIkpoCwHf/amXb/S8l4atSLPj/zb/FY3\ndxkmfm4ser56xzas+RmAVjvlUi5NyxZsI4UlPsG3yjy78KZY6fypgBmoQrSs\nAB7rhSkskRHPOyxB0ozNTfph54HVVCbHh2TU6SpdAm2YAWGBQMg9VJBswOB1\nJWSiyANi7CO9P5QJFwWDhuxi3WpY8dG854voH9HXKzR9IUby0UuFaEzD0vTj\n4ceeXzaMp1S599ZHhqoLVI1ksnMA+FEh//j1OvZn74gJhcCLiNY+sYJaCLKf\nVwtgYgEuzYv4KxAuQ0I9wiIKcpQcu0b9LClGnMOKnRjITLRPq8Fidh2ZleU3\nKiI/x733h/UUFsGxpokk0lYSSzlOuE8vlDNDYgOehNN61ewSUUNnferfPxkA\nY5YkQb3SjtyspY3ulDOMWyTw7RCxJtepTu+9I4SsAizrIDc4/iW8QoU4Y6oZ\n05GzWOLu7nofBLzj/joKh7w=\n"

      expect(MessageVerifier.verify(message, signature, @public_key)).to eq(false)
    end

    it 'should be false if signature is tampered' do
      message = %Q({\n  "latest-version": "Tampered_Version",\n  "release-time": "2015-07-13 17:52:28 UTC"\n})
      signature = "bad_signature"

      expect(MessageVerifier.verify(message, signature, @public_key)).to eq(false)
    end
  end
end