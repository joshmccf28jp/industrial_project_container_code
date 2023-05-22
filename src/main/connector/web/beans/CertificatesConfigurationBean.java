/*
 * Copyright 2015 EnergySys Limited. All Rights Reserved.
 *
 * This software is the proprietary information of EnergySys Limited.
 * Use is subject to licence terms.
 * This software is not designed or supplied to be used in circumstances where
 * personal injury, death or loss of or damage to property could result from any
 * defect in the software.
 * In no event shall the developers or owners be liable for personal injury,
 * death or loss or damage to property, loss of business, revenue, profits, use,
 * data or other economic advantage or for any indirect, punitive, special,
 * incidental, consequential or exemplary loss or damage resulting from the use
 * of the software or documentation.
 * Developer and owner make no warranties, representations or undertakings of
 * any nature in relation to the software and documentation.
 */
package com.energysys.connector.web.beans;

import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.keystore.CertificateStoreDAO;
import com.energysys.connector.keystore.StoredCertificate;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;


/**
 * This is the session scoped backing bean used for configuration.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
@ManagedBean(name = "CertificatesConfigurationBean")
@ViewScoped
public class CertificatesConfigurationBean extends AbstractDataListBackingBean<StoredCertificate>
{

  private static final String LINE_SEPERATOR = System.getProperty("line.separator");
  private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----" + LINE_SEPERATOR;
  private static final String END_CERT = LINE_SEPERATOR + "-----END CERTIFICATE-----";
  private static final String NOT_SUPPORTED = "Not supported.";

  /**
   * Create an instance of this bean.
   */
  public CertificatesConfigurationBean()
  {
    super();
  }

  @Override
  protected Class getDataClass()
  {
    return StoredCertificate.class;
  }

  @Override
  protected void loadData()
  {
    try
    {
      CertificateStoreDAO myDAO = new CertificateStoreDAO();
      setData(myDAO.getCertificates());
    }
    catch (ConnectorException myEx)
    {
      addGrowlMessage(myEx);
    }
  }

  @Override
  protected Boolean preDeleteProcess(StoredCertificate aDataObject)
  {
    return true;
  }

  @Override
  protected Boolean delete(StoredCertificate aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException(NOT_SUPPORTED);
  }

  private static String formatCrtFileContents(final StoredCertificate aCertificate) throws ConnectorException
  {
    try
    {
      Base64.Encoder myEncoder = Base64.getEncoder();
      String myCertContents = new String(myEncoder.encode(aCertificate.getCertificate().getEncoded()));

      return BEGIN_CERT + myCertContents + END_CERT;
    }
    catch (CertificateEncodingException ex)
    {
      throw new ConnectorException("Error encoding certificate contents", ex);
    }
  }

  /**
   * Gets a download stream for the selected certificate.
   *
   * @return the stream
   */
  @SuppressWarnings(
  {
    "checkstyle:illegalcatch", "UseSpecificCatch"
  })
  public StreamedContent getDownload()
  {
    try
    {
      InputStream myStream = new ByteArrayInputStream(formatCrtFileContents(getSelectedRow()).getBytes("UTF-8"));
      return DefaultStreamedContent.builder()
              .contentType("application/x-pem-file")
              .stream(() -> myStream)
              .name(getSelectedRow().getName() + ".pem")
              .build();
    }
    catch (Exception myEx)
    {
      addGrowlMessage(myEx);
      return null;
    }
  }

  /**
   * Uploads certificate.
   *
   * @param anEvent File upload event
   */
  public synchronized void onUpload(FileUploadEvent anEvent)
  {

    throw new UnsupportedOperationException(NOT_SUPPORTED);
  }

  @Override
  protected Boolean saveUpdate(StoredCertificate aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException(NOT_SUPPORTED);
  }

  @Override
  protected Boolean saveCreate(StoredCertificate aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException(NOT_SUPPORTED);
  }

}
