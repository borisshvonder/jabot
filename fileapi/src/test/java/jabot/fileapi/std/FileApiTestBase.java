package jabot.fileapi.std;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jabot.fileapi.FileAlreadyExistsException;
import jabot.fileapi.FileApi;
import jabot.fileapi.FileApiException;
import jabot.fileapi.FileNotFoundException;
import jabot.fileapi.NotADirectoryException;

public abstract class FileApiTestBase {
	protected abstract FileApi createFixture();
	protected abstract void destroyFixture(FileApi fixture) throws Exception;

	/** this must be an existing empty location */
	protected abstract URI testLocation(FileApi fixture);
	
	private URI testLocation;
	private URI testFile1, testFile2;
	private URI testDir1, testDir2;
	private FileApi fixture;
	
	@Before
	public void setUp() {
		fixture = createFixture();
		testLocation = testLocation(fixture);
		testFile1 = URI.create(testLocation+"testFile1");
		testFile2 = URI.create(testLocation+"testFile2");
		testDir1 = URI.create(testLocation+"testDir1");
		testDir2 = URI.create(testLocation+"testDir2");
	}
	
	@Test
	public void test_create_and_read_file() throws IOException {
		final String message = "Hello world!";
		
		createFile(testFile1, message);
		final String readBack = readFile(testFile1);
		
		Assert.assertEquals(message, readBack);
	}
	
	@Test
	public void test_create_and_read_non_ASCII() throws IOException {
		final String message = "你好，世界\nგამარჯობა მსოფლიო";
		
		createFile(testFile1, message);
		final String readBack = readFile(testFile1);
		
		Assert.assertEquals(message, readBack);
	}
	
	@Test(expected=FileAlreadyExistsException.class)
	public void test_cannot_create_an_existing_file() throws IOException {
		createFile(testFile1, "");
		createFile(testFile1, "");
	}
	
	@Test(expected=FileAlreadyExistsException.class)
	public void test_cannot_write_to_folder() throws IOException {
		fixture.mkdirs(testFile1);
		createFile(testFile1, "");
	}
	
	@Test(expected=FileApiException.class)
	public void test_cannot_create_file_if_folder_does_not_exists() throws IOException {
		createFile(URI.create(testFile1+"/otherFile"), "");
	}
	
	@Test(expected=FileNotFoundException.class)
	public void test_cannot_read_from_missing_file() throws IOException {
		readFile(testFile1);
	}
	
	@Test(expected=IOException.class)
	public void test_cannot_read_from_dir() throws IOException {
		fixture.mkdirs(testFile1);
		readFile(testFile1);
	}
	
	@Test
	public void test_append_to_file() throws IOException {
		createFile(testFile1, "hello ");
		appendFile(testFile1, "world!");
		Assert.assertEquals("hello world!", readFile(testFile1));
	}
	
	@Test(expected=FileNotFoundException.class)
	public void test_cannot_append_to_missing_file() throws IOException {
		appendFile(testFile1, "world!");
	}
	
	@Test(expected=FileApiException.class)
	public void test_cannot_append_to_dir() throws IOException {
		fixture.mkdirs(testFile1);
		appendFile(testFile1, "world!");
	}
	
	@Test
	public void test_missing_file_does_not_exists() throws IOException {
		Assert.assertFalse(fixture.exists(testFile1));
	}

	@Test
	public void test_created_file_exists() throws IOException {
		createFile(testFile1, "");
		Assert.assertTrue(fixture.exists(testFile1));
	}
	
	@Test
	public void test_created_dir_exists() throws IOException {
		fixture.mkdirs(testFile1);
		Assert.assertTrue(fixture.exists(testFile1));
	}
	
	@Test
	public void test_missing_file_is_not_a_file() {
		Assert.assertFalse(fixture.isFile(testFile1));
	}
	
	@Test
	public void test_dir_is_not_a_file() throws FileApiException {
		fixture.mkdirs(testFile1);
		Assert.assertFalse(fixture.isFile(testFile1));
	}
	
	@Test
	public void test_created_file_is_a_file() throws IOException {
		createFile(testFile1, "");
		Assert.assertTrue(fixture.isFile(testFile1));
	}
	
	@Test
	public void test_missing_dir_is_not_a_dir() {
		Assert.assertFalse(fixture.isDirectory(testFile1));
	}
	
	@Test
	public void test_file_is_not_a_dir() throws IOException {
		createFile(testFile1, "");
		Assert.assertFalse(fixture.isDirectory(testFile1));
	}
	
	@Test
	public void test_created_dir_is_a_dir() throws IOException {
		fixture.mkdirs(testDir1);
		Assert.assertTrue(fixture.isDirectory(testDir1));
	}
	
	@Test
	public void test_list_empty_dir() throws IOException {
		fixture.mkdirs(testDir1);
		final Stream<URI> stream = fixture.listDirectory(testDir1);
		Assert.assertEquals(0, stream.count());
	}
	
