require 'webrick/https'
require 'net/https'
require 'uri'
require 'thumbslug_common'
require 'timeout'

include WEBrick

describe 'HTTPS proxying' do
  include ThumbslugMethods

  around(:each) do |test|
    Timeout::timeout(6) {
      test.run
    }
  end

  before(:all) do
    @https_proc = create_httpd(true)
    @tslugs_proc = create_thumbslug(true)
  end

  after(:all) do
    #do any cleanup
    Process.kill('INT', @https_proc)
    Process.kill('INT', @tslugs_proc)
  end

  it 'validate mocked env' do
    filename = Dir.pwd + '/spec/data/random.10k'
    response = get('https://127.0.0.1:9443/random.10k')

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a file from thumbslug' do
    filename = Dir.pwd + '/spec/data/random.10k'
    response = get('https://127.0.0.1:8443/random.10k')

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a 404 from the cdn' do
    response = get('https://127.0.0.1:8443/this_will_404')
    response.code.should == '404'
  end

  it 'pull a 500 from the cdn' do
    response = get('https://127.0.0.1:8443/this_will_500')
    response.code.should == '500'
  end

  it 'check that client headers are being passed through' do
    response = get('https://127.0.0.1:8443/trace', {'captain' => 'sub'})
    response.code.should == '200'
    header_idx = response.body =~ /sub/
    header_idx.should > 0
  end

end
