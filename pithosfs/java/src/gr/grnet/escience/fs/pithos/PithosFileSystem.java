package gr.grnet.escience.fs.pithos;

import gr.grnet.escience.pithos.rest.HadoopPithosConnector;
import gr.grnet.escience.pithos.rest.PithosResponse;
import gr.grnet.escience.pithos.rest.PithosResponseFormat;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

/**
 * This class implements a custom file system based on FIleSystem class of
 * Hadoop 2.6.0. Essentially the main idea here, respects to the development of
 * a custom File System that will be able to allow the interaction between
 * hadoop and pithos storage system.
 * 
 * @since March, 2015
 * @author Dimitris G. Kelaidonis & Ioannis Stenos
 * @version 0.1
 * 
 */
public class PithosFileSystem extends FileSystem {

	private URI uri;

	private Path workingDir;
	public static final Log LOG = LogFactory.getLog(PithosFileSystem.class);

	public PithosFileSystem() {
	}

	public String getConfig(String param) {
		Configuration conf = new Configuration();
		String result = conf.get(param);
		return result;
	}

	/**
	 * @return the instance of hadoop - pithos connector
	 */
	public static HadoopPithosConnector getHadoopPithosConnector() {
		return hadoopPithosConnector;
	}

	/**
	 * Set thes instance of hadoop - pithos connector
	 */
	public static void setHadoopPithosConnector(
			HadoopPithosConnector hadoopPithosConnector) {
		PithosFileSystem.hadoopPithosConnector = hadoopPithosConnector;
	}

	@Override
	public String getScheme() {
		System.out.println("getScheme!");
		return "pithos";
	}

	@Override
	public URI getUri() {
		System.out.println("GetUri!");
		return uri;
	}

	@Override
	public void initialize(URI uri, Configuration conf) throws IOException {
		super.initialize(uri, conf);
		System.out.println("Initialize!");
		setConf(conf);
		this.uri = URI.create(uri.getScheme() + "://" + uri.getAuthority());
		System.out.println(this.uri.toString());
		this.workingDir = new Path("/user", System.getProperty("user.name"));
        this.workingDir = new Path("/user", System.getProperty("user.name"))
				.makeQualified(this.uri, this.getWorkingDirectory());
		System.out.println(this.workingDir.toString());
		System.out.println("Create System Store connector");

		// - Create instance of Hadoop connector
		setHadoopPithosConnector(new HadoopPithosConnector(
				getConfig("fs.pithos.url"), getConfig("auth.pithos.token"),
				getConfig("auth.pithos.uuid")));

	}

	@Override
	public Path getWorkingDirectory() {
		System.out.println("getWorkingDirectory!");
		return workingDir;
	}

	@Override
	public void setWorkingDirectory(Path dir) {
		System.out.println("SetWorkingDirectory!");
		workingDir = makeAbsolute(dir);
	}

	private Path makeAbsolute(Path path) {
		if (path.isAbsolute()) {
			return path;
		}
		return new Path(workingDir, path);
	}

	/** This optional operation is not yet supported. */
	@Override
	public FSDataOutputStream append(Path f, int bufferSize,
			Progressable progress) throws IOException {
		System.out.println("append!");
		throw new IOException("Not supported");
	}

	@Override
	public long getDefaultBlockSize() {
		System.out.println("blockSize!");
		return getConf().getLong("fs.pithos.block.size", 4 * 1024 * 1024);
	}

	@Override
	public String getCanonicalServiceName() {
		System.out.println("getcanonicalservicename!");
		// Does not support Token
		return null;
	}

