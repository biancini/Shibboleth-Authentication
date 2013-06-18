class shib2pam::prerequisites {

  # Install requested packages  
  package { [ 'libpam-python', 'python-dev', 'python-mechanize', 'nscd']:
    ensure => installed,
  }
  
  file { '/lib/i386-linux-gnu/security/pam_pyton.so':
      ensure  => link,
      target  => '/lib/security/pam_python.so',
      owner   => 'root',
      group   => 'root',
      mode    => '0644',
      require => Package['libpam-python'],
  }
  
  file { '/lib/i386-linux-gnu/libnss_shib.so.2':
      ensure  => link,
      target  => '/lib/security/pam_python.so',
      owner   => 'root',
      group   => 'root',
      mode    => '0644',
      source  => "puppet:///modules/shib2pam/lib_shib.so.2";
  }
}
