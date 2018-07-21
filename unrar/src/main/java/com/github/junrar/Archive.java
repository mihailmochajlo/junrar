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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.github.junrar.exception.RarException;
import com.github.junrar.exception.RarException.RarExceptionType;
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
        
        private InputStreamReadOnlyAccessFile rois;

	private final ComprDataIO dataIO;

	private MarkHeader markHead = null;

	private MainHeader newMhd = null;

	private Unpack unpack;

	public Archive() throws RarException, IOException
        {
            dataIO = new ComprDataIO(this);
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
                                    rois.setPosition(newpos);
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
                                    rois.setPosition(newpos);
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

        public IReadOnlyAccess getRois()
        {
            return rois;
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
		if (rois != null) {
			rois.close();
			rois = null;
		}
		if (unpack != null) {
			unpack.cleanUp();
		}
	}
}