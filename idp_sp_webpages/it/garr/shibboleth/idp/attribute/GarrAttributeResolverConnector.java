package it.garr.shibboleth.idp.attribute;

import java.util.HashMap;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.BaseDataConnector;

public class GarrAttributeResolverConnector extends BaseDataConnector {

	/** {@inheritDoc} */
	@SuppressWarnings("rawtypes")
	public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)  throws AttributeResolutionException {
		Map<String, BaseAttribute> result = new HashMap<String, BaseAttribute>();
		String username = resolutionContext.getAttributeRequestContext().getPrincipalName();
		String entityId = resolutionContext.getAttributeRequestContext().getLocalEntityId();
		
		BaseAttribute<String> amazonS3AccessKey = new BasicAttribute<String>(); 
		((BasicAttribute) amazonS3AccessKey).setId("amazonS3AccessKey");
		amazonS3AccessKey.getValues().add(getAmazonS3AccessKey(entityId, username));
		result.put(amazonS3AccessKey.getId(), amazonS3AccessKey);
		
		return result;
	}
	
	private String getAmazonS3AccessKey(String entityId, String username) {
		// TODO: retrieve IdP entityId in some way
		// TODO: string to be encoded base64
		return entityId + "!" + username;
	}

	/** {@inheritDoc} */
	@Override
	public void validate() throws AttributeResolutionException {
		// Method to validate that the attribute resolver is working properly.
		// Should try to perform all initializations and in case of error throw an exception.
	}
}
