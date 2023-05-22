/*
 * Copyright 2017 EnergySys Limited. All Rights Reserved.
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
package com.energysys.filesource.s3;

import com.energysys.filesource.IFileSourceFile;
import com.energysys.filesource.IFileSourceFileFilter;
import com.energysys.filesource.exception.FileSourceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class describing the remote catalog of objects on an S3 bucket.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class S3RemoteCachedCatalog implements IS3Catalog
{

    private static final Logger LOG = Logger.getLogger(S3RemoteCachedCatalog.class.getName());
    private static final String CATALOG_PREFIX = ".Catalog.";

    private S3Client theS3Client;
    private IS3FileSourceConfig theConfig;

    private Map<String, IFileSourceFile> theCatalog;
    private Date theLatestObjectUpdateDate;

    /**
     * Basic Constructor.
     */
    public S3RemoteCachedCatalog()
    {
    }

    /**
     * Opens and initialises the catalog.
     * @param anS3Client
     * @param aConfig
     * @throws FileSourceException
     */
    public void openCatalog(S3Client anS3Client, IS3FileSourceConfig aConfig) throws FileSourceException
    {
        this.theS3Client = anS3Client;
        this.theConfig = aConfig;
        initialise();
    }

    /**
     * Gets the values of the catalog map.
     *
     * @return all items in the catalog
     */
    @Override
    public List<IFileSourceFile> getAllFiles(IFileSourceFileFilter aFilter)
    {
        List<IFileSourceFile> myFoundFiles = new ArrayList<>();
        for (IFileSourceFile myFile : theCatalog.values())
        {
            if (aFilter.accept(myFile))
            {
                myFoundFiles.add(myFile);
            }
        }
        return myFoundFiles;
    }

    /**
     * Gets the ConnectorObject for a given key.
     *
     * @param anID the key
     * @return the metadata
     */
    @Override
    public IFileSourceFile getFileWithID(String anID)
    {
        if (theCatalog.containsKey(anID))
        {
            return theCatalog.get(anID);
        }

        HeadObjectResponse myHeadObjectResponse =
                S3Utils.headObjectIfExists(
                        theS3Client,
                        theConfig.getBucketName(),
                        theConfig.getLocation() + S3FileSource.SLASH + anID);

        // Not in catalog, check if it exists remotely (and add metadata to catalog)
        if (myHeadObjectResponse != null)
        {
            S3FileSourceFile myConnectorMetadata = new S3FileSourceFile(
                    anID,
                    Date.from(Instant.ofEpochMilli(
                            Long.valueOf(myHeadObjectResponse.metadata().get(S3FileSource.LOCAL_MODIFIED_DATE)))),
                    Date.from(myHeadObjectResponse.lastModified()),
                    myHeadObjectResponse.metadata().get("Owner"),
                    myHeadObjectResponse.contentLength(),
                    myHeadObjectResponse.contentType());

            update(anID, myConnectorMetadata);
            LOG.fine("Remote Catalog updated from S3 for: " + anID);

            return myConnectorMetadata;
        }

        return null;
    }

    /**
     * Updated the catalogs entry for the given key with the given metadata.
     *
     * @param aKey  the key
     * @param aFile the metadata
     */
    public void update(String aKey, S3FileSourceFile aFile)
    {
        theCatalog.put(aKey, aFile);

        if (theLatestObjectUpdateDate == null
                || aFile.getTheRemoteModifiedDate().after(theLatestObjectUpdateDate))
        {
            theLatestObjectUpdateDate = aFile.getTheRemoteModifiedDate();
        }
    }

    /**
     * Returns the most recent modified date of any file in Catalog. This can be useful for processes that need to know
     * what has changed since the last download.
     *
     * @return latest update date
     */
    public Date getLatestUpdateDate()
    {
        return theLatestObjectUpdateDate;
    }

    @Override
    public Boolean containsFile(String aFileID)
    {
        return theCatalog.containsKey(aFileID);
    }

    /**
     * Loads the remote catalog from S3 (if it exists).
     *
     * @return
     * @throws FileSourceException
     */
    private void initialise() throws FileSourceException
    {
        String myCatalogKey = CATALOG_PREFIX + theConfig.getLocation();

        // If there is a catalog present on S3 then download it
        if (S3Utils.headObjectIfExists(theS3Client, theConfig.getBucketName(), myCatalogKey) != null)
        {
            LOG.log(Level.FINE, "Downloading Remote Catalog: {0}", myCatalogKey);
            try (ResponseInputStream<GetObjectResponse> myCatalogResponse =
                         theS3Client.getObject(GetObjectRequest.builder()
                                 .bucket(theConfig.getBucketName())
                                 .key(myCatalogKey)
                                 .build());
                 ObjectInputStream myObjectInputStream = new ObjectInputStream(myCatalogResponse))
            {
                CatalogCache myRemoteCatalog = (CatalogCache) myObjectInputStream.readObject();
                theCatalog = myRemoteCatalog.getTheCatalog();
                theLatestObjectUpdateDate = myRemoteCatalog.getTheLatestObjectUpdateDate();
            }
            catch (IOException | ClassNotFoundException ex)
            {
                throw new FileSourceException("Error loading remote catalog: " + myCatalogKey, ex);
            }
        }
        // Otherwise build catalog
        else
        {
            LOG.log(Level.INFO, "Creating new Remote Catalog: {0}", myCatalogKey);
            theCatalog = new HashMap<String, IFileSourceFile>();
            // populate the  catalog
            ListObjectsResponse myListing =
                    theS3Client.listObjects(ListObjectsRequest.builder()
                            .bucket(theConfig.getBucketName())
                            .prefix(theConfig.getLocation())
                            .build());

            while (myListing.isTruncated())
            {
                List<S3Object> mySummaries = myListing.contents();
                for (S3Object mySummary : mySummaries)
                {
                    HeadObjectResponse myRemoteMetadata =
                            theS3Client.headObject(
                                    HeadObjectRequest.builder()
                                            .bucket(theConfig.getBucketName())
                                            .key(mySummary.key())
                                            .build());
                    S3FileSourceFile myConnectorMetadata = new S3FileSourceFile(myRemoteMetadata);

                    update(myConnectorMetadata.getFileId(), myConnectorMetadata);
                }
            }
        }
    }

    /**
     * Uploads the remote catalog to S3 for use later.
     *
     * @throws FileSourceException
     */
    private void uploadRemoteCatalog() throws FileSourceException
    {
        LOG.log(Level.FINE, "Uploading Remote Catalog: .Catalog.{0}", theConfig.getLocation());
        try
        {
            // Serialise remote catalog to byte array
            ByteArrayOutputStream myBytesOut = new ByteArrayOutputStream();
            ObjectOutputStream myObjectOutputStream = new ObjectOutputStream(myBytesOut);
            CatalogCache myStoredCatalog = new CatalogCache(theCatalog, theLatestObjectUpdateDate);
            myObjectOutputStream.writeObject(myStoredCatalog);

            // Write byte array to S3
            RequestBody myBody = RequestBody.fromBytes(myBytesOut.toByteArray());

            theS3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(theConfig.getBucketName())
                            .key(CATALOG_PREFIX + theConfig.getLocation())
                            .contentLength(Long.valueOf(myBytesOut.size()))
                            .build(),
                    myBody);
        }
        catch (IOException ex)
        {
            throw new FileSourceException(
                    "Error uploading remote catalog: " + CATALOG_PREFIX + theConfig.getLocation(),
                    ex);
        }
    }

    @Override
    public void closeCatalog() throws FileSourceException
    {
        uploadRemoteCatalog();
    }

}
