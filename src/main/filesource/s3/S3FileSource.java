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

import com.energysys.calendar.CurrentDateTime;
import com.energysys.filesource.ConnectionStatus;
import com.energysys.filesource.FileSourceComparison;
import com.energysys.filesource.IFileSource;
import com.energysys.filesource.IFileSourceFile;
import com.energysys.filesource.IFileSourceFileFilter;
import com.energysys.filesource.exception.FileSourceException;
import com.energysys.filesource.exception.FileSourceSystemException;
import com.energysys.filesource.exception.InvalidCredentialsException;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.utils.IoUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An IFileSource for connecting to an S3 bucket on AWS.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@SuppressWarnings("checkstyle:classfanoutcomplexity")
public class S3FileSource implements IFileSource<IS3FileSourceConfig>, AutoCloseable
{
    /**
     * Local modified date tag name.
     */
    protected static final String LOCAL_MODIFIED_DATE = "Local-Modified-Date";
    /**
     * Owner Tag name.
     */
    protected static final String OWNER = "Owner";
    /**
     * Local key (filename) Tag Name.
     */
    protected static final String LOCAL_KEY = "Local-Key";
    /**
     * Slash character.
     */
    protected static final String SLASH = "/";
    private static final Logger LOG = Logger.getLogger(S3FileSource.class.getName());
    private static final Long LOCK_TIMEOUT_MINUTES = 1L;

    private ConnectionStatus theConnectionStatus;
    private S3Client theS3Client;
    private final IS3Catalog theRemoteCatalog;

    private final IS3FileSourceConfig theConfig;
    private final String theLockKey;

    /**
     * Basic constructor.
     *
     * @param aConfig  the config
     * @param aCatalog a catalog defaults to basic S3Catalog
     */
    public S3FileSource(IS3FileSourceConfig aConfig, IS3Catalog aCatalog)
    {
        this.theConfig = aConfig;
        this.theLockKey = ".lock." + theConfig.getLocation();
        theRemoteCatalog = aCatalog;
    }

    /**
     * Basic constructor.
     *
     * @param aConfig the config
     */
    public S3FileSource(IS3FileSourceConfig aConfig)
    {
        this.theConfig = aConfig;
        this.theLockKey = ".lock." + theConfig.getLocation();
        theRemoteCatalog = new S3Catalog();
    }

    @Override
    @SuppressWarnings(
            {
                    "checkstyle:illegalcatch", "UseSpecificCatch"
            })
    public ConnectionStatus openConnection() throws FileSourceException
    {
        try
        {
            Region myRegion;
            try
            {
                myRegion = Region.of(theConfig.getAWSRegion());
            }
            catch (IllegalArgumentException myEx)
            {
                throw new FileSourceException("Invalid Region", myEx);
            }
            theS3Client = S3Client.builder().region(myRegion).credentialsProvider(
                    StaticCredentialsProvider.create(theConfig)).build();
            // Attempt to lock the remote catalog to stop other processes accessing bucket
            // whilst this connector is open
            Boolean myLockResult = getLock();

            // Set status based on result of getLock()
            if (myLockResult)
            {
                // If successful then load the remote catalog
                theRemoteCatalog.openCatalog(theS3Client, theConfig);
                theConnectionStatus = ConnectionStatus.OPEN;
            }
            else
            {
                theConnectionStatus = ConnectionStatus.LOCKED_OUT;
            }

            return theConnectionStatus;
        }
        catch (S3Exception ex)
        {
            String myErrorCode = ex.awsErrorDetails().errorCode();
            if (myErrorCode.equals("InvalidAccessKeyId") || ex.statusCode() == 403)
            {
                throw new InvalidCredentialsException();
            }
            else
            {
                throw new FileSourceException("Could not open connection", ex);
            }
        }
        catch (Exception ex)
        {
            throw new FileSourceException("Error opening connection to S3", ex);
        }

    }

