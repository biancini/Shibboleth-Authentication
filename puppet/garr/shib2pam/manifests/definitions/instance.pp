define shib2pam::instance (
  $sp_hostname = undef,
  $sess_username = undef,
  $sslcheck = undef,
) {
  stage { 'prerequisites': } -> stage { 'install': } -> Stage['main'] -> stage { 'postinstall': }
  
  # Install prerequisites
  class { 'shib2pam::prerequisites':
    stage      => 'prerequisites',
  }

  # Install and configure Shibboleth PAM and NSS modules
  class { 'shib2pam::pam':
    sp_hostname         => $sp_hostname,
    sess_username       => $sess_username,
    sslcheck            => $sslcheck,
    require             => Class['shib2sp::prerequisites'],
    stage               => 'install',
    notify              => Exec['pam-apache-restart'],
  }

  # Exec to restart apache after installations and configuration
  exec { 'pam-apache-restart':
    command     => "/usr/sbin/service apache2 restart",
    refreshonly => true;
  }
}