	@Override
	public FileStatus getFileStatus(Path arg0) throws IOException {
		System.out.println("here in getFileStatus BEFORE!");

		long pf_size = getHadoopPithosConnector().getPithosObjectSize("pithos",
				"server.txt");
		long pf_bsize = getHadoopPithosConnector().getPithosObjectBlockSize(
				"pithos", "server.txt");

		try {
			FileStatus pithos_file_status = new FileStatus(pf_size, false, 1,
					pf_bsize, 0, arg0);
			System.out.println("here in getFileStatus AFTER!");
			return pithos_file_status;
		} catch (Exception e) {
			System.out.println("URI exception thrown");
			return null;
		}
	}


	@Override
	public FSDataOutputStream create(Path arg0, FsPermission arg1,
			boolean arg2, int arg3, short arg4, long arg5, Progressable arg6)
			throws IOException {
		System.out.println("Create!");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(Path arg0, boolean arg1) throws IOException {
		System.out.println("Delete!");
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public PithosFileStatus getFileStatus(Path arg0) throws IOException {
		boolean exist = true, isDir = false;
		long length = 0;
		PithosFileStatus pithos_file_status = null;
		System.out.println("here in getFileStatus BEFORE!");
		System.out.println("Path: " + arg0.toString());
		// long length = conn.getPithosObjectBlockSize("pithos",
		// arg0.toString());
		// System.out.println("length: " + length);
		// FileStatus pithos_file_status = new FileStatus(12345, false,0,
		// this.getDefaultBlockSize(),0,
		// 0, null, null, null, arg0);
		// int x = conn.readPithosObject("", "pithosFile.txt").available();
		// System.out.println(x);
		// System.out.println("X = " + x);
		HadoopPithosRestConnector conn = new HadoopPithosRestConnector(
				getConfig("fs.pithos.url"), getConfig("auth.pithos.token"),
				getConfig("auth.pithos.uuid"));
		/*---Check if file exist in pithos------------------------------------*/
		String pathStr = arg0.toString();
		pathStr = pathStr.substring(pathStr.lastIndexOf(pathStr) + 9);
		String pathSplit[] = pathStr.split("/");
		String container = pathSplit[0];
		System.out.println("Container: " + container);
		String filename = pathSplit[pathSplit.length-1];
		int count = 2;
		while (pathSplit[pathSplit.length-count] != container){
			filename = pathSplit[pathSplit.length-count]+"/"+filename;
			count ++;
		}
//		String filename = arg0.toString().substring(
//				arg0.toString().lastIndexOf('/') + 1, arg0.toString().length());
//		System.out.println(filename);

		PithosResponse metadata = conn.getPithosObjectMetaData(container,
				filename, PithosResponseFormat.JSON);
//		System.out.println("metadata: " + metadata.toString());

		// JSONObject obj = new JSONObject(metadata.toString());
		// String objExist =
		// obj.getJSONObject("pithosResponse").getString("null");
		if (metadata.toString().contains("404")) {
			System.out.println("File does not exist in Pithos FS.");
			exist = false;
		}
		/*---------------------------------------------------------*/
		if (exist) {
			for (String obj : metadata.getResponseData().keySet()) {
				if (obj != null) {
					if (obj.matches("Content-Type")) {
						for (String fileType : metadata.getResponseData()
								.get(obj)) {
							if (fileType.contains("application/directory")) {
								isDir = true;
								break;
							} else {
								isDir = false;
							}
						}
					}

				}
			}

			// String getContentType =
			// obj.getJSONObject("pithosResponse").getString("Content-Type");
			// String contentType;
			// for(String obj : metadata.getResponseData().keySet()){
			// if(obj.equals("Content-Type")){
			// contentType = metadata.getResponseData().get(key)
			// }
			// }
			// // int left0 = getContentType.indexOf("[\"");
			// // int right0 = getContentType.indexOf("\"]");
			// // String isDirOrFile = getContentType.substring(left0+2,
			// right0);
			// if (isDirOrFile.contains("directory")){
			// isDir = true;
			// }
			//
			// String lastMod =
			// obj.getJSONObject("pithosResponse").getString("Last-Modified");
			// int left1 = lastMod.indexOf("[\"");
			// int right1 = lastMod.indexOf("\"]");
			// String lastModified = lastMod.substring(left1+2, right1);
			// System.out.println("modification date : " + lastModified);

			if (isDir) {
				pithos_file_status = new PithosFileStatus(true, false, arg0); // arg0.makeQualified(this.uri,
																				// this.workingDir));
			} else {
//					String contentLength = obj.getJSONObject("pithosResponse")
//							.getString("Content-Length");
//					int left = contentLength.indexOf("[\"");
//					int right = contentLength.indexOf("\"]");
//					String objlength = contentLength.substring(left + 2, right);
//					length = Long.parseLong(objlength);
//					System.out.println("object length: " + length);
				
				for (String obj : metadata.getResponseData().keySet()) {
					if (obj != null) {
						if (obj.matches("Content-Length")) {
							for (String lengthStr : metadata.getResponseData()
									.get(obj)) {
								length = Long.parseLong(lengthStr);
							}
						}

					}
				}
				pithos_file_status = new PithosFileStatus(length, 123, arg0);
			}
		}

		System.out.println("here in getFileStatus AFTER!");
		return pithos_file_status;
	}

	@Override
	public FileStatus[] listStatus(Path f) throws FileNotFoundException,
			IOException {
		System.out.println("\n--->  List Status Method!");

		HadoopPithosRestConnector conn = new HadoopPithosRestConnector(
				getConfig("fs.pithos.url"), getConfig("auth.pithos.token"),
				getConfig("auth.pithos.uuid"));
		/*----List from pithos rest communication-----*/
//		String container = f.getParent().toString();
//		container = container.substring(container.lastIndexOf(container) + 9);
//		container = container.substring(0, container.length() - 1);
		String pathStr = f.toString();
		pathStr = pathStr.substring(pathStr.lastIndexOf(pathStr) + 9);
		String pathSplit[] = pathStr.split("/");
		String container = pathSplit[0];
		String conList = conn.getContainerList(container);
		// System.out.println("Container List: \n" + conList);
		/*--------------------------------------------*/
//		String folder = f.toString().substring(
//				f.toString().lastIndexOf('/') + 1, f.toString().length());
		String targetFolder = pathSplit[pathSplit.length-1];
//		System.out.println("path without: " + Path.getPathWithoutSchemeAndAuthority(f));

		final List<FileStatus> result = new ArrayList<FileStatus>();
		FileStatus fileStatus; 
		
		String files[] = conList.split("\\r?\\n");
		for (int i = 0; i < files.length; i++) {
			if (files[i].contains(targetFolder + "/")) {
//			if (files[i] == ) {
				Path path = new Path(this.getScheme()+"://"+container+"/"+files[i]);
				fileStatus = getFileStatus(path);
				System.out.println(files[i]);
				result.add(fileStatus);
			}
		}

		// - Add Serial Port parameters
		// conf.set("hadoop.job.ugi", "hduser");
		// File pithosActualObject = conn.getPithosObject(container,
		// f.toString(), "/user/hduser");
		// System.out.println("File name: " + pithosActualObject.getName());
		// FileStatus[] status = fs.listStatus(f);
		// for(int i=0;i<status.length;i++){
		// System.out.println(status[i].getPath());
		// }
		return result.toArray(new FileStatus[result.size()]);
	}

	@Override
	public boolean mkdirs(Path arg0, FsPermission arg1) throws IOException {
		System.out.println("Make dirs!");
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FSDataInputStream open(Path arg0, int arg1) throws IOException {
		// TODO: parse the container
		return getHadoopPithosConnector().pithosObjectInputStream("pithos",
				"server.txt");
	}

	@Override
	public boolean rename(Path arg0, Path arg1) throws IOException {
		System.out.println("rename!");
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Stub so we can create a 'runnable jar' export for packing
		// dependencies
		System.out.println("Pithos FileSystem Connector loaded.");
	}

}
