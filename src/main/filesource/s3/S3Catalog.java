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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Catalog of S3 bucket (location).
 * @author EnergySys Limited
 */
public class S3Catalog implements IS3Catalog
{
    private S3Client theS3Client;
    private IS3FileSourceConfig theConfig;

    /**
     * Default constructor.
     */
    public S3Catalog()
    {
    }

    /**
     * Opens the catalog.
     * @param anS3Client
     * @param aConfig
     * @throws FileSourceException
     */
    public void openCatalog(S3Client anS3Client, IS3FileSourceConfig aConfig) throws FileSourceException
    {
        this.theS3Client = anS3Client;
        this.theConfig = aConfig;
    }

    @Override
    public List<IFileSourceFile> getAllFiles(IFileSourceFileFilter aFilter)
    {
        List<IFileSourceFile> myFiles = new ArrayList<>();

        ListObjectsResponse myListing = theS3Client.listObjects(
                ListObjectsRequest.builder()
                        .bucket(theConfig.getBucketName())
                        .prefix(theConfig.getLocation())
                        .build());

        while (myListing.isTruncated())
        {
            List<S3Object> mySummaries = myListing.contents();
            for (S3Object mySummary : mySummaries)
            {
                HeadObjectResponse myRemoteMetadata = theS3Client.headObject(
                        HeadObjectRequest.builder()
                                .bucket(theConfig.getBucketName())
                                .key(mySummary.key())
                                .build());
                S3FileSourceFile myConnectorMetadata = new S3FileSourceFile(myRemoteMetadata);

                if (aFilter.accept(myConnectorMetadata))
                {
                    myFiles.add(myConnectorMetadata);
                }
            }
        }
        return myFiles;
    }

    @Override
    public IFileSourceFile getFileWithID(String anID)
    {
        if (containsFile(anID))
        {
            HeadObjectResponse myMetadata = theS3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(theConfig.getBucketName())
                            .key(theConfig.getLocation() + S3FileSource.SLASH + anID)
                            .build());
            S3FileSourceFile myConnectorMetadata = new S3FileSourceFile(
                    anID,
                    Date.from(Instant.ofEpochMilli(Long.valueOf(
                            myMetadata.metadata().get(S3FileSource.LOCAL_MODIFIED_DATE)))),
                    Date.from(myMetadata.lastModified()),
                    myMetadata.metadata().get("Owner"),
                    myMetadata.contentLength(),
                    myMetadata.contentType());

            return myConnectorMetadata;
        }
        else
        {
            return null;
        }
    }

    @Override
    public Boolean containsFile(String anID)
    {
        return S3Utils.headObjectIfExists(
                theS3Client,
                theConfig.getBucketName(),
                theConfig.getLocation() + S3FileSource.SLASH + anID) != null;
    }

    @Override
    public void closeCatalog() throws FileSourceException
    {
        return;
    }

    @Override
    public void update(String aKey, S3FileSourceFile aFile)
    {
        return;
    }

    @Override
    public Date getLatestUpdateDate()
    {
        throw new UnsupportedOperationException(
                "getLatestUpdateDate() not supported by S3Catalog. Use S3RemoteCachedCatalog instead.");
    }

}
