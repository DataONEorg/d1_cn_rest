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

package org.dataone.cn.rest.web.node.v2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.EqualPredicate;
import org.apache.log4j.Logger;
import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.hazelcast.ClientConfiguration;
import org.dataone.cn.hazelcast.HazelcastInstanceFactory;
import org.dataone.cn.indexer.SolrIndexService;
import org.dataone.cn.indexer.solrhttp.SolrElementAdd;
import org.dataone.cn.ldap.NodeAccess;
import org.dataone.cn.rest.web.AbstractWebController;
import org.dataone.cn.synchronization.types.SyncObject;
import org.dataone.configuration.Settings;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.portal.PortalCertificateManager;
import org.dataone.service.cn.impl.v2.NodeRegistryService;
import org.dataone.service.cn.v2.CNIdentity;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.util.NodelistUtil;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.ServiceMethodRestriction;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

/**
 *
 * This package will expose endpoints to handle manipulation of the Node Registry structure.
 * 
 * @author waltz
 * 
 */
@Controller("nodeControllerV2")
public class NodeController extends AbstractWebController implements ServletContextAware {

    Logger logger = Logger.getLogger(NodeController.class.getName());
    private static final String NODE_PATH_V2 = "/v2/" + Constants.RESOURCE_NODE + "/";
    private static final String NODELIST_PATH_V2 = "/v2/" + Constants.RESOURCE_NODE;
    private static final String RESOURCE_SYNCHRONIZE_V2 = "/v2/" + Constants.RESOURCE_SYNCHRONIZE;
    private static final String RESOURCE_DIAG_SYSMETA_V2 = "/v2/" + Constants.RESOURCE_DIAG_SYSMETA;
    private static final String RESOURCE_DIAG_INDEX_V2 = "/v2/" + Constants.RESOURCE_DIAG_OBJECT;

    private ServletContext servletContext;
    CertificateManager certificateManager = CertificateManager.getInstance();
    MultipartRequestResolver multipartRequestResolver = new MultipartRequestResolver("/tmp", 1000000000, 0);
    static final int SMALL_BUFF_SIZE = 25000;
    static final int MED_BUFF_SIZE = 50000;
    static final int LARGE_BUFF_SIZE = 100000;
    // need to exclude certain patterns from urlBase,
    // do not want an entry that makes the CN a sychronization target as an MN node
    // or rather we do not want an MN node to point to a CN end-point
    @Autowired
    @Qualifier("cnNodeRegistryV2")
    NodeRegistryService nodeRegistry;
    @Autowired
    @Qualifier("hzClientConfiguration")
    ClientConfiguration clientConfiguration;
    HazelcastInstance hzclient = null;
    @Autowired
    @Qualifier("cnIdentityV2")
    CNIdentity cnIdentity;

    @Value("${cn.nodeId}")
    String nodeIdentifier;
    NodeReference nodeReference;

    @Autowired
    SolrIndexService solrIndexService;

    List<String> nodeAdministrators = Settings.getConfiguration().getList("cn.administrators");
    List<Subject> nodeAdminSubjects = new ArrayList<Subject>();

