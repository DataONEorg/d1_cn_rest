/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dataone.cn.web;
import org.jibx.runtime.IUnmarshallingContext;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.BindingDirectory;
import org.dataone.service.util.TypeMarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dataone.service.types.v1.SystemMetadata;
import org.jibx.runtime.JiBXException;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author waltz
 */
public class MarshallerTestCase {
        @Test
    public void deserializeSystemMetadata()  {
        try {
                    IBindingFactory bfact = BindingDirectory.getFactory(SystemMetadata.class);
        IUnmarshallingContext uctx = bfact.createUnmarshallingContext();

            InputStream is = this.getClass().getResourceAsStream("/org/dataone/cn/resources/samples/v1/systemMetadataSample1.xml");
//            SystemMetadata domainObject = (SystemMetadata) uctx.unmarshalDocument(is, null);
            SystemMetadata systemMetadata = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, is);
        } catch (InstantiationException ex) {
            ex.printStackTrace();
            fail("Test misconfiguration" +  ex);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            fail("Test misconfiguration" +  ex);
        } catch (JiBXException ex) {
            ex.printStackTrace();
            fail("Test misconfiguration" +  ex);
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("Test misconfiguration" +  ex);
        }


    }

}
