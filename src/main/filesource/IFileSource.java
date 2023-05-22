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
package com.energysys.filesource;

import com.energysys.filesource.exception.FileSourceException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Interface for File Sources.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * @param <T> the type of config this file source expects
 */
public interface IFileSource<T extends IFileSourceConfig>
{

  /**
   * Find files in the file source that match a given file pattern and are accepted by a given IFileSourceFileFilter.
   *
   * @param aFileNamePattern the filename pattern
   * @param isDirRecursive whether to recurse down directories
   * @param aFilter the filter
   * @return list of matching IFileSourceFile
   */
  List<IFileSourceFile> findFiles(String aFileNamePattern, Boolean isDirRecursive, IFileSourceFileFilter aFilter);

  /**
   * Finds a file in the file source with the given id.
   *
   * @param anID the id
   * @return IFileSourceFile if file exists
   */
  IFileSourceFile findFile(String anID);

  /**
   * Opens a connection to the file source.
   *
   * @return result
   * @throws com.energysys.filesource.exception.FileSourceException
   */
  ConnectionStatus openConnection() throws FileSourceException;

  /**
   * Writes the given content to the file on the file source.
   *
   * @param anInputStream input stream of the content
   * @param aFile the file to write to
   * @throws com.energysys.filesource.exception.FileSourceException
   */
  void putContent(InputStream anInputStream, IFileSourceFile aFile) throws FileSourceException;

  /**
   * Gets the content of a file on the file source. Callers are responsible for closing the output stream.
   *
   * @param anOutputStream output stream to write content to
   * @param aFile the file
   * @return result
   * @throws com.energysys.filesource.exception.FileSourceException
   */
  IFileSourceFile getContent(OutputStream anOutputStream, IFileSourceFile aFile) throws FileSourceException;

  /**
   * Compares the given file with the version on this file source.
   *
   * @param someLocalMetadata the object to compare
   * @return the result
   */
  FileSourceComparison compare(IFileSourceFile someLocalMetadata);

  /**
   * Closes the connection to the file source.
   *
   * @return result
   */
  ConnectionStatus closeConnection();

  /**
   * Gets an input stream for a given file. Useful for streaming data between to file sources. Callers are responsible
   * for closing the input stream.
   *
   * @param aFile a file
   * @return an input stream
   * @throws FileNotFoundException if file does not exist in file source
   */
  InputStream getInputStream(IFileSourceFile aFile) throws FileNotFoundException;

  /**
   * Move a file.
   * @param aFile
   * @param aNewID
   * @return the new file
   * @throws FileSourceException
   */
  IFileSourceFile moveFile(IFileSourceFile aFile, String aNewID) throws FileSourceException;
}
