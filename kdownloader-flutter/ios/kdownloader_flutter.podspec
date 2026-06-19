Pod::Spec.new do |s|
  s.name             = 'kdownloader_flutter'
  s.version          = '2.2.0'
  s.summary          = 'A high-performance KMP download plugin wrapping KDownloader.'
  s.description      = <<-DESC
A high-performance multi-chunk dynamic download plugin wrapping KDownloader.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'RoxyBasicNeedBot' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '13.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'

  # Links the KMP framework built by kdownloader-core
  s.vendored_frameworks = 'kdownloader_core.xcframework'
end