    @Override
    @SuppressWarnings("checkstyle:missingswitchdefault")
    public ConnectionStatus closeConnection()
    {
        if (theConnectionStatus == null)
        {
            theConnectionStatus = ConnectionStatus.CLOSED;
        }
        switch (theConnectionStatus)
        {
            // If connection was already closed or was locked out then there is nothing to do
            case CLOSED:
            case LOCKED_OUT:
                theConnectionStatus = ConnectionStatus.CLOSED;
                break;

            // If connection is open then upload the remote catalog and release the lock
            case OPEN:
                try
                {
                    theRemoteCatalog.closeCatalog();
                    releaseLock();
                    theConnectionStatus = ConnectionStatus.CLOSED;
                    break;
                }
                catch (FileSourceException ex)
                {
                    theConnectionStatus = ConnectionStatus.CLOSED;
                    break;
                }
        }
        return theConnectionStatus;
    }

    @Override
    public IFileSourceFile moveFile(IFileSourceFile aFile, String aNewID)
    {
        theS3Client.copyObject(
                CopyObjectRequest.builder()
                        .sourceBucket(theConfig.getBucketName())
                        .sourceKey(theConfig.getLocation() + S3FileSource.SLASH + aFile.getFileId())
                        .destinationBucket(theConfig.getBucketName())
                        .destinationKey(theConfig.getLocation() + S3FileSource.SLASH + aNewID)
                        .build());

        theS3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(theConfig.getBucketName())
                        .key(theConfig.getLocation() + S3FileSource.SLASH + aFile.getFileId())
                        .build());

