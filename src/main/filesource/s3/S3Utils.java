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

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
/**
 * Common Utility classes for S3 interaction.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public final class S3Utils
{
    private S3Utils()
    { }

    /**
     * Heads an object, if it exists.
     * @param aS3Client
     * @param bucketName
     * @param theLockKey
     * @return the Head Object response or null if it doesn't exist
     */
    public static HeadObjectResponse headObjectIfExists(
            S3Client aS3Client, String bucketName, String theLockKey)
    {
        try
        {
            return aS3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(theLockKey)
                            .build());
        }
        catch (NoSuchKeyException e)
        {
            return null;
        }
    }
}
