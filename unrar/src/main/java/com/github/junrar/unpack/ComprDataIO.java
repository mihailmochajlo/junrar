/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 31.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 * 
 * the unrar licence applies to all junrar source and binary distributions 
 * you are not allowed to use this source to re-create the RAR compression algorithm
 * 
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;" 
 */
package com.github.junrar.unpack;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.junrar.Archive;
import com.github.junrar.crc.RarCRC;
import com.github.junrar.exception.RarException;
import com.github.junrar.io.InputStreamReadOnlyAccessFile;
import com.github.junrar.rarfile.FileHeader;


/**
 * DOCUMENT ME
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class ComprDataIO {

	private final Archive archive;

	private long unpPackedSize;

	private InputStreamReadOnlyAccessFile inputStream;

	private OutputStream outputStream;

	private FileHeader subHead;

	private long unpFileCRC;

	public ComprDataIO(Archive arc) {
		this.archive = arc;
	}

	public void init(OutputStream outputStream) {
		this.outputStream = outputStream;
		unpPackedSize = 0;
		unpFileCRC = 0xffffffff;
		subHead = null;
	}

	public void init(FileHeader hd) throws IOException {
		long startPos = hd.getPositionInFile() + hd.getHeaderSize();
		unpPackedSize = hd.getFullPackSize();
		inputStream = archive.getRois();
		subHead = hd;
	}

	public int unpRead(byte[] addr, int offset, int count) throws IOException,
			RarException {
		int retCode = 0, totalRead = 0;
		while (count > 0) {
			int readSize = (count > unpPackedSize) ? (int) unpPackedSize
					: count;
			retCode = inputStream.read(addr, offset, readSize);
			if (retCode < 0) {
				throw new EOFException();
			}
                        
			totalRead += retCode;
			offset += retCode;
			count -= retCode;
			unpPackedSize -= retCode;
			if (unpPackedSize != 0 || !subHead.isSplitAfter())
                        {
                            break;
			}
		}

		if (retCode != -1) {
			retCode = totalRead;
		}
		return retCode;

	}

	public void unpWrite(byte[] addr, int offset, int count) throws IOException {
            outputStream.write(addr, offset, count);
            if (archive.isOldFormat()) {
                    unpFileCRC = RarCRC
                                    .checkOldCrc((short) unpFileCRC, addr, count);
            } else {
                    unpFileCRC = RarCRC.checkCrc((int) unpFileCRC, addr, offset,
                                    count);
            }
	}

	public long getUnpFileCRC() {
		return unpFileCRC;
	}

	public void setUnpFileCRC(long unpFileCRC) {
		this.unpFileCRC = unpFileCRC;
	}

	public FileHeader getSubHeader() {
		return subHead;
	}
}