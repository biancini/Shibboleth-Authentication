package it.garr.shibboleth.idp.attribute;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.BaseDataConnectorFactoryBean;

public class GarrAttributeResolverFactoryBean extends BaseDataConnectorFactoryBean{

	@Override
    @SuppressWarnings("rawtypes")
	public Class getObjectType() {
        return GarrAttributeResolverConnector.class;
    }

    @Override
    protected Object createInstance() throws Exception {
    	GarrAttributeResolverConnector connector = new GarrAttributeResolverConnector();
        populateDataConnector(connector);
        return connector;
    }

}
