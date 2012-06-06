<?php

/**************************************************
Php pages that reads the Shib-Identity-Provider
session variable and redirects to the NSS page
properly installed on the IdP.
**************************************************/

$idp = "".$_SERVER['Shib-Identity-Provider'];
if ($idp != "") {
  $idp = str_replace("http://", "https://", $idp);
  $idp = str_replace("/idp/shibboleth", "/idp/nss", $idp);

  if ($_SERVER['QUERY_STRING'] != null) header('Location: '.$idp.'?'.$_SERVER['QUERY_STRING']);
  else header('Location: '.$idp);
}
?>

