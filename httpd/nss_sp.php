<?php
$idp = "".$_SERVER['Shib-Identity-Provider'];
if ($idp != "") {
  $idp = str_replace("http://", "https://", $idp);
  $idp = str_replace("/idp/shibboleth", "idp/nss", $idp);
  header('Location: '.$idp);
}
?>
