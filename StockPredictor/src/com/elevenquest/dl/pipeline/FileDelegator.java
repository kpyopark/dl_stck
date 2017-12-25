package com.elevenquest.dl.pipeline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.amazonaws.SdkClientException;

public class FileDelegator {
	
	boolean s3path = false;
	String path;
	String bucket;
	String key;
	PrintWriter pw = null;
	File tempFile = null;
	
	public FileDelegator(String path) throws IOException {
		this.path = path;
		s3path = path.indexOf("s3://") == 0;
		if(s3path) {
			Object[] bucketAndKey = FileUtil.getBucketAndKey(path);
			this.bucket = (String)bucketAndKey[0];
			this.key = (String)bucketAndKey[1];
			tempFile = File.createTempFile("tempfile", ".csv");
			tempFile.deleteOnExit();
		} else {
			File parentFolder = new File(this.path).getParentFile(); 
			if(!parentFolder.exists()) 
				parentFolder.mkdirs();
			tempFile = new File(path);
		}
	}
	
	private void initPrintWriter() throws IOException {
		pw = new PrintWriter(new FileOutputStream(tempFile));
	}
	
	public File getTempFile() throws IOException {
		if(s3path) {
			FileUtil.copyS3ToFile(this.path, tempFile.getAbsolutePath());
			return this.tempFile;
		} else {
			return tempFile;
		}
	}
	
	public long getLength() {
		if(s3path) {
			return FileUtil.getS3FileSize(this.path);
		} else {
			return tempFile.length();
		}
	}
	
	public void println(String content) throws IOException {
		if(pw == null)
			initPrintWriter();
		pw.println(content);
	}
	
	public void flush() {
		if(s3path) {
			FileUtil.copyFileToS3(this.tempFile.getAbsolutePath(), this.path);
		}
	}
	
	public void close() {
		if(pw != null)
			pw.close();
		if(s3path) {
			if(pw != null) {
				FileUtil.copyFileToS3(this.tempFile.getAbsolutePath(), this.path);
			}
			tempFile.deleteOnExit();
			tempFile.delete();
		} else {
			//
		}
	}
	
}