    /*
     * initialize a couple class scope variables immediately after the controller has
     * been initialized by Spring
     * 
     * @author waltz
     * @returns void
     */
    @PostConstruct
    public void init() {
        nodeReference = new NodeReference();
        nodeReference.setValue(nodeIdentifier);
        
        // during intialization, construct the NodeAdminSubjects regardless
        // of whether nodeAdministrators have changed
        this.hasNodeAdministratorsChanged();
        this.constructNodeAdministrators();
        
    }
    /* 
     * Returns a list of nodes that have been registered with and approved by the DataONE infrastructure.
     * 
     * @author waltz
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @throws NotImplemented
     * @throws ServiceFailure
     * @return ModelAndView
     */
    @RequestMapping(value = {NODELIST_PATH_V2, NODE_PATH_V2}, method = RequestMethod.GET)
    public ModelAndView getNodeList(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented {

        //NodeList nodeList = nodeListRetrieval.retrieveNodeList(request, response, servletContext);
        NodeList nodeList;
        try {
            nodeList = nodeRegistry.listNodes();
        } catch (ServiceFailure ex) {
            ex.setDetail_code("4862");
            throw ex;
        }

        return new ModelAndView("xmlV2NodeListViewResolver", "org.dataone.service.types.v2.NodeList", nodeList);

    }

    /*
     * pass in a Node Identifier and receive back the node structure.
     * 
     * @author waltz
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @param String nodeId
     * @throws NotFound
     * @throws ServiceFailure
     * @return ModelAndView
     */
    @RequestMapping(value = NODE_PATH_V2 + "{nodeId}", method = RequestMethod.GET)
    public ModelAndView getNode(HttpServletRequest request, HttpServletResponse response, @PathVariable String nodeId) throws ServiceFailure, NotFound {
        NodeReference reference = new NodeReference();
        reference.setValue(nodeId);
        Node node = nodeRegistry.getNode(reference);

        return new ModelAndView("xmlV2NodeViewResolver", "org.dataone.service.types.v2.Node", node);

    }
    /*
     * For updating the capabilities of the specified node. Most information is replaced by information in the new node,
     * however, the node identifier, nodeType, ping, syncrhonization.lastHarvested, and
     * synchronization.lastCompleteHarvest are preserved from the existing entry. Services in the old record not
     * included in the new Node will be removed.
     *
     * Successful completion of this operation is indicated by a HTTP response status code of 200.
     *
     * Unsuccessful completion of this operation MUST be indicated by returning an appropriate exception.
     * 
     * @author waltz
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @param String nodeId
     * @throws InvalidToken 
     * @throws InvalidRequest
     * @throws IdentifierNotUnique
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws NotFound
     * @throws ServiceFailure
     * @return ModelAndView
     */
    @RequestMapping(value = NODE_PATH_V2 + "{nodeId}", method = RequestMethod.PUT)
    public void updateNodeCapabilities(MultipartHttpServletRequest fileRequest, HttpServletResponse response, @PathVariable String nodeId) throws InvalidToken, ServiceFailure, InvalidRequest, IdentifierNotUnique, NotAuthorized, NotImplemented, NotFound {
        Session session = PortalCertificateManager.getInstance().getSession(fileRequest);
        if (session == null) {
            throw new NotAuthorized("4821", "Need a valid certificate before request can be processed");
        }


        NodeReference updateNodeReference = new NodeReference();
        updateNodeReference.setValue(nodeId);
        // don't think lazy init will not work in this case since this is the controller for a servlet
        // so lazy init the client here. the hz cluster  should be up before
        // calls to update node capabilities is called (or write to LDAP will reflect the changes  )
        logger.info("group " + clientConfiguration.getGroup() + " pwd " + clientConfiguration.getPassword() + " addresses " + clientConfiguration.getLocalhost());
        if (hzclient == null) {
            // try to work around 
            try {
                // TODO: try to connect to each hzClusterMember (localhost being the first one) should a connection fail
                ClientConfig cc = new ClientConfig();
                cc.getGroupConfig().setName(clientConfiguration.getGroup());
                cc.getGroupConfig().setPassword(clientConfiguration.getPassword());
                cc.addAddress(clientConfiguration.getLocalhost());
                hzclient = HazelcastClient.newHazelcastClient(cc);
            } catch (Exception e) {
                logger.error("hzclient is not able to connect to cluster");
                hzclient = null;
            }
        }

        // retrieve the node structure being updated
        Node node = null;
        MultipartFile nodeDataMultipart = null;
        Set<String> keys = fileRequest.getFileMap().keySet();
        for (String key : keys) {
            logger.info("Found filepart " + key);
            if (key.equalsIgnoreCase("node")) {
                nodeDataMultipart = fileRequest.getFileMap().get(key);
            }
        }
        if (nodeDataMultipart != null) {
            try {
                node = TypeMarshaller.unmarshalTypeFromStream(Node.class, nodeDataMultipart.getInputStream());
            } catch (IOException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (InstantiationException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (IllegalAccessException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (JiBXException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            }

        } else {
            throw new InvalidRequest("4843", "Updated Node Xml not found in MultiPart request");
        }
        if (!updateNodeReference.equals(node.getIdentifier())) {
            throw new InvalidRequest("4843", "Updated Node Xml Node Reference " + updateNodeReference.getValue()
                    + " does not equal path node identifier " + node.getIdentifier().getValue());
        }
        if (this.hasNodeAdministratorsChanged()) {
             this.constructNodeAdministrators();
        }
        // decide if the subject requesting an update has permission to update
        Boolean approvedAdmin = false;
        Subject clientCertSubject = session.getSubject();
        logger.debug("Certificate has subject " + clientCertSubject.getValue());
        // is the subject equal to the value found in the subject list of the node?
        
        // the node subject must be retrieved from the current node information
        // the node subjects may be changed if the calling subject is an approved administrator
        Node currentNode = nodeRegistry.getNode(updateNodeReference);
        
        if ((currentNode.getSubjectList() != null) && !(currentNode.getSubjectList().isEmpty())) {
            for (Subject subject : currentNode.getSubjectList()) {
                if (subject.equals(clientCertSubject)) {
                    approvedAdmin = true;
                }
            }
        }
        
        // the subject may be a member of an approved administrators list maintained 
        // by the CN.  Currently, the list is in a node.properties file, but
        // it may reside elsewhere in the future
        
        if (!approvedAdmin && (nodeAdminSubjects != null)) {
            for (Subject subject : nodeAdminSubjects) {

                if (subject.equals(clientCertSubject)) {
                    approvedAdmin = true;
                    logger.debug("Approved Administrative subject is " + subject.getValue());
                }
            }
            if (!approvedAdmin) {
                logger.debug("Not approved yet");
                try {
                    SubjectInfo clientSubjectInfo = cnIdentity.getSubjectInfo(session,clientCertSubject);
                    // check to see if an equivalent identity of the administrative subject
                    if (clientSubjectInfo.getPersonList() != null) {
                        List<Person> clientPersonList = clientSubjectInfo.getPersonList();
                        logger.debug("how many persons on my list?" + clientPersonList.size());
                        for (Person person : clientPersonList) {
                            logger.debug("Person List :" + person.getSubject().getValue() + " for subject " + person.getSubject().getValue());
                            // check if the person listed is a part of the node administrators list
                            EqualPredicate equalPredicate = new EqualPredicate(person.getSubject());
                            if (CollectionUtils.exists(nodeAdminSubjects, equalPredicate)) {
                                 logger.debug("Admin Approved");
                                approvedAdmin = true;
                                break;
                            }
                            // check if any equivalent identities listed are a part of the node administrators list
                            for (Subject equivSubject : person.getEquivalentIdentityList()) {
                                logger.debug("Equiv Subject List :" + equivSubject.getValue());
                            }
                            if (CollectionUtils.containsAny(nodeAdminSubjects,person.getEquivalentIdentityList())) {
                                approvedAdmin = true;
                                logger.debug("Equiv Subject Approved");
                                break;
                            }
                            // check if any group memberships are a part of the node administrators list
                            if (CollectionUtils.containsAny(nodeAdminSubjects,person.getIsMemberOfList())) {
                                approvedAdmin = true;
                                logger.debug("Group Member Subject Approved");
                                break;
                            }
                        }
                    } else {
                         logger.debug("Person LIst is null");
                    }
                } catch (NotFound ex){
                    approvedAdmin = false;
                    logger.warn(clientCertSubject.getValue() + " in is not entered in LDAP");
                } catch (Exception ex) {
                    approvedAdmin = false;
                    logger.warn(ex.getMessage());
                }
            }
            
        }
        
        if (!approvedAdmin) {
           throw new NotAuthorized("4821", "Certificate should be an administrative subject before request can be processed");
        }
        // the contactSubject must be a registered and verified user or group
        List<Subject> contactSubjectList = node.getContactSubjectList();
        Boolean unVerifiedRegistration = false;
        StringBuilder errorMessage = new StringBuilder();
        if ( (contactSubjectList != null) && !(contactSubjectList.isEmpty())) {
            for (Subject contactSubject : contactSubjectList) {
                if (!(isVerifiedSubject(session, contactSubject))) {
                    errorMessage.append("Node Contact Subject: " + contactSubject.getValue() + " is not verified! \n");
                    unVerifiedRegistration = true;
                }
                // this is an even stricter check, if any one user in a group is unverified
                // then throw an exception
                SubjectInfo contactSubjectInfo;
                try {
                    contactSubjectInfo = cnIdentity.getSubjectInfo(session, contactSubject);

                    List<Group> contactGroupList = contactSubjectInfo.getGroupList();
                    for (Group contactGroup : contactGroupList) {
                        List<Subject> contactGroupSubjectList = contactGroup.getHasMemberList();
                        for (Subject groupSubject : contactGroupSubjectList) {
                            if (!(isVerifiedSubject(session, contactSubject))) {
                                errorMessage.append("Node Contact Subject: " + groupSubject.getValue() + " of Group: " + groupSubject.getValue() + " is not verified! \n");
                                unVerifiedRegistration = true;
                            }
                        }
                    }
                } catch (NotFound ex) {
                    throw new NotAuthorized("4821", "Node Contact Subject: " + contactSubject.getValue() + " is not a Registered Subject, and cannot be found");
                }
            }
        }
        if (unVerifiedRegistration) {
            throw new NotAuthorized("4821", errorMessage.toString());
        }
        if (hzclient == null) {
            nodeRegistry.updateNodeCapabilities(updateNodeReference, node);
        } else {
            IMap<NodeReference, Node> hzNodes = hzclient.getMap("hzNodes");

            NodeReference nodeReference = node.getIdentifier();

            hzNodes.put(updateNodeReference, node);
        }
        return;

    }
    
    /*
     * Register a new node in the system. If the node already exists, then a IdentifierNotUnique exception MUST be returned.
     * 
     * @author waltz
     * @param MultipartHttpServletRequest request
     * @param HttpServletResponse response
     * @throws InvalidToken 
     * @throws InvalidRequest
     * @throws IdentifierNotUnique
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws ServiceFailure
     * @return ModelAndView
     * 
     */
    @RequestMapping(value = {NODELIST_PATH_V2, NODE_PATH_V2}, method = RequestMethod.POST)
    public ModelAndView register(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, NotImplemented, InvalidRequest, NotAuthorized, IdentifierNotUnique, InvalidToken {
        Session session = PortalCertificateManager.getInstance().getSession(fileRequest);
        if (session == null) {
            throw new NotAuthorized("4841", "Need a valid certificate before request can be processed");
        }

        Node node = null;
        MultipartFile nodeDataMultipart = null;
        Set<String> keys = fileRequest.getFileMap().keySet();
        for (String key : keys) {
            logger.info("Found filepart " + key);
            if (key.equalsIgnoreCase("node")) {
                nodeDataMultipart = fileRequest.getFileMap().get(key);
            }
        }
        if (nodeDataMultipart != null) {
            try {
                node = TypeMarshaller.unmarshalTypeFromStream(Node.class, nodeDataMultipart.getInputStream());
            } catch (IOException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (InstantiationException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (IllegalAccessException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            } catch (JiBXException ex) {
                throw new ServiceFailure("4842", ex.getMessage());
            }

        } else {
            throw new InvalidRequest("4843", "New Node Xml not found in MultiPart request");
        }


        // Contact Subject must be registered and verified users
        List<Subject> contactSubjectList = node.getContactSubjectList();
        Boolean unVerifiedRegistration = false;
        StringBuilder errorMessage = new StringBuilder();
        for (Subject contactSubject : contactSubjectList) {
            if (!(isVerifiedSubject(session, contactSubject))) {
                errorMessage.append("ContactSubject: " + contactSubject.getValue() + " is not verified! \n");
                unVerifiedRegistration = true;
            }
            // this is an even stricter check, if any one user in a group is unverified
            // then throw an exception
            SubjectInfo contactSubjectInfo;
            try {
                contactSubjectInfo = cnIdentity.getSubjectInfo(session, contactSubject);

                List<Group> contactGroupList = contactSubjectInfo.getGroupList();
                for (Group contactGroup : contactGroupList) {
                    List<Subject> contactGroupSubjectList = contactGroup.getHasMemberList();
                    for (Subject groupSubject : contactGroupSubjectList) {
                        if (!(isVerifiedSubject(session, contactSubject))) {
                            errorMessage.append("ContactSubject: " + groupSubject.getValue() + " of Group: " + groupSubject.getValue() + " is not verified! \n");
                            unVerifiedRegistration = true;
                        }
                    }
                }
            } catch (NotFound ex) {
                throw new NotAuthorized("4841", "ContactSubject:" + contactSubject.getValue() + " is not a Registered Subject");
            }
        }
        if (unVerifiedRegistration) {
            throw new NotAuthorized("4841", errorMessage.toString());
        }

        //IMap<NodeReference, Node> hzNodes = hzclient.getMap("hzNodes");
        //for (NodeReference noderef : hzNodes.keySet()) {
        //    logger.info(noderef.getValue());
        //}
        //NodeReference nodeReference = node.getIdentifier();
        //if (hzNodes.containsKey(nodeReference)) {
        //    throw new IdentifierNotUnique("4844", "Sorry! Node Identifier " + nodeReference.getValue() + " already exists ");
        //}
        // XXX need to generate new Node Reference before putting it in the map
        //       NodeReference nodeReference = nodeRegistry.generateNodeIdentifier();
        //       node.setIdentifier(nodeReference);

        NodeReference nodeReference = nodeRegistry.register(node);
        return new ModelAndView("xmlNodeReferenceViewResolver", "org.dataone.service.types.v1.NodeReference", nodeReference);
    }
    
    
    
    
    @RequestMapping(value = {RESOURCE_SYNCHRONIZE_V2, RESOURCE_SYNCHRONIZE_V2 + "/" }, method = RequestMethod.POST)
    public void synchronize(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotAuthorized { // throws ServiceFailure, NotImplemented, InvalidToken, NotAuthorized {

        try {
            // get the NodeID from the subject via xref to the nodeList
            Session session = PortalCertificateManager.getInstance().getSession(request);
            final Subject clientSubject = session.getSubject();
            NodeAccess na = nodeRegistry.getNodeAccess();
            List<Node> nodes = na.getApprovedNodeList();
            Node firstFoundNode = (Node) CollectionUtils.find(nodes,
                    new Predicate() {
                        public boolean evaluate(Object o) {
                            Node node = (Node) o;
                            return node.getSubjectList().contains(clientSubject);
                        }
                    }
                    );
            if (firstFoundNode == null) {
                throw new NotAuthorized("4962","The client certificate does not find an approved Member Node");
            }
            NodeReference nodeId = firstFoundNode.getIdentifier();
            
            // get the pid parameter from the request object directly
            String pidString = request.getParameter("pid");
            Identifier pid = new Identifier();
            pid.setValue(pidString);
            
            // check locally to see if the calling node is the authoritative one.
            HazelcastInstance hazelcast = HazelcastInstanceFactory.getProcessingInstance();
            
            String hzSystemMetaMapName = 
                    Settings.getConfiguration().getString("dataone.hazelcast.systemMetadata");

            
            // if this is a synchronized object and the recorded authoritativeMN conflicts, 
            // we need to throw a NotAuthorized
            SystemMetadata sysmeta = (SystemMetadata)hazelcast.getMap(hzSystemMetaMapName).get(pid);
            if (sysmeta != null && !sysmeta.getAuthoritativeMemberNode().equals(nodeId)) {
                String message = String.format(
                        "The requesting MemberNode (%s) is not the Authoritative MN for this object (%s).", 
                        nodeId.getValue(), pid.getValue());
                logger.info(message);
                throw new NotAuthorized("4692", message);
            }

            // process the request and add to the queue
            String synchronizationObjectQueue = 
                    Settings.getConfiguration().getString("dataone.hazelcast.synchronizationObjectQueue");
            BlockingQueue<SyncObject> hzSyncObjectQueue = hazelcast.getQueue(synchronizationObjectQueue);
            

            // check that the item isn't already in the queue
            // TODO: comparator for SyncObjects
            SyncObject so = new SyncObject(nodeId.getValue(), pid.getValue());
            if (!hzSyncObjectQueue.contains(so)) {
                hzSyncObjectQueue.add(so);
            }
        } catch (ServiceFailure e) {
            e.setDetail_code("4961");
            throw e;
        } catch (Exception e) {
            throw new ServiceFailure("4961","Unexpected Exception:: " + e.toString());
        }
    }
    
    
    @RequestMapping(value = {RESOURCE_DIAG_SYSMETA_V2, RESOURCE_DIAG_SYSMETA_V2 + "/" }, method = RequestMethod.POST)
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
            } catch (JiBXException ex) {
                throw new InvalidSystemMetadata("4976", ex.getMessage());
            } catch (Exception e) {
                throw new ServiceFailure("4971", e.getMessage());
            }
            
            // now we have systemMetadata, what do we check?
           	return new ModelAndView("xmlV2MetaViewResolver", SystemMetadata.class.getName(), sysMeta);


        } else {
            throw new InvalidRequest("4974", "SystemMetadata not found in MultiPart request");
        }
    }
    
    @RequestMapping(value = {RESOURCE_DIAG_INDEX_V2, RESOURCE_DIAG_INDEX_V2 + "/" }, method = RequestMethod.POST)
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
                } catch (JiBXException ex) {
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
			SolrElementAdd addCommand = solrIndexService.processObject(id, sysMetaMultipart.getInputStream(), objectFile.getAbsolutePath());
			
			// remove temp file
			objectFile.delete();
			
			// send result to response output stream
			addCommand.serialize(response.getOutputStream(), "UTF-8");
			
			
        } catch (Exception e) {
        	e.printStackTrace();
            throw new ServiceFailure("4981", e.getMessage());
        }
        
    }
    
    /*
     * determine if the passed in subject has been verified in the Identity service
     * 
     * @author waltz
     * @param Session session
     * @param Subject subject
     * @throws ServiceFailure
     * @throws InvalidRequest
     * @throws NotAuthorized
     * @throws NotImplemented
     * @throws InvalidToken
     * @return Boolean
     */
    private Boolean isVerifiedSubject(Session session, Subject subject) throws ServiceFailure, InvalidRequest, NotAuthorized, NotImplemented, InvalidToken {
        Boolean verifiedRegistration = false;
        SubjectInfo contactSubjectInfo;
        try {
            contactSubjectInfo = cnIdentity.getSubjectInfo(session, subject);
        } catch (NotFound ex) {
            throw new NotAuthorized("4841", subject.getValue() + " is not a Registered Subject");
        }
        if (contactSubjectInfo == null) {
            return verifiedRegistration;
        }
        if (contactSubjectInfo.getPersonList() == null || contactSubjectInfo.getPersonList().isEmpty()) {
            return verifiedRegistration;
        }
        List<Person> contactPersonList = contactSubjectInfo.getPersonList();
        for (Person contactPerson : contactPersonList) {
            if (contactPerson.getVerified()) {
                verifiedRegistration = true;
                break;
            }
        }
        return verifiedRegistration;
    }
    
    /*
     * determine if any changes have occurred either on the NodeList or 
     * in the properties file to CN administrative subjects
     * 
     * @author waltz
     * @return boolean
     */
    private boolean hasNodeAdministratorsChanged()  {
        try {
        List<String> tmpNodeAdministrators = buildUpdateNodeCapabilitiesAdministrativeList();
        
        if (!CollectionUtils.isEqualCollection(nodeAdministrators, tmpNodeAdministrators)) {
            nodeAdministrators =  tmpNodeAdministrators;
            return true;
        }
        } catch (NotImplemented ex) {
            ex.printStackTrace();
        } catch (ServiceFailure ex) {
            ex.printStackTrace();
        }
        return false;
    }
    
    /*
     * from nodeAdministrators list, build a list of Administrator subjects
     * 
     * @author waltz
     * @return void
     */
    private void constructNodeAdministrators() {
        nodeAdminSubjects  = new ArrayList<Subject>();
        for (String administrator : nodeAdministrators) {
            logger.info("AdminList entry " + administrator);
            Subject adminSubject = new Subject();
            adminSubject.setValue(administrator);
            nodeAdminSubjects.add(adminSubject);
        }
    }
    
    /*
     * refreshes an array of subjects listed as CN's in the nodelist or in a properties file.
     * If the ServiceMethodRestriction list of updateNodeCapabilities is set, 
     * then those subjects act as administrators as well.
     * 
     * @author waltz
     * @param HttpServletRequest request
     * @param HttpServletResponse response
     * @param String acceptType
     * @throws NotImplemented
     * @throws ServiceFailure
     * 
     * @return List<String>
     */
    private List<String> buildUpdateNodeCapabilitiesAdministrativeList() throws NotImplemented, ServiceFailure {
        List<String> administrators = new ArrayList<String>();

        List<String> administratorsProperties = Settings.getConfiguration().getList("cn.administrators");
        if (administrators != null) {
            for (String administrator : administratorsProperties) {
                logger.debug("AdminList entry " + administrator);
                administrators.add(administrator);
            }
        }

        List<Node> nodeList = nodeRegistry.listNodes().getNodeList();
        for (Node node : nodeList) {
            if (node.getType().equals(NodeType.CN) && node.getState().equals(NodeState.UP)) {
                for (Subject adminstrativeSubject : node.getSubjectList()) {
                     administrators.add(adminstrativeSubject.getValue());
                }
                List<Service> cnServices = node.getServices().getServiceList();
                for (Service service : cnServices) {
                    if (service.getName().equalsIgnoreCase("CNRegister")) {
                        if ((service.getRestrictionList() != null)
                                && !service.getRestrictionList().isEmpty()) {
                            List<ServiceMethodRestriction> serviceMethodRestrictionList = service
                                    .getRestrictionList();
                            for (ServiceMethodRestriction serviceMethodRestriction : serviceMethodRestrictionList) {
                                if (serviceMethodRestriction.getMethodName().equalsIgnoreCase(
                                        "updateNodeCapabilities")) {
                                    if (serviceMethodRestriction.getSubjectList() != null) {
                                           for (Subject administrator : serviceMethodRestriction.getSubjectList()) {
                                                logger.debug("updateNodeCapabilities ServiceMethodRestriction entry " + administrator);
                                                administrators.add(administrator.getValue());
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return administrators;
    }
    @Override
    public void setServletContext(ServletContext sc) {
        this.servletContext = sc;
    }

    public HazelcastInstance getHzclient() {
        return hzclient;
    }

    public void setHzclient(HazelcastInstance hzclient) {
        this.hzclient = hzclient;
    }
}
