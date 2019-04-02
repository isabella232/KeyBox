package com.looker.azure.util;

import com.keybox.common.util.AppConfig;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSAnyImpl;
import org.springframework.security.saml.SAMLCredential;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is inspired from:
 * http://stackoverflow.com/a/33678256
 */
public class SAMLUtil {

    public final static String EMAIL_ATTRIBUTE_NAME = AppConfig.getProperty("azureEmailName");
    public final static String FIRSTNAME_ATTRIBUTE_NAME = AppConfig.getProperty("azureFirstName");
    public final static String LASTNAME_ATTRIBUTE_NAME = AppConfig.getProperty("azureLastName");
    public final static String DEPARTMENT_ATTRIBUTE_NAME = AppConfig.getProperty("azureDepartment");
    public final static String GROUPS_ATTRIBUTE_NAME = AppConfig.getProperty("azureGroups");

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
        throw new IllegalArgumentException("attribute (" + attributeName + ") not found");
    }

    public static ArrayList<String> getGroupIds(SAMLCredential credential)
    {
        ArrayList<String> retList = new ArrayList<>();
        for (Attribute attribute : credential.getAttributes())
        {
            if (GROUPS_ATTRIBUTE_NAME.equals(attribute.getName()))
            {
                List<XMLObject> attributeValues = attribute.getAttributeValues();
                for (XMLObject xmlObject : attributeValues)
                {
                    String value = getAttributeValue(xmlObject);
                    if (!value.isEmpty())
                    {
                        retList.add(value);
                    }
                }
            }
        }
        return retList;
    }

    public static String departmentFromGroups(List<String> groupIds)
    {
        // get each of the items
        //  they should be in the format "key1:val1;key2:val2"
        String[] keyValuePairs = AppConfig.getProperty("lookerGroups").split(";");
        // initialize the hashmap
        HashMap<String, String> idsToGroups = new HashMap<>();

        // loop over each key value to split apart
        for (String pair : keyValuePairs)
        {
            String[] entry = pair.split(":");
            idsToGroups.put(entry[0].trim(), entry[1].trim());
        }

        // loop over the group ids from the saml assertion
        for (String groupId: groupIds)
        {
            if (idsToGroups.containsKey(groupId))
            {
                //short circuit on the first group we find
                return idsToGroups.get(groupId);
            }
        }

        // no department mapping found; no department returned
        return null;
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
