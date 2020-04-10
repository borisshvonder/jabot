package jabot.fileapi;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.stream.Stream;

/**
 * One-shop stop for all file operations. Encapsulating these to their own API allows for unit-testing and can easilly 
 * be scaled to cluster filesystems such as HDFS.
 * 
 * For portability, files are referenced by {@link URI}s having a certain scheme that this filesystem handles. 
 * For example, local filesystem can use <code>file</code> scheme, while HDFS-backed filesystem may use 
 * <code>hdfs</code> scheme.
 * 
 * All {@link URI}s MUST use this scheme, otherwise they will be rejected with {@link InvalidPathException}.
 */
public interface FileApi {
	/** 
	 * @return @notnull scheme that filesystem supports
	 */
	String getScheme();
	
	/**
	 * @param @notnull path
	 * @return true if path exists
	 */
	boolean exists(URI path);
	
	/**
	 * @param @notnull path
	 * @return true if path exists and is a regular file
	 */
	boolean isFile(URI path);
	
	/**
	 * @param @notnull path
	 * @return true if path exists and is a directory
	 */
	boolean isDirectory(URI path);
	
	/**
	 * @param path
	 * @return files in the directory (possibly empty)
	 * @throws NotADirectoryException if path is not a directory (or does not exist)
	 * @throws FileApiException if general error occurs
	 */
	Stream<URI> listDirectory(URI path) throws NotADirectoryException, FileApiException;
	
	/**
	 * Move/rename file/directory to a new name. Note that it does NOT support moving files into a directory like
	 * regular unix mv (<code>mv file /to/dir</code>). The same effect should be archieved using full destination name.
	 * 
	 * Will fail with FileApiException if:
	 * <ul>
	 *   <li>overwrite=false and target exists</li>
	 *   <li>overwrite=true, target is a non-empty directory</li>
	 * </ul>
	 * 
	 * (<code>api.move(file, /to/dir/file)</code>)
	 * @param @notnull source
	 * @param @notnull dest
	 * @param overwrite if true, overwrite destination (will fail if dest is directory)
	 * @throws FileApiException if general error occurs
	 */
	void move(URI source, URI dest, boolean overwrite) throws FileApiException;
	
	/**
	 * Remove a file/empty directory
	 * @param @notnull path
	 * @return true if path was removed, false if it did not exist
	 * @throws FileApiException if general error occurs, for ex. directory is not empty
	 */
	boolean remove(URI path) throws FileApiException;
	
	/**
	 * Make a directory (and all required parent directories as well)
	 * @param @notnull path
	 * @throws FileApiException if general error occurs, for example if stumbled on a file in the given path
	 */
	void mkdirs(URI path) throws FileApiException;
	
	/**
	 * Create new file and return output stream for that file
	 * @param @notnull path
	 * @return @notnull output stream
	 * @throws FileApiException if general error occurs
	 * @throws FileAlreadyExistsException when file already present
	 */
	OutputStream createFile(URI path) throws FileApiException, FileAlreadyExistsException;
	
	/**
	 * Open existing file for appending
	 * 
	 * Note: not all filesystems support opening existing files for writing(HDFS), but most support appending
	 * 
	 * @param @notnull path
	 * @return @notnull output stream
	 * @throws FileApiException if general error occurs
	 * @throws FileNotFoundException when file does not exists or is not a regular file
	 */
	OutputStream appendFile(URI path) throws FileApiException, FileNotFoundException;
	
	/**
	 * Open file for reading
	 * 
	 * @param @notnull path
	 * @return @notnull input stream
	 * @throws FileApiException if general error occurs
	 * @throws FileNotFoundException when file does not exists or is not a regular file
	 */
	InputStream readFile(URI path) throws FileApiException, FileNotFoundException;
}