	@Test
	public void test_list_dir_with_one_file() throws IOException {
		fixture.mkdirs(testDir1);
		final URI file1 = URI.create(testDir1+"/file1");
		createFile(file1, "");
		final Stream<URI> stream = fixture.listDirectory(testDir1);
		final List<URI> result = stream.collect(Collectors.toList());
		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0).toString().endsWith("/file1"));
		Assert.assertTrue(fixture.isFile(result.get(0)));
	}
	
	@Test
	public void test_listDirectory_with_one_subdir() throws IOException {
		final URI dir2 = URI.create(testDir1+"/dir2");
		fixture.mkdirs(dir2);
		final Stream<URI> stream = fixture.listDirectory(testDir1);
		final List<URI> result = stream.collect(Collectors.toList());
		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0).toString().endsWith("/dir2"));
		Assert.assertTrue(fixture.isDirectory(result.get(0)));
	}
	
	@Test(expected=NotADirectoryException.class)
	public void test_listDirectory_not_supported_on_files() throws IOException {
		createFile(testFile1, "");
		fixture.listDirectory(testFile1);
	}
	
	@Test
	public void test_listDirectory_complex_usecase() throws IOException {
		fixture.mkdirs(URI.create(testDir1+"/dir2/dir3"));
		createFile(URI.create(testDir1+"/file1"), "file1.contents");
		createFile(URI.create(testDir1+"/dir2/file2"), "file2.contents");
		createFile(URI.create(testDir1+"/dir2/dir3/file3"), "file3.contents");
		
		List<URI> result = fixture.listDirectory(testDir1).collect(Collectors.toList());
		Assert.assertEquals(2, result.size());
		
		result = fixture.listDirectory(URI.create(testDir1+"/dir2")).collect(Collectors.toList());
		Assert.assertEquals(2, result.size());
		
		result = fixture.listDirectory(URI.create(testDir1+"/dir2/dir3")).collect(Collectors.toList());
		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0).toString().endsWith("/file3"));
		Assert.assertTrue(fixture.isFile(result.get(0)));
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_missing_file_to_itself() throws IOException {
		fixture.move(testFile1, testFile1, true);
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_missing_file_to_another_file() throws IOException {
		fixture.move(testFile1, testFile2, true);
	}
	
	@Test
	public void test_move_existing_file_to_same_file() throws IOException {
		createFile(testFile1, "");
		fixture.move(testFile1, testFile1, false);
	}
	
	@Test
	public void test_move_existing_file_to_anoter_non_existing_file() throws IOException {
		createFile(testFile1, "");
		fixture.move(testFile1, testFile2, false);
		Assert.assertFalse(fixture.exists(testFile1));
		Assert.assertTrue(fixture.exists(testFile2));
	}

	@Test(expected=FileApiException.class)
	public void test_move_cannot_overwrite_existing_file() throws IOException {
		createFile(testFile1, "");
		createFile(testFile2, "");
		fixture.move(testFile1, testFile2, false);
	}
	
	@Test
	public void test_move_overwrite_existing_file() throws IOException {
		createFile(testFile1, "testFile1");
		createFile(testFile2, "testFile2");
		fixture.move(testFile1, testFile2, true);
		Assert.assertFalse(fixture.exists(testFile1));
		Assert.assertTrue(fixture.exists(testFile2));
		Assert.assertEquals("testFile1", readFile(testFile2));
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_cannot_move_file_over_empty_dir() throws IOException {
		createFile(testFile1, "");
		fixture.mkdirs(testDir2);
		fixture.move(testFile1, testDir2, false);
	}
	
	@Test
	public void test_move_can_move_file_over_dir_when_overwriting() throws IOException {
		createFile(testFile1, "");
		fixture.mkdirs(testDir2);
		fixture.move(testFile1, testDir2, true);
		Assert.assertFalse(fixture.exists(testFile1));
		Assert.assertTrue(fixture.isFile(testDir2));
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_cannot_move_file_over_nonempty_dir() throws IOException {
		createFile(testFile1, "");
		fixture.mkdirs(URI.create(testDir2+"/dir2"));
		fixture.move(testFile1, testDir2, false);
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_cannot_move_file_over_nonempty_dir_even_when_overwriting() throws IOException {
		createFile(testFile1, "");
		fixture.mkdirs(URI.create(testDir2+"/dir2"));
		fixture.move(testFile1, testDir2, true);
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_cannot_move_dir_over_file() throws IOException {
		createFile(testFile1, "");
		fixture.mkdirs(testDir2);
		fixture.move(testDir2, testFile1, false);
	}
	
	@Test
	public void test_move_can_move_dir_over_file_when_overwriting() throws IOException {
		createFile(testFile1, "");
		fixture.mkdirs(testDir2);
		fixture.move(testDir2, testFile1, true);
		Assert.assertFalse(fixture.exists(testDir2));
		Assert.assertTrue(fixture.isDirectory(testFile1));
	}
	
	@Test
	public void test_move_can_move_dir_over_same_dir() throws IOException {
		fixture.mkdirs(testDir2);
		fixture.move(testDir2, testDir2, false);
	}
	
	@Test
	public void test_move_can_move_dir_over_non_existing_dir() throws IOException {
		fixture.mkdirs(testDir1);
		fixture.move(testDir1, testDir2, false);
		Assert.assertFalse(fixture.exists(testDir1));
		Assert.assertTrue(fixture.isDirectory(testDir2));
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_cannot_move_dir_over_empty_existing_dir() throws IOException {
		fixture.mkdirs(testDir1);
		fixture.mkdirs(testDir2);
		fixture.move(testDir1, testDir2, false);
	}
	
	@Test
	public void test_move_can_move_dir_over_empty_existing_dir_when_overwriting() throws IOException {
		fixture.mkdirs(testDir1);
		fixture.mkdirs(testDir2);
		fixture.move(testDir1, testDir2, true);
		Assert.assertFalse(fixture.exists(testDir1));
		Assert.assertTrue(fixture.isDirectory(testDir2));
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_cannot_move_dir_over_nonempty_existing_dir() throws IOException {
		fixture.mkdirs(testDir1);
		fixture.mkdirs(URI.create(testDir2+"/dir2"));
		fixture.move(testDir1, testDir2, false);
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_cannot_move_dir_over_nonempty_existing_dir_even_when_overwriting() throws IOException {
		fixture.mkdirs(testDir1);
		fixture.mkdirs(URI.create(testDir2+"/dir2"));
		fixture.move(testDir1, testDir2, true);
	}
	
	@Test
	public void test_move_can_move_non_empty_dir_over_nonexisting_dir() throws IOException {
		fixture.mkdirs(URI.create(testDir1+"/dir2"));
		fixture.move(testDir1, testDir2, false);
		Assert.assertFalse(fixture.exists(testDir1));
		Assert.assertTrue(fixture.isDirectory(testDir2));
		Assert.assertTrue(fixture.isDirectory(URI.create(testDir2+"/dir2")));
	}
	
	@Test(expected=FileApiException.class)
	public void test_move_can_move_non_empty_dir_over_existing_empty_dir() throws IOException {
		fixture.mkdirs(URI.create(testDir1+"/dir2"));
		fixture.mkdirs(testDir2);
		fixture.move(testDir1, testDir2, false);
	}
	
	@Test
	public void test_move_can_overwrite_non_empty_dir_over_existing_empty_dir() throws IOException {
		fixture.mkdirs(URI.create(testDir1+"/dir2"));
		fixture.mkdirs(testDir2);
		fixture.move(testDir1, testDir2, true);
		Assert.assertFalse(fixture.exists(testDir1));
		Assert.assertTrue(fixture.isDirectory(testDir2));
		Assert.assertTrue(fixture.isDirectory(URI.create(testDir2+"/dir2")));
	}
	
	@Test
	public void test_remove_non_existing() throws IOException {
		Assert.assertFalse(fixture.remove(testFile1));
	}
	
	@Test
	public void test_remove_existing_file() throws IOException {
		createFile(testFile1, "");
		Assert.assertTrue(fixture.remove(testFile1));
		Assert.assertFalse(fixture.exists(testFile1));
	}
	
	@Test
	public void test_remove_existing_empty_dir() throws IOException {
		fixture.mkdirs(testDir1);
		Assert.assertTrue(fixture.remove(testDir1));
		Assert.assertFalse(fixture.exists(testDir1));
	}
	
	@Test(expected=FileApiException.class)
	public void test_remove_cannot_remove_existing_non_empty_dir() throws IOException {
		fixture.mkdirs(URI.create(testDir1+"/dir2"));
		fixture.remove(testDir1);
	}
	
	@Test(expected=FileApiException.class)
	public void test_mkdirs_cannot_create_dir_with_regular_file_in_path() throws IOException {
		createFile(testFile1, "");
		fixture.mkdirs(URI.create(testFile1+"/dir2"));
	}

	private void createFile(final URI uri, final String contents) throws IOException {
		try(OutputStream out = fixture.createFile(uri)) {
			final PrintStream print = new PrintStream(out, false, "UTF-8");
			print.print(contents);
			print.close();
		}
	}
	
	private void appendFile(final URI uri, final String contents) throws IOException {
		try(OutputStream out = fixture.appendFile(uri)) {
			final PrintStream print = new PrintStream(out, false, "UTF-8");
			print.print(contents);
			print.close();
		}
	}
	
	private String readFile(final URI uri) throws IOException {
		final StringBuilder readBack = new StringBuilder();
		try(InputStream in = fixture.readFile(uri)) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line = reader.readLine();
			while (line != null) {
				readBack.append(line);
				line = reader.readLine();
				if (line != null) {
					readBack.append('\n');
				}
			}
		}
		return readBack.toString();
	}
}
