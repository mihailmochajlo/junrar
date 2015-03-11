package com.github.junrar.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.github.junrar.Archive;
import com.github.junrar.Volume;
import com.github.junrar.io.IReadOnlyAccess;
import com.github.junrar.io.ReadOnlyAccessByteArray;


/**
 * @author Daniel Rabe</a>
 * 
 */
public class InputStreamVolume implements Volume {
	private final Archive archive;
	private final byte[] file;

	/**
	 * @param file
	 * @throws IOException 
	 */
	public InputStreamVolume(Archive archive, InputStream inputstream) throws IOException {
		this.archive = archive;
		byte[] buff = new byte[8000];
		int bytesRead = 0;
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		while((bytesRead = inputstream.read(buff)) != -1) {
			bao.write(buff, 0, bytesRead);
		}
		byte[] data = bao.toByteArray();
		this.file = data;
	}

	@Override
	public IReadOnlyAccess getReadOnlyAccess() throws IOException {
        return new ReadOnlyAccessByteArray(file);
	}

	@Override
	public long getLength() {
		return file.length;
	}

	@Override
	public Archive getArchive() {
		return archive;
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		// there is no File object anymore
		return null;
	}
}
