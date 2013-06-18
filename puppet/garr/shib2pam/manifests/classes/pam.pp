class shib2pam::pam(
  $sp_hostname   = 'localhost',
  $sess_username = 'uid',
  $sslcheck      = false,
) {
  
  file {
    '/var/www/nss.php' :
	    ensure  => file,
	    owner   => "root",
	    group   => "root",
	    mode    => "644",
	    source  => "puppet:///modules/shib2pam/nss.php";
	    
	  '/lib/security/pam_shibboleth.py' :
      ensure  => file,
      owner   => "root",
      group   => "root",
      mode    => "644",
      source  => "puppet:///modules/shib2pam/pam_shibboleth.py";
      
    '/usr/share/pam-configs/mkhomedir' :
      ensure  => file,
      owner   => "root",
      group   => "root",
      mode    => "644",
      source  => "puppet:///modules/shib2pam/mkhomedir";
	    
    '/usr/share/pam-configs/shibboleth' :
      ensure  => file,
      owner   => "root",
      group   => "root",
      mode    => "644",
	    content => template("shib2pam/shibboleth.erb");
	   
	  '/etc/libnss.conf' :
      ensure  => file,
      owner   => "root",
      group   => "root",
      mode    => "644",
      content => template("shib2pam/libnss.conf.erb"); 
	  
  }
  
}
