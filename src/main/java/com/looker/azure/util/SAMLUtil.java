package com.looker.azure.util;

import com.keybox.common.util.AppConfig;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSAnyImpl;
import org.springframework.security.saml.SAMLCredential;

import java.util.List;

/**
 * This class is inspired from:
 * http://stackoverflow.com/a/33678256
 */
public class SAMLUtil {

    public final static String EMAIL_ATTRIBUTE_NAME = AppConfig.getProperty("azureEmailName");
    public final static String FIRSTNAME_ATTRIBUTE_NAME = AppConfig.getProperty("azureFirstName");
    public final static String LASTNAME_ATTRIBUTE_NAME = AppConfig.getProperty("azureLastName");

    public static String getAttribute(SAMLCredential credential, String attributeName)
    {
        for (Attribute attribute : credential.getAttributes())
        {
            if (attributeName.equals(attribute.getName()))
            {
                List<XMLObject> attributeValues = attribute.getAttributeValues();
                if (!attributeValues.isEmpty())
                {
                    return getAttributeValue(attributeValues.get(0));
                }
            }
        }
        throw new IllegalArgumentException("no email attribute found");
    }

    private static String getAttributeValue(XMLObject attributeValue)
    {
        return attributeValue == null ?
                null :
                attributeValue instanceof XSString ?
                        getStringAttributeValue((XSString) attributeValue) :
                        attributeValue instanceof XSAnyImpl ?
                                getAnyAttributeValue((XSAnyImpl) attributeValue) :
                                attributeValue.toString();
    }

    private static String getStringAttributeValue(XSString attributeValue)
    {
        return attributeValue.getValue();
    }

    private static String getAnyAttributeValue(XSAnyImpl attributeValue)
    {
        return attributeValue.getTextContent();
    }
}
