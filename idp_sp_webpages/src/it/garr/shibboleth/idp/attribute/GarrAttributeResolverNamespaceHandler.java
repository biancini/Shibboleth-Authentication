package it.garr.shibboleth.idp.attribute;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

public class GarrAttributeResolverNamespaceHandler extends BaseSpringNamespaceHandler{
	public static String NAMESPACE = "urn:garr.it:shibboleth:2.0:resolver";

    public void init() {
        registerBeanDefinitionParser(GarrAttributeResolverBeanDefinitionParser.TYPENAME, new GarrAttributeResolverBeanDefinitionParser());
    }
}
