package it.garr.shibboleth.idp.attribute;

import it.garr.shibboleth.idp.S3AccessorMethods;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.BaseDataConnector;

public class GarrAttributeResolverConnector extends BaseDataConnector {

	private final Logger log = LoggerFactory.getLogger(GarrAttributeResolverConnector.class);
	
	/** {@inheritDoc} */
	@SuppressWarnings("rawtypes")
	public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)  throws AttributeResolutionException {
		Map<String, BaseAttribute> result = new HashMap<String, BaseAttribute>();
		String username = resolutionContext.getAttributeRequestContext().getPrincipalName();
		String entityId = resolutionContext.getAttributeRequestContext().getLocalEntityId();
		
		BaseAttribute<String> amazonS3AccessKey = new BasicAttribute<String>(); 
		((BasicAttribute) amazonS3AccessKey).setId("amazonS3AccessKey");
		try {
			amazonS3AccessKey.getValues().add(S3AccessorMethods.getStoredAccessKey(entityId, username));
		}
		catch (UnsupportedEncodingException e) {
			log.error("Error while encoding 'amazonS3AccessKey' attribute.", e);
			throw new AttributeResolutionException(e.getMessage());
		}
		result.put(amazonS3AccessKey.getId(), amazonS3AccessKey);
		
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public void validate() throws AttributeResolutionException {
		// Method to validate that the attribute resolver is working properly.
		// Should try to perform all initializations and in case of error throw an exception.
	}
}
