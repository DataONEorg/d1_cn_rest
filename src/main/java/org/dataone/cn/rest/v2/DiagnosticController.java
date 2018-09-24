/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */
package org.dataone.cn.rest.v2;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dataone.cn.indexer.SolrIndexService;
import org.dataone.cn.indexer.solrhttp.SolrDoc;
import org.dataone.cn.rest.AbstractServiceController;
import org.dataone.exceptions.MarshallingException;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 * Implements the rest endpoint methods from the Diagnostic API
 * 
 * 
 */
@Controller("diagnosticControllerV2")
public class DiagnosticController extends AbstractServiceController implements ServletContextAware {
    
    Logger logger = Logger.getLogger(DiagnosticController.class);
    private ServletContext servletContext;
    
    private static final String RESOURCE_DIAG_SYSMETA_V2 = "/v2/" + Constants.RESOURCE_DIAG_SYSMETA;
    private static final String RESOURCE_DIAG_INDEX_V2 = "/v2/" + Constants.RESOURCE_DIAG_OBJECT;
    private static final String CREDENTIALS_PATH_V2 = "/v2/" + Constants.RESOURCE_DIAG_SUBJECT;
    @Autowired
    @Qualifier("solrIndexService")
    SolrIndexService solrIndexService;

    /**
     * Parse and echo the provided system metadata
     * 
     * On successful parsing, a copy of the system metadata is returned, 
     * otherwise an exception is returned indicating an error condition.
     * 
     * @param fileRequest
     * @param response
     * @return
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws IdentifierNotUnique 
     */
    @RequestMapping(value = {RESOURCE_DIAG_SYSMETA_V2, RESOURCE_DIAG_SYSMETA_V2 + "/"}, method = RequestMethod.POST)
    public ModelAndView echoSystemMetadata(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws
            ServiceFailure, NotImplemented, InvalidToken, NotAuthorized, InvalidRequest, InvalidSystemMetadata, IdentifierNotUnique {

        SystemMetadata sysMeta = null;
        MultipartFile sysMetaMultipart = null;
        Set<String> keys = fileRequest.getFileMap().keySet();
        for (String key : keys) {
            logger.info("Found filepart " + key);
            if (key.equalsIgnoreCase("sysMeta")) {
                sysMetaMultipart = fileRequest.getFileMap().get(key);
            }
        }
        if (sysMetaMultipart != null) {
            try {
                sysMeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysMetaMultipart.getInputStream());
            } catch (MarshallingException ex) {
                throw new InvalidSystemMetadata("4976", ex.getMessage());
            } catch (Exception e) {
                throw new ServiceFailure("4971", e.getMessage());
            }

            // now we have systemMetadata, what do we check?
            return new ModelAndView("xmlMetaViewResolverV2", SystemMetadata.class.getName(), sysMeta);

        } else {
            throw new InvalidRequest("4974", "SystemMetadata not found in MultiPart request");
        }
    }
  /**
    *
    * Echo the credentials used to make the call. This method can be used to verify the
    * client certificate is valid and contains the expected information.
    *
    * GET /diag/subject
    *
    * @author leinfelder
    * 
    * @param request
    * @param response
    * @return 
    * @throws org.dataone.service.exceptions.ServiceFailure 
    * @throws org.dataone.service.exceptions.InvalidToken 
    * @throws org.dataone.service.exceptions.NotImplemented 
    *
    */
   @RequestMapping(value = {CREDENTIALS_PATH_V2, CREDENTIALS_PATH_V2 + "/"}, method = RequestMethod.GET)
   public ModelAndView echoCredentials(HttpServletRequest request, HttpServletResponse response) 
           throws ServiceFailure, InvalidToken, NotImplemented {

       try {
           // get the Session object from the request
           Session session = PortalCertificateManager.getInstance().getSession(request);
           if (session == null) {
               //throw new InvalidToken("4967", "No credentials were received in the request. (Session was null)");
               throw new InvalidToken("4967", "The supplied authentication token (Session) could not be verified as being valid.");
           }

           // serialize it back
           SubjectInfo subjectInfo = session.getSubjectInfo();

           return new ModelAndView("xmlSubjectInfoViewResolverV1", "org.dataone.service.types.v1.SubjectInfo", subjectInfo);
       } catch (InvalidToken e) {
           throw e;
       }
       catch (Exception e) {
           ServiceFailure sf =  new ServiceFailure("4966", 
                   "Unexpected exception while processing the request:: " + e.toString());
           sf.initCause(e);
           throw sf;
       }
   }

    /**
     * Parse and echo the provided science metadata or resource map document. 
     * The response is governed by the type of object provided in the request, and on success 
     * is one or more documents that are the result of parsing for indexing.
     * 
     * Since DataONE supports multiple types of query engine, the query engine to be used for 
     * parsing is specified in the request.
     * 
     * The servce may terminate the POST operation if the size of the object is beyond a reasonable size.
     * 
     * @param fileRequest
     * @param response
     * @throws ServiceFailure
     * @throws NotImplemented
     * @throws InvalidToken
     * @throws NotAuthorized
     * @throws InvalidRequest
     * @throws InvalidSystemMetadata
     * @throws IdentifierNotUnique 
     */
    @RequestMapping(value = {RESOURCE_DIAG_INDEX_V2, RESOURCE_DIAG_INDEX_V2 + "/"}, method = RequestMethod.POST)
    public void echoIndexedObject(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws
            ServiceFailure, NotImplemented, InvalidToken, NotAuthorized, InvalidRequest, InvalidSystemMetadata, IdentifierNotUnique {

        String id = null;
        SystemMetadata sysMeta = null;
        String queryEngine = null;
        MultipartFile object = null;
        MultipartFile sysMetaMultipart = null;
        Set<String> keys = fileRequest.getFileMap().keySet();
        for (String key : keys) {
            logger.info("Found filepart " + key);
            if (key.equalsIgnoreCase("sysMeta")) {
                sysMetaMultipart = fileRequest.getFileMap().get(key);
                try {
                    sysMeta = TypeMarshaller.unmarshalTypeFromStream(SystemMetadata.class, sysMetaMultipart.getInputStream());
                    id = sysMeta.getIdentifier().getValue();
                } catch (MarshallingException ex) {
                    throw new InvalidSystemMetadata("4985", ex.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new ServiceFailure("4981", e.getMessage());
                }
            }
            if (key.equalsIgnoreCase("object")) {
                object = fileRequest.getFileMap().get(key);
            }

        }

        if (object == null) {
            throw new InvalidRequest("4984", "Object not found in MultiPart request");
        }
        try {

            // save the object locally for index processor
            File objectFile = File.createTempFile("dia_object", ".tmp");
            object.transferTo(objectFile);

            // process the object
            Map<String,SolrDoc> solrDocs = solrIndexService.parseTaskObject(id, sysMetaMultipart.getInputStream(), objectFile.getAbsolutePath());

            // remove temp file
            objectFile.delete();

            // send result to response output stream
            for (SolrDoc sd : solrDocs.values()) {
                sd.serialize(response.getOutputStream(), "UTF-8");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceFailure("4981", e.getMessage());
        }

    }
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }
}
