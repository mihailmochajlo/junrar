package com.github.junrar.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.github.junrar.Archive;
import com.github.junrar.Volume;
import com.github.junrar.io.IReadOnlyAccess;
import com.github.junrar.io.InputStreamReadOnlyAccessFile;


/**
 * @author Daniel Rabe</a>
 * 
 */
public class InputStreamVolume implements Volume {
	private final Archive archive;
	private final InputStream file;

	/**
	 * @param file
	 * @throws IOException 
	 */
	public InputStreamVolume(Archive archive, InputStream inputstream) throws IOException {
		this.archive = archive;
		this.file = inputstream;
	}

	@Override
	public IReadOnlyAccess getReadOnlyAccess() throws IOException {
        return new InputStreamReadOnlyAccessFile(file);
	}

	@Override
	public long getLength() {
		//return file.length;
                return Long.MAX_VALUE;
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
