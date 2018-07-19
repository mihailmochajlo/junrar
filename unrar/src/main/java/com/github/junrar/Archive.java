/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 22.05.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 *
 * the unrar licence applies to all junrar source and binary distributions
 * you are not allowed to use this source to re-create the RAR compression
 * algorithm
 *
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;"
 */
package com.github.junrar;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.junrar.exception.RarException;
import com.github.junrar.exception.RarException.RarExceptionType;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.impl.InputStreamVolumeManager;
import com.github.junrar.io.IReadOnlyAccess;
import com.github.junrar.rarfile.AVHeader;
import com.github.junrar.rarfile.BaseBlock;
import com.github.junrar.rarfile.BlockHeader;
import com.github.junrar.rarfile.CommentHeader;
import com.github.junrar.rarfile.EAHeader;
import com.github.junrar.rarfile.EndArcHeader;
import com.github.junrar.rarfile.FileHeader;
import com.github.junrar.rarfile.MacInfoHeader;
import com.github.junrar.rarfile.MainHeader;
import com.github.junrar.rarfile.MarkHeader;
import com.github.junrar.rarfile.ProtectHeader;
import com.github.junrar.rarfile.SignHeader;
import com.github.junrar.rarfile.SubBlockHeader;
import com.github.junrar.rarfile.UnixOwnersHeader;
import com.github.junrar.rarfile.UnrarHeadertype;
import com.github.junrar.unpack.ComprDataIO;
import com.github.junrar.unpack.Unpack;
import com.github.junrar.io.InputStreamReadOnlyAccessFile;


