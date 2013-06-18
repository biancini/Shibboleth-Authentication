<?php

/**************************************************
Php pages that reads the Shib-Identity-Provider
session variable and redirects to the NSS page
properly installed on the IdP.
**************************************************/

#$idp = "".$_SERVER['Shib-Identity-Provider'];
#if ($idp != "") {
#  $idp = str_replace("http://", "https://", $idp);
#  $idp = str_replace("/idp/shibboleth", "/idp/nss", $idp);
#
#  if ($_SERVER['QUERY_STRING'] != null) header('Location: '.$idp.'?'.$_SERVER['QUERY_STRING']);
#  else header('Location: '.$idp);
#}

if ($_SERVER['QUERY_STRING'] == null || $_SERVER['QUERY_STRING'] == "passwd") {
        print "andrea:x:1001:1001:Andrea Biancini:/home/andrea:/bin/bash\n";
        print "fabio:x:1002:1002:Fabio Farina:/home/fabio:/bin/bash\n";
}
else {
        print "andrea:x:1001:\n";
        print "fabio:x:1002:\n";
}
?>
