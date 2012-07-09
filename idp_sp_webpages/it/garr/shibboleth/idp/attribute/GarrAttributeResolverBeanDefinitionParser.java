package it.garr.shibboleth.idp.attribute;

import javax.xml.namespace.QName;
import org.w3c.dom.Element;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.BaseDataConnectorBeanDefinitionParser;

public class GarrAttributeResolverBeanDefinitionParser extends BaseDataConnectorBeanDefinitionParser{
	
	Element pluginConfig = null;
	public static final QName TYPENAME = new QName(GarrAttributeResolverNamespaceHandler.NAMESPACE, "GarrConnector");

	@Override
	@SuppressWarnings("rawtypes")
	protected Class getBeanClass(Element element) {
        return GarrAttributeResolverFactoryBean.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        super.doParse(element, builder);
		//String lookupUrl = pluginConfig.getAttributeNS(null, "lookupUrl");
        //builder.addPropertyValue("lookupUrl", lookupUrl);
    }
    
}
