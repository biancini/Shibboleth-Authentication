<?php
error_reporting(E_ERROR);

require_once('SOAP/Server.php');
require_once('SOAP/Disco.php');

class Backend
{
     var $__dispatch_map = array();

     function Backend() {
      $this->__dispatch_map['oncall'] =
        array('in' => array('loggeduser' => 'string'),
             'out' => array('salutation' => 'string'));
     }

     function oncall($loggeduser)  {
       return "Hello ".$loggeduser." from webservice!";
     }
}

$server = new SOAP_Server();
$webservice = new Backend();
$server->addObjectMap($webservice, 'http://schemas.xmlsoap.org/soap/envelope/');

if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD']=='POST') {
     $server->service($HTTP_RAW_POST_DATA);
} else {
     $disco = new SOAP_DISCO_Server($server,'Sales');
     header("Content-type: text/xml");
     if (isset($_SERVER['QUERY_STRING']) && strcasecmp($_SERVER['QUERY_STRING'], 'wsdl') == 0) {
         echo $disco->getWSDL();
     } else {
         echo $disco->getDISCO();
     }
}

exit;
?>

