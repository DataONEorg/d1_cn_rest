/**
 * This work was created by participants in the DataONE project, and is jointly copyrighted by participating
 * institutions in DataONE. For more information on DataONE, see our web site at http://dataone.org.
 *
 * Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * $Id$
 */
package org.dataone.cn.indexer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.codec.EncoderException;
import org.apache.log4j.Logger;
import org.dataone.cn.index.util.PerformanceLogger;
import org.dataone.cn.indexer.parser.IDocumentDeleteSubprocessor;
import org.dataone.cn.indexer.parser.IDocumentSubprocessor;
import org.dataone.cn.indexer.solrhttp.SolrElementAdd;
import org.xml.sax.SAXException;

/**
 *
 * @author waltz
 */
public class MockSolrIndexService extends SolrIndexService {

    /**
     * Top level document processing class.
     *
     * Contains collection of document sub-processors which are used to mine search index data from document objects.
     * Each sub-processor is configured via spring to collect data from different types of documents (by formatId).
     *
     * There should only be one instance of XPathDocumentParser in place at a time since it performs updates on the SOLR
     * index and transactions on SOLR are at the server level - so if multiple threads write and commit then things
     * could get messy.
     *
     */
    private static Logger log = Logger.getLogger(MockSolrIndexService.class);
    private static final String OUTPUT_ENCODING = "UTF-8";
    private List<IDocumentSubprocessor> subprocessors = null;
    private List<IDocumentDeleteSubprocessor> deleteSubprocessors = null;
    private IDocumentSubprocessor systemMetadataProcessor = null;

    private String solrIndexUri = null;

    private String solrQueryUri = null;

    private D1IndexerSolrClient httpService = null;

    private PerformanceLogger perfLog = PerformanceLogger.getInstance();

    public MockSolrIndexService() {
    }

    public void removeFromIndex(String identifier) throws Exception {

        log.info("removeFromIndex");
    }

    /**
     * Given a PID, system metadata input stream, and an optional document path, populate the set of SOLR fields for the
     * document.
     *
     * @param id
     * @param systemMetaDataStream
     * @param objectPath
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws EncoderException
     */
    @Override
    public SolrElementAdd processObject(String id, InputStream systemMetaDataStream,
            String objectPath) throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException, EncoderException {

        SolrElementAdd addCommand = new SolrElementAdd();

        systemMetaDataStream.close();
        return addCommand;
    }

    /**
     * Given a PID, system metadata input stream, and an optional document path, populate the set of SOLR fields for the
     * document and update the index. Note that if the document is a resource map, then records that it references will
     * be updated as well.
     *
     * @param id
     * @param systemMetaDataStream
     * @param objectPath
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws EncoderException
     */
    @Override
    public void insertIntoIndex(String id, InputStream systemMetaDataStream, String objectPath)
            throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException, EncoderException {

        log.info("insertIntoINdex");
    }

    @Override
    public String getSolrindexUri() {
        return solrIndexUri;
    }

    @Override
    public void setSolrIndexUri(String solrindexUri) {
        this.solrIndexUri = solrindexUri;
    }

    @Override
    public void setHttpService(D1IndexerSolrClient service) {
        this.httpService = service;
    }

    @Override
    public D1IndexerSolrClient getHttpService() {
        return httpService;
    }

    @Override
    public String getSolrQueryUri() {
        return solrQueryUri;
    }

    @Override
    public void setSolrQueryUri(String solrQueryUri) {
        this.solrQueryUri = solrQueryUri;
    }

    @Override
    public List<IDocumentSubprocessor> getSubprocessors() {
        if (this.subprocessors == null) {
            this.subprocessors = new ArrayList<IDocumentSubprocessor>();
        }
        return subprocessors;
    }

    @Override
    public List<IDocumentDeleteSubprocessor> getDeleteSubprocessors() {
        if (this.deleteSubprocessors == null) {
            this.deleteSubprocessors = new ArrayList<IDocumentDeleteSubprocessor>();
        }
        return deleteSubprocessors;
    }

    @Override
    public void setSubprocessors(List<IDocumentSubprocessor> subprocessorList) {
        this.subprocessors = subprocessorList;
    }

    @Override
    public void setDeleteSubprocessors(List<IDocumentDeleteSubprocessor> deleteSubprocessorList) {
        this.deleteSubprocessors = deleteSubprocessorList;
    }

    @Override
    public IDocumentSubprocessor getSystemMetadataProcessor() {
        return systemMetadataProcessor;
    }

    @Override
    public void setSystemMetadataProcessor(IDocumentSubprocessor systemMetadataProcessor) {
        this.systemMetadataProcessor = systemMetadataProcessor;
    }

}
