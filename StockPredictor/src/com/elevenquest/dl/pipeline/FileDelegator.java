package com.elevenquest.dl.pipeline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class FileDelegator {
	
	boolean s3path = false;
	String path;
	String bucket;
	String key;
	PrintWriter pw;
	File tempFile = null;
	
	public FileDelegator(String path) throws IOException {
		this.path = path;
		s3path = path.indexOf("s3://") == 0;
		if(s3path) {
			Object[] bucketAndKey = FileUtil.getBucketAndKey(path);
			this.bucket = (String)bucketAndKey[0];
			this.key = (String)bucketAndKey[1];
			tempFile = File.createTempFile("tempfile", ".csv");
			pw = new PrintWriter(new FileOutputStream(tempFile));
		} else {
			File parentFolder = new File(this.path).getParentFile(); 
			if(!parentFolder.exists()) {
				parentFolder.mkdirs();
			}
			pw = new PrintWriter(new FileOutputStream(path));
		}
	}
	
	public void println(String content) {
		pw.println(content);
	}
	
	public void close() {
		if(s3path) {
			pw.close();
			FileUtil.copyFileToS3(this.tempFile.getAbsolutePath(), this.path);
			tempFile.deleteOnExit();
			tempFile.delete();
		} else {
			pw.close();
		}
	}
	
}
