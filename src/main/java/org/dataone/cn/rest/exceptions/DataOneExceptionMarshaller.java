/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dataone.cn.rest.exceptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.dataone.service.exceptions.AuthenticationTimeout;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidCredentials;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.exceptions.UnsupportedMetadataType;
import org.dataone.service.exceptions.UnsupportedType;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.XmlMappingException;

/**
 *
 * @author waltz
 */
public class DataOneExceptionMarshaller implements Marshaller {

    static final private List<String> validClasses= new ArrayList<String>();;

    static {

        validClasses.add(AuthenticationTimeout.class.getName());
        validClasses.add(BaseException.class.getName());
        validClasses.add(IdentifierNotUnique.class.getName());
        validClasses.add(InsufficientResources.class.getName());
        validClasses.add(InvalidCredentials.class.getName());
        validClasses.add(InvalidRequest.class.getName());
        validClasses.add(InvalidSystemMetadata.class.getName());
        validClasses.add(InvalidToken.class.getName());
        validClasses.add(NotAuthorized.class.getName());
        validClasses.add(NotFound.class.getName());
        validClasses.add(NotImplemented.class.getName());
        validClasses.add(ServiceFailure.class.getName());
        validClasses.add(SynchronizationFailed.class.getName());
        validClasses.add(UnsupportedMetadataType.class.getName());
        validClasses.add(UnsupportedType.class.getName());

    }
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();

    @Override
    public boolean supports(Class<?> type) {
        String className = type.getName();
        for (String validClass : validClasses) {
            if (className.equals(validClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void marshal(Object o, Result result) throws IOException, XmlMappingException {
        BaseException baseException = (BaseException) o;

        ByteArrayInputStream is = new ByteArrayInputStream(baseException.serialize(BaseException.FMT_XML).getBytes());

        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new StreamSource(is), result);
        } catch (TransformerException ex) {
            throw new MarshallingFailureException("DataOneExceptionMarshaller: " + ex.getMessage());
        }
    }

}
