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
package com.energysys.filesource.local;

import com.energysys.filesource.ConnectionStatus;
import com.energysys.filesource.FileSourceComparison;
import com.energysys.filesource.IFileSource;
import com.energysys.filesource.IFileSourceFile;
import com.energysys.filesource.IFileSourceFileFilter;
import com.energysys.filesource.exception.FileSourceException;
import software.amazon.awssdk.utils.IoUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An IFileSource that is based in a local directory.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class LocalFileSource implements IFileSource<ILocalFileSourceConfig>
{

  private static final Logger LOG = Logger.getLogger(LocalFileSource.class.getName());
  private static final String SLASH = "/";

  private final ILocalFileSourceConfig theConfig;

  /**
   * Basic constructor.
   *
   * @param aConfig config for file source
   * @throws com.energysys.filesource.exception.FileSourceException
   */
  public LocalFileSource(ILocalFileSourceConfig aConfig) throws FileSourceException
  {
    this.theConfig = aConfig;
    File myBaseDir = new File(aConfig.getLocalDir());
    if (!myBaseDir.canWrite())
    {
      throw new FileSourceException("Local base directory must have write access");
    }
  }

  @Override
  public IFileSourceFile findFile(String anID)
  {
    File myFile = new File(theConfig.getLocalDir() + SLASH + anID);
    if (myFile.exists())
    {
      return new LocalFileSourceFile(myFile, anID, theConfig.getOwner());
    }
    else
    {
      return null;
    }
  }

  @Override
  public ConnectionStatus openConnection()
  {
    return ConnectionStatus.OPEN;
  }

  @Override
  public ConnectionStatus closeConnection()
  {
    return ConnectionStatus.CLOSED;
  }

  @Override
  public void putContent(InputStream anInputStream, IFileSourceFile aFile) throws FileSourceException
  {
    File myLocalFile = new File(theConfig.getLocalDir() + SLASH + aFile.getFileId());
    myLocalFile.getParentFile().mkdirs();

    try (FileOutputStream myFileOutputStream = new FileOutputStream(myLocalFile))
    {
      IoUtils.copy(anInputStream, myFileOutputStream);
      String myResultMessage = "File saved: " + aFile.getFileId();
      LOG.fine(myResultMessage);
    }
    catch (IOException ex)
    {
      throw new FileSourceException("Error writing file content", ex);
    }
  }

  @Override
  public IFileSourceFile getContent(OutputStream anOutputStream, IFileSourceFile aFile) throws FileSourceException
  {
    File myLocalFile = new File(theConfig.getLocalDir() + SLASH + aFile.getFileId());

    try (FileInputStream myFileInputStream = new FileInputStream(myLocalFile))
    {
      IoUtils.copy(myFileInputStream, anOutputStream);
      String myResultMessage = "File retrieved: " + aFile.getFileId();
      LOG.fine(myResultMessage);
      return aFile;
    }
    catch (IOException ex)
    {
      throw new FileSourceException("Error getting file content", ex);
    }
  }

  @Override
  public InputStream getInputStream(IFileSourceFile aFile) throws FileNotFoundException
  {
    File myLocalFile = new File(theConfig.getLocalDir() + SLASH + aFile.getFileId());
    return new FileInputStream(myLocalFile);
  }

  @Override
  public FileSourceComparison compare(IFileSourceFile aFile)
  {
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

  @Override
  public List<IFileSourceFile> findFiles(final String aFileNamePattern, final Boolean isDirRecursive,
          final IFileSourceFileFilter aFilter)
  {
    return scanForFiles(new File(theConfig.getLocalDir()), aFileNamePattern, isDirRecursive, aFilter);
  }

  private List<IFileSourceFile> scanForFiles(File aDirectory, final String aFileNamePattern,
          final Boolean isDirRecursive,
          final IFileSourceFileFilter aFilter)
  {
    ArrayList<IFileSourceFile> myFilesFound = new ArrayList<>();

    FileFilter myBasicFileFilter = new BasicFileFilter(isDirRecursive, aFileNamePattern);

    File[] myMatchingFiles = aDirectory.listFiles(myBasicFileFilter);

    if (myMatchingFiles == null)
    {
      return myFilesFound;
    }
    for (File myMatchingFile : myMatchingFiles)
    {
      if (myMatchingFile.isDirectory())
      {
        myFilesFound.addAll(scanForFiles(myMatchingFile, aFileNamePattern, isDirRecursive, aFilter));
      }
      else
      {
        LocalFileSourceFile myLocalFile = new LocalFileSourceFile(myMatchingFile,
                getFileID(theConfig.getLocalDir(), myMatchingFile), theConfig.getOwner());

        if (aFilter == null || aFilter.accept(myLocalFile))
        {
          myFilesFound.add(myLocalFile);
        }
      }
    }

    return myFilesFound;
  }

  /**
   * Generates a standard file id given a java.io.File and a local base directory name.
   *
   * @param aBaseDir the base dir
   * @param aFile the file
   * @return a file id
   */
  private String getFileID(String aBaseDir, File aFile)
  {
    int myStartingPosition = aBaseDir.length() + 1;

    String myKey = aFile.getAbsolutePath()
            .substring(myStartingPosition)
            .replaceAll("\\\\", "/");
    return myKey;
  }

  @SuppressWarnings("IllegalCatch")
  @Override
  public IFileSourceFile moveFile(IFileSourceFile aFile, String aNewID) throws FileSourceException
  {
    try
    {
      File oldFile = new File(theConfig.getLocalDir() + SLASH + aFile.getFileId());
      File newFile = new File(theConfig.getLocalDir() + SLASH + aNewID);

      newFile.getParentFile().mkdirs();
      Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return findFile(aNewID);
    }
    catch (Exception ex)
    {
      throw new FileSourceException("Error moving file", ex);
    }
  }

  private class BasicFileFilter implements FileFilter
  {

    private final Boolean isDirRecursive;
    private final String theFileNamePattern;

    BasicFileFilter(Boolean isDirRecursive, String aFileNamePattern)
    {
      this.isDirRecursive = isDirRecursive;
      this.theFileNamePattern = aFileNamePattern;
    }

    @Override
    public boolean accept(File aFile)
    {
      // No Hidden files allowed!
      if (aFile.isHidden())
      {
        return false;
      }
      // If is a directory then return true if recursive
      if (aFile.isDirectory())
      {
        return isDirRecursive;
      }
      // Check that the filename matches the filePattern regex, if there is one.
      if (theFileNamePattern != null && !theFileNamePattern.isEmpty() && !aFile.getName().matches(theFileNamePattern))
      {
        return false;
      }
      // Otherwise it matches
      return true;
    }
  };
}
