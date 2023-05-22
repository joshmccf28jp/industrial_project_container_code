/*
 *  Copyright 2014 EnergySys Limited. All Rights Reserved.
 * 
 *  This software is the proprietary information of EnergySys Limited.
 *  Use is subject to licence terms.
 *  This software is not designed or supplied to be used in circumstances where
 *  personal injury, death or loss of or damage to property could result from any
 *  defect in the software.
 *  In no event shall the developers or owners be liable for personal injury,
 *  death or loss or damage to property, loss of business, revenue, profits, use,
 *  data or other economic advantage or for any indirect, punitive, special,
 *  incidental, consequential or exemplary loss or damage resulting from the use
 *  of the software or documentation.
 *  Developer and owner make no warranties, representations or undertakings of
 *  any nature in relation to the software and documentation.
 */
package com.energysys.connector.util.odata.client;

import com.energysys.connector.exception.ConnectorException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is responsible for providing helper methods to deal with xml documents.
 * 
 * @author EnergySys Limited
 * @version $Revision$
 * Last modified by: $Author$
 */
public final class ODataXMLHelper 
{
  private static final Logger LOG = Logger.getLogger(ODataXMLHelper.class.getName());
  
  private ODataXMLHelper()
  {
    
  }

  /**
   * Read a DOM Document from the provided input stream.
   * @param anInputStream an input stream
   * @return a DOM Document
   * @throws ConnectorException on error
   */
  public static Document readDocument(InputStream anInputStream) throws ConnectorException
  {
    Document myDocument = null;
    try
    {
      InputSource myXmlSource = new InputSource(anInputStream);
      DocumentBuilderFactory myDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
      myDocumentBuilderFactory.setNamespaceAware(true);
      myDocumentBuilderFactory.setIgnoringElementContentWhitespace(true);
      DocumentBuilder myBuilder = myDocumentBuilderFactory.newDocumentBuilder();
      myDocument = myBuilder.parse(myXmlSource);
    }
    catch (SAXException | IOException | ParserConfigurationException myEx)
    {
      throw new ConnectorException("Failed to read document from input stream.", myEx);
    }
    return myDocument;
  }

  /**
   * Return a DOM document as a string containing XML.
   *
   * @param aNode the DOM node to be converted
   * @return String the string containing XML
   * @throws ConnectorException on error
   */
  public static String serialize(Node aNode) throws ConnectorException
  {
    String myDocument = null;
    if (aNode != null)
    {
      StringWriter myWriter = new StringWriter();
      Source mySource = new DOMSource(aNode);
      try
      {
        Result myResult = new StreamResult(myWriter);
        Transformer myTransformer = TransformerFactory.newInstance().newTransformer();
        myTransformer.transform(mySource, myResult);
      }
      catch (TransformerException myEx)
      {
        throw new ConnectorException("Failed to serialise document", myEx);
      }
      myDocument = myWriter.toString();
    }
    return myDocument;
  }

}
