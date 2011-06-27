require 'webrick'
require 'net/http'
require 'uri'
require 'thumbslug_common'

include WEBrick

describe 'HTTP proxying' do
  include ThumbslugMethods
  before(:all) do
    @http_proc = create_httpd
    @tslug_proc = create_thumbslug
  end

  after(:all) do
    #clean up what we forked out
    Process.kill('INT', @http_proc)
    Process.kill('KILL', @tslug_proc)
  end


  it 'validate mocked env' do
    filename = Dir.pwd + '/spec/data/random.10k'
    response = get('http://127.0.0.1:9090/random.10k')

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a file from thumbslug' do
    filename = Dir.pwd + '/spec/data/random.10k'
    response = get('http://127.0.0.1:8088/random.10k')

    file_digest = Digest::MD5.hexdigest(File.read(filename))
    uri_digest = Digest::MD5.hexdigest(response.body)

    #ensure that the file we got is the same as what's on disk
    uri_digest.should == file_digest
  end

  it 'pull a 404 from the cdn' do
    response = get('http://127.0.0.1:8088/this_will_404')
    response.code.should == '404'
  end

  it 'pull a 500 from the cdn' do
    response = get('http://127.0.0.1:8088/this_will_500')
    response.code.should == '500'
  end

  it 'check that headers are being passed through' do
    response = get('http://127.0.0.1:8088/trace', {'halifax'=>'sewage'})
    header_idx = response.body =~ /sewage/
    header_idx.should > 0
  end
end
