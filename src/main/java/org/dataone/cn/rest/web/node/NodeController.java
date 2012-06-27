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

package org.dataone.cn.rest.web.node;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.IMap;

import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.dataone.client.auth.CertificateManager;
import org.dataone.cn.hazelcast.ClientConfiguration;
import org.dataone.mimemultipart.MultipartRequestResolver;
import org.dataone.service.cn.impl.v1.NodeRegistryService;
import org.dataone.service.cn.v1.CNIdentity;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.util.Constants;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
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
import javax.annotation.PostConstruct;
import org.dataone.cn.rest.web.AbstractWebController;
import org.dataone.configuration.Settings;

/**
 * Returns a list of nodes that have been registered with the DataONE infrastructure.
 * This list is also referred to as the registry.
 *
 * This package will also edit and add to the registry
 * @author waltz
 */
@Controller("nodeController")
public class NodeController extends AbstractWebController implements ServletContextAware {

    Logger logger = Logger.getLogger(NodeController.class.getName());
    private static final String NODE_PATH_V1 = "/v1/" + Constants.RESOURCE_NODE + "/";
    private static final String NODELIST_PATH_V1 = "/v1/" + Constants.RESOURCE_NODE;
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
    @Qualifier("cnNodeRegistry")
    NodeRegistryService nodeRegistry;
    @Autowired
    @Qualifier("hzClientConfiguration")
    ClientConfiguration clientConfiguration;
    HazelcastInstance hzclient = null;
    @Autowired
    @Qualifier("cnIdentity")
    CNIdentity cnIdentity;

    @Value("${cn.nodeId}")
    String nodeIdentifier;
    NodeReference nodeReference;

    @Value("${cn.administrators}")
    String nodeAdministrators;
    List<Subject> nodeAdminSubjects = new ArrayList<Subject>();

    @PostConstruct
    public void init() {
        nodeReference = new NodeReference();
        nodeReference.setValue(nodeIdentifier);
        this.constructNodeAdministrators();

    }
    @RequestMapping(value = {NODELIST_PATH_V1, NODE_PATH_V1}, method = RequestMethod.GET)
    public ModelAndView getNodeList(HttpServletRequest request, HttpServletResponse response) throws ServiceFailure, NotImplemented {

        //NodeList nodeList = nodeListRetrieval.retrieveNodeList(request, response, servletContext);
        NodeList nodeList;

        nodeList = nodeRegistry.listNodes();

        return new ModelAndView("xmlNodeListViewResolver", "org.dataone.service.types.v1.NodeList", nodeList);

    }

    @RequestMapping(value = NODE_PATH_V1 + "{nodeId}", method = RequestMethod.GET)
    public ModelAndView getNode(HttpServletRequest request, HttpServletResponse response, @PathVariable String nodeId) throws Exception {
        NodeReference reference = new NodeReference();
        reference.setValue(nodeId);
        Node node = nodeRegistry.getNode(reference);

        return new ModelAndView("xmlNodeViewResolver", "org.dataone.service.types.v1.Node", node);

    }

    @RequestMapping(value = NODE_PATH_V1 + "{nodeId}", method = RequestMethod.PUT)
    public void updateNodeCapabilities(MultipartHttpServletRequest fileRequest, HttpServletResponse response, @PathVariable String nodeId) throws InvalidToken, ServiceFailure, InvalidRequest, IdentifierNotUnique, NotAuthorized, NotImplemented, NotFound {
        Session session = CertificateManager.getInstance().getSession(fileRequest);
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
            hzclient = HazelcastClient.newHazelcastClient(clientConfiguration.getGroup(), clientConfiguration.getPassword(),
                    clientConfiguration.getLocalhost());
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
        // XXX this would be easy to manipuate, in order to subvert a node
        if ((node.getSubjectList() != null) && !(node.getSubjectList().isEmpty())) {
            for (Subject subject : node.getSubjectList()) {
                if (subject.equals(clientCertSubject)) {
                    approvedAdmin = true;
                }
            }
        }
        if (!approvedAdmin && (nodeAdminSubjects != null)) {
            for (Subject subject : nodeAdminSubjects) {
                logger.debug("Administrative subject is " + subject.getValue());
                if (subject.equals(clientCertSubject)) {
                    approvedAdmin = true;
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

    @RequestMapping(value = {NODELIST_PATH_V1, NODE_PATH_V1}, method = RequestMethod.POST)
    public ModelAndView register(MultipartHttpServletRequest fileRequest, HttpServletResponse response) throws ServiceFailure, NotImplemented, InvalidRequest, NotAuthorized, IdentifierNotUnique, InvalidToken {
        Session session = CertificateManager.getInstance().getSession(fileRequest);
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

    private boolean hasNodeAdministratorsChanged() {
        logger.info(Settings.getConfiguration().getString("cn.administrators"));
        if (!nodeAdministrators.equalsIgnoreCase( Settings.getConfiguration().getString("cn.administrators"))) {
            nodeAdministrators =  Settings.getConfiguration().getString("cn.administrators");
            logger.info(nodeAdministrators);
            nodeAdminSubjects  = new ArrayList<Subject>();
            return true;
        } else {
            return false;
        }
    }
    private void constructNodeAdministrators() {

        String[] administrators =nodeAdministrators.split(";");
        for (int i = 0; i < administrators.length; i++) {
            logger.info(administrators[i]);
            Subject adminSubject = new Subject();
            adminSubject.setValue(administrators[i]);
            nodeAdminSubjects.add(adminSubject);
        }
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
