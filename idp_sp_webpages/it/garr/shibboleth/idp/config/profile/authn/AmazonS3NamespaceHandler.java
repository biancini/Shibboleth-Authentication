package it.garr.shibboleth.idp.config.profile.authn;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

public class AmazonS3NamespaceHandler extends BaseSpringNamespaceHandler {

     /** Namespace URI. */
    public static final String NAMESPACE = "http://garr.it/shibboleth/authn";

    public void init(){
        registerBeanDefinitionParser(AmazonS3BeanDefinitionParser.SCHEMA_TYPE, new AmazonS3BeanDefinitionParser());
    }
}