        return findFile(aNewID);
    }

    @Override
    public void putContent(InputStream anInputStream, IFileSourceFile aFile) throws FileSourceException
    {
        // Check that connection was opened successfully
        checkOpen();
        try
        {
            final String myRemoteObjectKey = theConfig.getLocation() + SLASH + aFile.getFileId();

            // Upload to S3
            Map<String, String> myS3ObjectMetadata = convertToS3Metadata(aFile);
            Tagging myTagging = Tagging.builder()
                    .tagSet(theConfig.getTags())
                    .build();

            PutObjectRequest myPutRequest = PutObjectRequest.builder()
                    .bucket(theConfig.getBucketName())
                    .key(myRemoteObjectKey)
                    .contentLength(aFile.getSize())
                    .metadata(myS3ObjectMetadata)
                    .tagging(myTagging)
                    .build();

            RequestBody myBody = RequestBody.fromInputStream(anInputStream, aFile.getSize());


            PutObjectResponse myPutResult = theS3Client.putObject(myPutRequest, myBody);

            String myResultMessage = "File uploaded: " + myRemoteObjectKey;
            LOG.fine(myResultMessage);

            // Uncomment if remote catalog is required
            //ObjectMetadata myObjectMetadata =
            // theS3Client.getObjectMetadata(theConfig.getBucketName(), myRemoteObjectKey);
            //final S3FileSourceFile myS3File = new S3FileSourceFile(myObjectMetadata);
            //theRemoteCatalog.update(aFile.getFileId(), myS3File);

            // Touch the lock file
            writeLockFile();
        }
        catch (SdkClientException ex)
        {
            throw new FileSourceException("Failed to put content on S3", ex);
        }
    }

    @Override
    public IFileSourceFile getContent(OutputStream anOutputStream, IFileSourceFile aFile) throws FileSourceException
    {
        // Check connection is open
        checkOpen();
        final String myRemoteObjectKey = theConfig.getLocation() + SLASH + aFile.getFileId();
        //Get the object head
        HeadObjectResponse myHeadResponse = theS3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(theConfig.getBucketName())
                        .key(myRemoteObjectKey)
                        .build());

        GetObjectRequest myGetObjectRequest =
                GetObjectRequest.builder()
                        .bucket(theConfig.getBucketName())
                        .key(myRemoteObjectKey)
                        .build();
        // Copy the object to the given output stream
        try (ResponseInputStream<GetObjectResponse> myObjectInputStream = theS3Client.getObject(myGetObjectRequest))
        {
            IoUtils.copy(myObjectInputStream, anOutputStream);

            String myResultMessage = "File downloaded: " + myRemoteObjectKey;
            LOG.fine(myResultMessage);

            return new S3FileSourceFile(myHeadResponse);
        }
        catch (IOException ex)
        {
            LOG.log(Level.SEVERE, "Error loading remote catalog: {0}", ex.getMessage());
            throw new FileSourceException("Error getting content from S3", ex);
        }
    }

    @Override
    public FileSourceComparison compare(IFileSourceFile aFile)
    {
        checkOpen();
        IFileSourceFile myFile = findFile(aFile.getFileId());

        if (myFile == null)
        {
            return new FileSourceComparison(aFile, FileSourceComparison.Status.NOT_PRESENT);
        }
        switch (aFile.getProducerModifiedDate().compareTo(myFile.getProducerModifiedDate()))
        {
            case 1:
                return new FileSourceComparison(myFile, FileSourceComparison.Status.NEWER);
            case -1:
                return new FileSourceComparison(myFile, FileSourceComparison.Status.OLDER);
            case 0:
            default:
                return new FileSourceComparison(myFile, FileSourceComparison.Status.SAME);
        }
    }

    /**
     * Returns the most recent modified date of any file in the S3 File Source.
     * This can be useful for processes that need to know what has changed since the last download.
     *
     * @return latest update date
     */
    public Date getLatestUpdateDate()
    {
        checkOpen();
        return theRemoteCatalog.getLatestUpdateDate();
    }

    /**
     * Checks that the this object has a currently open connection to the server.
     * If not then throws a SystemException as this should never be the case.
     */
    private void checkOpen()
    {
        // In theory... If the service using the connector is written properly then the connection should always be OPEN
        // if this method is being called. Any other result would have been passed back the service and it should
        // have dealt with the result appropriately and NOT called anything else.
        if (theConnectionStatus == ConnectionStatus.OPEN)
        {
            return;
        }
        throw new FileSourceSystemException(
                "No connection to remote service. Connection status: " + theConnectionStatus.name());
    }

    private void releaseLock()
    {
        if (theS3Client != null && theConfig != null && theConfig.getBucketName() != null && theLockKey != null)
        {
            //Only action if a lock is required
            if (theConfig.isLockRequired())
            {
                LOG.log(Level.FINE, "Releasing Remote Catalog: .Catalog.{0}", theConfig.getLocation());
                // delete the lock file
                theS3Client.deleteObject(
                        DeleteObjectRequest.builder()
                                .bucket(theConfig.getBucketName())
                                .key(theLockKey)
                                .build());
            }
        }
        else
        {
            LOG.log(Level.INFO,
                    "Could not Release Remote Catalog null bucket name: .Catalog.{0}",
                    theConfig.getLocation());
        }
    }

    private Boolean getLock() throws FileSourceException
    {
        if (!theConfig.isLockRequired())
        {
            return true;
        }
        LOG.log(Level.FINE, "Locking Remote Catalog: .Catalog.{0}", theConfig.getLocation());

        // Check no lock file exists
        HeadObjectResponse myHeadObjectResponse =
                S3Utils.headObjectIfExists(theS3Client, theConfig.getBucketName(), theLockKey);
        if (myHeadObjectResponse != null)
        {
            // Check it isn't older than timeout
            long myLockDate = Long.parseLong(myHeadObjectResponse.metadata().get(LOCAL_MODIFIED_DATE));
            long myTimeoutPoint = CurrentDateTime.getCurrentTimeInMillis() - (LOCK_TIMEOUT_MINUTES * 60000);
            if (myLockDate > myTimeoutPoint)
            {
                String myMessage = "Remote Catalog <.Catalog." + theConfig.getLocation() + "> locked by other user: "
                        + myHeadObjectResponse.metadata().get(OWNER);
                LOG.warning(myMessage);
                return false;
            }
        }

        // Write a lock file with a unique id in it so we know it belongs to this process
        String myUniqueID = writeLockFile();

        // wait a little in case another process was attempting to lock at the same time
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException ex)
        {
            throw new FileSourceException("Error locking Remote Catalog: .Catalog." + theConfig.getLocation(), ex);
        }

        // load lock file and ensure that it is still our lock
        // If not then there must have been a conflicting processes run at exactly same time.
        // In this case we have to defer to the other process.
        String myLockString = getObjectAsString(theConfig.getBucketName(), theLockKey);

        if (!myLockString.equals(myUniqueID))
        {
            HeadObjectResponse myLockFileHeadObjectResponse =
                    S3Utils.headObjectIfExists(theS3Client, theConfig.getBucketName(), theLockKey);
            String myMessage = "Remote Catalog <.Catalog." + theConfig.getLocation() + "> locked by other user: "
                    + myLockFileHeadObjectResponse.metadata().get(OWNER);
            LOG.fine(myMessage);
            return false;
        }
        return true;
    }

    private String getObjectAsString(String bucketName, String theLockKey) throws FileSourceException
    {
        ResponseInputStream<GetObjectResponse> myLockResponse =
                theS3Client.getObject(GetObjectRequest.builder()
                        .bucket(theConfig.getBucketName())
                        .key(theLockKey)
                        .build());

        try
        {
            return IoUtils.toUtf8String(myLockResponse);
        }
        catch (IOException e)
        {
            throw new FileSourceException("Failed to get S3 Object content", e);
        }
    }


    private String writeLockFile() throws SdkClientException
    {
        String myUniqueID = UUID.randomUUID().toString();
        ByteArrayInputStream myStream = new ByteArrayInputStream(myUniqueID.getBytes());
        S3FileSourceFile myMetadata
                = new S3FileSourceFile(theLockKey, CurrentDateTime.getCurrentDate(), theConfig.getOwner(),
                Long.valueOf(myUniqueID.length()), "");

        PutObjectRequest myPutRequest = PutObjectRequest.builder()
                .bucket(theConfig.getBucketName())
                .key(theLockKey)
                .metadata(convertToS3Metadata(myMetadata))
                .build();

        PutObjectResponse myResponse =
                theS3Client.putObject(
                        myPutRequest,
                        RequestBody.fromInputStream(myStream, myUniqueID.getBytes().length));
        return myUniqueID;
    }

    private Map<String, String> convertToS3Metadata(IFileSourceFile aFile)
    {
        // Populate the S3 Metadata
        HashMap<String, String> myS3ObjectMetadata = new HashMap<>();
        myS3ObjectMetadata.put(LOCAL_KEY, aFile.getFileId());
        myS3ObjectMetadata.
                put(LOCAL_MODIFIED_DATE,
                        String.valueOf(aFile.getProducerModifiedDate().getTime()));
        if (aFile.getOwner() != null && !aFile.getOwner().isEmpty())
        {
            myS3ObjectMetadata.put(OWNER, aFile.getOwner());
        }
        else
        {
            myS3ObjectMetadata.put(OWNER, theConfig.getOwner());
        }
        return myS3ObjectMetadata;
    }

    @Override
    public void close()
    {
        releaseLock();
        closeConnection();
    }

    @Override
    public IFileSourceFile findFile(String anID)
    {
        return theRemoteCatalog.getFileWithID(anID);

    }

    @Override
    public List<IFileSourceFile> findFiles(final String aFileNamePattern, final Boolean isDirRecursive,
                                           final IFileSourceFileFilter aFilter)
    {
        IFileSourceFileFilter myFileFilter = new IFileSourceFileFilter()
        {
            @Override
            public boolean accept(IFileSourceFile aFile)
            {
                // If the file is in a sub dir then check for recursion
                if (!isDirRecursive && aFile.getFileId().contains(SLASH))
                {
                    return false;
                }
                // Check that the filename matches the filePattern regex, if there is one.
                if (aFileNamePattern != null
                        && !aFileNamePattern.isEmpty() && !aFile.getFileId().matches(aFileNamePattern))
                {
                    return false;
                }
                // Otherwise it matches
                return (aFilter == null || aFilter.accept(aFile));
            }
        };

        return theRemoteCatalog.getAllFiles(myFileFilter);
    }

    @Override
    public InputStream getInputStream(IFileSourceFile aFile)
    {
        // Check connection is open
        checkOpen();
        final String myRemoteObjectKey = theConfig.getLocation() + SLASH + aFile.getFileId();

        //Get the object
        ResponseInputStream<GetObjectResponse> myS3Object = theS3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(theConfig.getBucketName())
                        .key(myRemoteObjectKey)
                        .build());

        return myS3Object;
    }
}