/**
 * The Main Rar Class; represents a rar Archive
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class Archive implements Closeable {
	private static Logger logger = Logger.getLogger(Archive.class.getName());

	private IReadOnlyAccess rof;
        
        private InputStreamReadOnlyAccessFile rois;

	private final UnrarCallback unrarCallback = null;

	private final ComprDataIO dataIO;

	private final List<BaseBlock> headers = new ArrayList<BaseBlock>();

	private MarkHeader markHead = null;

	private MainHeader newMhd = null;

	private Unpack unpack;

	private int currentHeaderIndex;

	/** Size of packed data in current file. */
	private long totalPackedSize = 0L;

	/** Number of bytes of compressed data read from current file. */
	private long totalPackedRead = 0L;

	private VolumeManager volumeManager;
	private Volume volume;

	public Archive(VolumeManager volumeManager) throws RarException,
			IOException {
		this(volumeManager, null);
	}

	/**
	 * create a new archive object using the given {@link VolumeManager}
	 * 
	 * @param volumeManager
	 *            the the {@link VolumeManager} that will provide volume stream
	 *            data
	 * @throws RarException
	 */
	public Archive(VolumeManager volumeManager, UnrarCallback unrarCallback)
			throws RarException, IOException {
		this.volumeManager = volumeManager;
		//this.unrarCallback = unrarCallback;

		//setVolume(this.volumeManager.nextArchive(this, null));
		dataIO = new ComprDataIO(this);
	}

	public Archive(File firstVolume) throws RarException, IOException {
		this(new FileVolumeManager(firstVolume), null);
	}

	public Archive(File firstVolume, UnrarCallback unrarCallback)
			throws RarException, IOException {
		this(new FileVolumeManager(firstVolume), unrarCallback);
	}

	public Archive(InputStream firstVolume) throws RarException, IOException {
            rois = new InputStreamReadOnlyAccessFile(firstVolume);
            dataIO = new ComprDataIO(this);
		//this(new InputStreamVolumeManager(firstVolume), null);
	}

	public Archive(InputStream firstVolume, UnrarCallback unrarCallback)
			throws RarException, IOException {
		this(new InputStreamVolumeManager(firstVolume), unrarCallback);
	}

	public void bytesReadRead(int count) {
		if (count > 0) {
			totalPackedRead += count;
			if (unrarCallback != null) {
				unrarCallback.volumeProgressChanged(totalPackedRead,
						totalPackedSize);
			}
		}
	}

	public IReadOnlyAccess getRois() {
		return rois;
	}

	/**
	 * @return returns all file headers of the archive
	 */
	public List<FileHeader> getFileHeaders() {
		List<FileHeader> list = new ArrayList<FileHeader>();
		for (BaseBlock block : headers) {
			if (block.getHeaderType().equals(UnrarHeadertype.FileHeader)) {
				list.add((FileHeader) block);
			}
		}
		return list;
	}
        
        public FileHeader getFileHeaderFromName(String name)
        {
            FileHeader result = null;
            List<FileHeader> file_headers = getFileHeaders();
            for (int i = 0; i < file_headers.size() && result == null; i = i + 1)
            {
                if (file_headers.get(i).getFileNameW().equals(name))
                {
                    result = file_headers.get(i);
                }
            }
            return result;
        }

	public FileHeader nextFileHeader() {
		int n = headers.size();
		while (currentHeaderIndex < n) {
			BaseBlock block = headers.get(currentHeaderIndex++);
			if (block.getHeaderType() == UnrarHeadertype.FileHeader) {
				return (FileHeader) block;
			}
		}
		return null;
	}

	public UnrarCallback getUnrarCallback() {
		return unrarCallback;
	}

	/**
	 * 
	 * @return whether the archive is encrypted
	 */
	public boolean isEncrypted() {
		if (newMhd != null) {
			return newMhd.isEncrypted();
		} else {
			throw new NullPointerException("mainheader is null");
		}
	}

        public List<String> readFileHeaders(InputStream is) throws IOException, RarException
        {
            rois = new InputStreamReadOnlyAccessFile(is);
            List<String> fileNames = new ArrayList<String>();
            int toRead = 0;

            while (true) {
                    int size = 0;
                    long newpos = 0;
                    byte[] baseBlockBuffer = new byte[BaseBlock.BaseBlockSize];

                    long position = rois.getPosition();

                    // logger.info("\n--------reading header--------");
                    size = rois.readFully(baseBlockBuffer, BaseBlock.BaseBlockSize);
                    if (size == 0) {
                            break;
                    }
                    BaseBlock block = new BaseBlock(baseBlockBuffer);

                    block.setPositionInFile(position);

                    switch (block.getHeaderType())
                    {
                        case MarkHeader:
                            markHead = new MarkHeader(block);
                            if (!markHead.isSignature()) {
                                throw new RarException(RarException.RarExceptionType.badRarArchive);
                            }
                            break;
                        case MainHeader:
                            toRead = block.hasEncryptVersion() ? MainHeader.mainHeaderSizeWithEnc
                                            : MainHeader.mainHeaderSize;
                            byte[] mainbuff = new byte[toRead];
                            rois.readFully(mainbuff, toRead);
                            MainHeader mainhead = new MainHeader(block, mainbuff);
                            this.newMhd = mainhead;
                            if (newMhd.isEncrypted()) {
                                throw new RarException(RarExceptionType.rarEncryptedException);
                            }
                            break;
                        case SignHeader:
                            toRead = SignHeader.signHeaderSize;
                            rois.skip(toRead);
                            break;

                        case AvHeader:
                            toRead = AVHeader.avHeaderSize;
                            rois.skip(toRead);
                            break;

                        case CommHeader:
                            toRead = CommentHeader.commentHeaderSize;
                            byte[] commBuff = new byte[toRead];
                            rois.readFully(commBuff, toRead);
                            CommentHeader commHead = new CommentHeader(block, commBuff);
                            newpos = commHead.getPositionInFile()
                                            + commHead.getHeaderSize();
                            rois.setPosition(newpos);
                            break;
                        case EndArcHeader:
                            toRead = 0;
                            if (block.hasArchiveDataCRC()) {
                                    toRead += EndArcHeader.endArcArchiveDataCrcSize;
                            }
                            if (block.hasVolumeNumber()) {
                                    toRead += EndArcHeader.endArcVolumeNumberSize;
                            }
                            if (toRead > 0) {
                                    rois.skip(toRead);
                            }
                            return fileNames;
                        default:
                        {
                            byte[] blockHeaderBuffer = new byte[BlockHeader.blockHeaderSize];
                            rois.readFully(blockHeaderBuffer, BlockHeader.blockHeaderSize);
                            BlockHeader blockHead = new BlockHeader(block,
                                            blockHeaderBuffer);
                            FileHeader fh = null;
                            switch (blockHead.getHeaderType())
                            {
                                case NewSubHeader:
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] subHeaderBuffer = new byte[toRead];
                                    rois.readFully(subHeaderBuffer, toRead);
                                    fh = new FileHeader(blockHead, subHeaderBuffer);
                                    newpos = fh.getPositionInFile() + fh.getHeaderSize()
                                                    + fh.getFullPackSize();
                                    rois.setPosition(newpos);
                                    break;
                                case FileHeader:
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] fileHeaderBuffer = new byte[toRead];
                                    rois.readFully(fileHeaderBuffer, toRead);
                                    fh = new FileHeader(blockHead, fileHeaderBuffer);
                                    fileNames.add(fh.getFileNameW());
                                    newpos = fh.getPositionInFile() + fh.getHeaderSize()
                                                    + fh.getFullPackSize();
                                    rois.setPosition(newpos);
                                    break;
                                case ProtectHeader:
                                {
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] protectHeaderBuffer = new byte[toRead];
                                    rois.readFully(protectHeaderBuffer, toRead);
                                    ProtectHeader ph = new ProtectHeader(blockHead,
                                                    protectHeaderBuffer);

                                    newpos = ph.getPositionInFile() + ph.getHeaderSize()
                                                    + ph.getDataSize();
                                    rof.setPosition(newpos);
                                    break;
                                }
                                case SubHeader: 
                                {
                                    byte[] subHeadbuffer = new byte[SubBlockHeader.SubBlockHeaderSize];
                                    rois.readFully(subHeadbuffer,
                                                    SubBlockHeader.SubBlockHeaderSize);
                                    SubBlockHeader subHead = new SubBlockHeader(blockHead,
                                                    subHeadbuffer);
                                    switch (subHead.getSubType())
                                    {
                                        case MAC_HEAD:
                                        {
                                            rois.skip(MacInfoHeader.MacInfoHeaderSize);
                                            break;
                                        }
                                        case EA_HEAD:
                                        {
                                            rois.skip(EAHeader.EAHeaderSize);
                                            break;
                                        }
                                        case UO_HEAD:
                                        {
                                            toRead = subHead.getHeaderSize();
                                            toRead -= BaseBlock.BaseBlockSize;
                                            toRead -= BlockHeader.blockHeaderSize;
                                            toRead -= SubBlockHeader.SubBlockHeaderSize;
                                            rois.skip(toRead);
                                            break;
                                        }
                                        default:
                                        {
                                            break;
                                        }
                                    }
                                    break;
                                }
                                default:
                                {
                                    logger.warning("Unknown Header");
                                    throw new RarException(RarExceptionType.notRarArchive);
                                }
                            }
                        }
                    }
                    // logger.info("\n--------end header--------");
            }
            
            return fileNames;
	}
        
        public boolean extractFile(InputStream is, String fileName, OutputStream os) throws IOException, RarException
        {
            rois = new InputStreamReadOnlyAccessFile(is);
            int toRead = 0;
            boolean result = false;

            while (result == false) {
                    int size = 0;
                    long newpos = 0;
                    byte[] baseBlockBuffer = new byte[BaseBlock.BaseBlockSize];

                    long position = rois.getPosition();

                    // logger.info("\n--------reading header--------");
                    size = rois.readFully(baseBlockBuffer, BaseBlock.BaseBlockSize);
                    if (size == 0) {
                            break;
                    }
                    BaseBlock block = new BaseBlock(baseBlockBuffer);

                    block.setPositionInFile(position);

                    switch (block.getHeaderType())
                    {
                        case MarkHeader:
                            markHead = new MarkHeader(block);
                            if (!markHead.isSignature()) {
                                throw new RarException(RarException.RarExceptionType.badRarArchive);
                            }
                            break;
                        case MainHeader:
                            toRead = block.hasEncryptVersion() ? MainHeader.mainHeaderSizeWithEnc
                                            : MainHeader.mainHeaderSize;
                            byte[] mainbuff = new byte[toRead];
                            rois.readFully(mainbuff, toRead);
                            MainHeader mainhead = new MainHeader(block, mainbuff);
                            this.newMhd = mainhead;
                            if (newMhd.isEncrypted()) {
                                throw new RarException(RarExceptionType.rarEncryptedException);
                            }
                            break;
                        case SignHeader:
                            toRead = SignHeader.signHeaderSize;
                            rois.skip(toRead);
                            break;

                        case AvHeader:
                            toRead = AVHeader.avHeaderSize;
                            rois.skip(toRead);
                            break;

                        case CommHeader:
                            toRead = CommentHeader.commentHeaderSize;
                            byte[] commBuff = new byte[toRead];
                            rois.readFully(commBuff, toRead);
                            CommentHeader commHead = new CommentHeader(block, commBuff);
                            newpos = commHead.getPositionInFile()
                                            + commHead.getHeaderSize();
                            rois.setPosition(newpos);
                            break;
                        case EndArcHeader:
                            toRead = 0;
                            if (block.hasArchiveDataCRC()) {
                                    toRead += EndArcHeader.endArcArchiveDataCrcSize;
                            }
                            if (block.hasVolumeNumber()) {
                                    toRead += EndArcHeader.endArcVolumeNumberSize;
                            }
                            if (toRead > 0) {
                                    rois.skip(toRead);
                            }
                            return result;
                        default:
                        {
                            byte[] blockHeaderBuffer = new byte[BlockHeader.blockHeaderSize];
                            rois.readFully(blockHeaderBuffer, BlockHeader.blockHeaderSize);
                            BlockHeader blockHead = new BlockHeader(block,
                                            blockHeaderBuffer);
                            FileHeader fh = null;
                            switch (blockHead.getHeaderType())
                            {
                                case NewSubHeader:
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] subHeaderBuffer = new byte[toRead];
                                    rois.readFully(subHeaderBuffer, toRead);
                                    fh = new FileHeader(blockHead, subHeaderBuffer);
                                    newpos = fh.getPositionInFile() + fh.getHeaderSize()
                                                    + fh.getFullPackSize();
                                    rois.setPosition(newpos);
                                    break;
                                case FileHeader:
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] fileHeaderBuffer = new byte[toRead];
                                    rois.readFully(fileHeaderBuffer, toRead);
                                    fh = new FileHeader(blockHead, fileHeaderBuffer);
                                    if (fileName.compareTo(fh.getFileNameW()) == 0)
                                    {
                                        result = true;
                                        doExtractFile(fh, os);
                                    }
                                    newpos = fh.getPositionInFile() + fh.getHeaderSize()
                                                    + fh.getFullPackSize();
                                    rois.setPosition(newpos);
                                    break;
                                case ProtectHeader:
                                {
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] protectHeaderBuffer = new byte[toRead];
                                    rois.readFully(protectHeaderBuffer, toRead);
                                    ProtectHeader ph = new ProtectHeader(blockHead,
                                                    protectHeaderBuffer);

                                    newpos = ph.getPositionInFile() + ph.getHeaderSize()
                                                    + ph.getDataSize();
                                    rof.setPosition(newpos);
                                    break;
                                }
                                case SubHeader: 
                                {
                                    byte[] subHeadbuffer = new byte[SubBlockHeader.SubBlockHeaderSize];
                                    rois.readFully(subHeadbuffer,
                                                    SubBlockHeader.SubBlockHeaderSize);
                                    SubBlockHeader subHead = new SubBlockHeader(blockHead,
                                                    subHeadbuffer);
                                    switch (subHead.getSubType())
                                    {
                                        case MAC_HEAD:
                                        {
                                            rois.skip(MacInfoHeader.MacInfoHeaderSize);
                                            break;
                                        }
                                        case EA_HEAD:
                                        {
                                            rois.skip(EAHeader.EAHeaderSize);
                                            break;
                                        }
                                        case UO_HEAD:
                                        {
                                            toRead = subHead.getHeaderSize();
                                            toRead -= BaseBlock.BaseBlockSize;
                                            toRead -= BlockHeader.blockHeaderSize;
                                            toRead -= SubBlockHeader.SubBlockHeaderSize;
                                            rois.skip(toRead);
                                            break;
                                        }
                                        default:
                                        {
                                            break;
                                        }
                                    }
                                    break;
                                }
                                default:
                                {
                                    logger.warning("Unknown Header");
                                    throw new RarException(RarExceptionType.notRarArchive);
                                }
                            }
                        }
                    }
                    // logger.info("\n--------end header--------");
            }
            
            return result;
	}
        
	/**
	 * Extract the file specified by the given header and write it to the
	 * supplied output stream
	 * 
	 * @param header
	 *            the header to be extracted
	 * @param os
	 *            the outputstream
	 * @throws RarException
	 */
	public void extractFile(FileHeader hd, OutputStream os) throws RarException {
		if (!headers.contains(hd)) {
			throw new RarException(RarExceptionType.headerNotInArchive);
		}
		try {
			doExtractFile(hd, os);
		} catch (Exception e) {
			if (e instanceof RarException) {
				throw (RarException) e;
			} else {
				throw new RarException(e);
			}
		}
	}
        
        public void extractFile(String file_name, OutputStream os) throws RarException
        {
            FileHeader header = getFileHeaderFromName(file_name);
            if (header == null)
            {
                throw new RarException(RarExceptionType.headerNotInArchive);
            }
            extractFile(header, os);
        }

	/**
	 * Returns an {@link InputStream} that will allow to read the file and
	 * stream it. Please note that this method will create a new Thread and an a
	 * pair of Pipe streams.
	 * 
	 * @param header
	 *            the header to be extracted
	 * @throws RarException
	 * @throws IOException
	 *             if any IO error occur
	 */
	public InputStream getInputStream(final FileHeader hd) throws RarException,
			IOException {
		final PipedInputStream in = new PipedInputStream(32 * 1024);
		final PipedOutputStream out = new PipedOutputStream(in);

		// creates a new thread that will write data to the pipe. Data will be
		// available in another InputStream, connected to the OutputStream.
		new Thread(new Runnable() {
			public void run() {
				try {
					extractFile(hd, out);
				} catch (RarException e) {
				} finally {
					try {
						out.close();
					} catch (IOException e) {
					}
				}
			}
		}).start();

		return in;
	}

	private void doExtractFile(FileHeader hd, OutputStream os)
			throws RarException, IOException {
		dataIO.init(os);
		dataIO.init(hd);
		dataIO.setUnpFileCRC(this.isOldFormat() ? 0 : 0xffFFffFF);
		if (unpack == null) {
			unpack = new Unpack(dataIO);
		}
		if (!hd.isSolid()) {
			unpack.init(null);
		}
		unpack.setDestSize(hd.getFullUnpackSize());
		try {
			unpack.doUnpack(hd.getUnpVersion(), hd.isSolid());
			// Verify file CRC
			hd = dataIO.getSubHeader();
			long actualCRC = hd.isSplitAfter() ? ~dataIO.getPackedCRC()
					: ~dataIO.getUnpFileCRC();
			int expectedCRC = hd.getFileCRC();
			if (actualCRC != expectedCRC) {
				throw new RarException(RarExceptionType.crcError);
			}
			// if (!hd.isSplitAfter()) {
			// // Verify file CRC
			// if(~dataIO.getUnpFileCRC() != hd.getFileCRC()){
			// throw new RarException(RarExceptionType.crcError);
			// }
			// }
		} catch (Exception e) {
			unpack.cleanUp();
			if (e instanceof RarException) {
				// throw new RarException((RarException)e);
				throw (RarException) e;
			} else {
				throw new RarException(e);
			}
		}
	}

	/**
	 * @return returns the main header of this archive
	 */
	public MainHeader getMainHeader() {
		return newMhd;
	}

	/**
	 * @return whether the archive is old format
	 */
	public boolean isOldFormat() {
		return markHead.isOldFormat();
	}

	/** Close the underlying compressed file. */
	public void close() throws IOException {
		if (rof != null) {
			rof.close();
			rof = null;
		}
		if (unpack != null) {
			unpack.cleanUp();
		}
	}

	/**
	 * @return the volumeManager
	 */
	public VolumeManager getVolumeManager() {
		return volumeManager;
	}

	/**
	 * @param volumeManager
	 *            the volumeManager to set
	 */
	public void setVolumeManager(VolumeManager volumeManager) {
		this.volumeManager = volumeManager;
	}

	/**
	 * @return the volume
	 */
	public Volume getVolume() {
		return volume;
	}
}