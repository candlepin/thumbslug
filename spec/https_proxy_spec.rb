require 'webrick/https'
require 'net/https'
require 'uri'
require 'thumbslug_common'

include WEBrick

describe 'HTTPS proxying' do
  include ThumbslugMethods
  before(:all) do
    @https_proc = create_secure_httpd
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
  end


  it 'validate mocked env' do
    #sleep 60
    filename = Dir.pwd + '/spec/data/random.10k'
    uri = URI.parse('https://127.0.0.1:9443/random.10k')
    https_client = Net::HTTP.new uri.host, uri.port
    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
    https_client.use_ssl = true

    response = https_client.request(Net::HTTP::Get.new(uri.path))

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

#this won't work until tslug supports https
#  it 'pull a file from thumbslug' do
#    #sleep 60
#    filename = Dir.pwd + '/spec/data/random.10k'
#    uri = URI.parse('https://127.0.0.1:8088/random.10k')
#    https_client = Net::HTTP.new uri.host, uri.port
#    https_client.verify_mode = OpenSSL::SSL::VERIFY_NONE
#    https_client.use_ssl = true
#
#    response = https_client.request(Net::HTTP::Get.new(uri.path))
#
#    file_digest = Digest::MD5.hexdigest(File.read(filename))
#    uri_digest = Digest::MD5.hexdigest(response.body)
#
#    #ensure that the file we got is the same as what's on disk
#    uri_digest.should == file_digest
#  end

end
