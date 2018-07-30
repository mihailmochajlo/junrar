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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.github.junrar.exception.RarException;
import com.github.junrar.exception.RarException.RarExceptionType;
import com.github.junrar.io.InputStreamReader;
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
import java.util.logging.Level;


/**
 * The Main Rar Class; represents a rar Archive
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class Archive{
	private static Logger logger = Logger.getLogger(Archive.class.getName());
        
        private InputStreamReader rois;

	private MarkHeader markHead = null;

	private MainHeader newMhd = null;

	private Unpack unpack;

        public List<String> readFileHeaders(InputStream is) throws IOException, RarException
        {
            rois = new InputStreamReader(is);
            List<String> fileNames = new ArrayList<String>();
            int toRead = 0;

            while (true) {
                    int size = 0;
                    long newpos = 0;
                    byte[] baseBlockBuffer = new byte[BaseBlock.BaseBlockSize];

                    long position = rois.getPosition();

                    logger.log(Level.INFO, "<---reading header--->");
                    size = rois.read(baseBlockBuffer, 0, BaseBlock.BaseBlockSize);
                    if (size == 0) {
                            break;
                    }
                    BaseBlock block = new BaseBlock(baseBlockBuffer);

                    block.setPositionInFile(position);

                    switch (block.getHeaderType())
                    {
                        case MarkHeader:
                            logger.log(Level.INFO, "<---MARK HEADER detected--->");
                            markHead = new MarkHeader(block);
                            if (!markHead.isSignature()) {
                                throw new RarException(RarException.RarExceptionType.badRarArchive);
                            }
                            break;
                        case MainHeader:
                            logger.log(Level.INFO, "<---MAIN HEADER detected--->");
                            toRead = block.hasEncryptVersion() ? MainHeader.mainHeaderSizeWithEnc
                                            : MainHeader.mainHeaderSize;
                            byte[] mainbuff = new byte[toRead];
                            rois.read(mainbuff, 0, toRead);
                            MainHeader mainhead = new MainHeader(block, mainbuff);
                            this.newMhd = mainhead;
                            if (newMhd.isEncrypted()) {
                                throw new RarException(RarExceptionType.rarEncryptedException);
                            }
                            break;
                        case SignHeader:
                            logger.log(Level.INFO, "<---SIGN HEADER detected--->");
                            toRead = SignHeader.signHeaderSize;
                            rois.skip(toRead);
                            break;

                        case AvHeader:
                            logger.log(Level.INFO, "<---AV HEADER detected--->");
                            toRead = AVHeader.avHeaderSize;
                            rois.skip(toRead);
                            break;

                        case CommHeader:
                            logger.log(Level.INFO, "<---COMM HEADER detected--->");
                            toRead = CommentHeader.commentHeaderSize;
                            byte[] commBuff = new byte[toRead];
                            rois.read(commBuff, 0, toRead);
                            CommentHeader commHead = new CommentHeader(block, commBuff);
                            newpos = commHead.getPositionInFile()
                                            + commHead.getHeaderSize();
                            rois.setPosition(newpos);
                            break;
                        case EndArcHeader:
                            logger.log(Level.INFO, "<---END ARCH HEADER detected--->");
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
                            rois.read(blockHeaderBuffer, 0, BlockHeader.blockHeaderSize);
                            BlockHeader blockHead = new BlockHeader(block,
                                            blockHeaderBuffer);
                            FileHeader fh = null;
                            switch (blockHead.getHeaderType())
                            {
                                case NewSubHeader:
                                    logger.log(Level.INFO, "<---NEW SUB HEADER detected--->");
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] subHeaderBuffer = new byte[toRead];
                                    rois.read(subHeaderBuffer, 0, toRead);
                                    fh = new FileHeader(blockHead, subHeaderBuffer);
                                    newpos = fh.getPositionInFile() + fh.getHeaderSize()
                                                    + fh.getFullPackSize();
                                    rois.setPosition(newpos);
                                    break;
                                case FileHeader:
                                    logger.log(Level.INFO, "<---FILE HEADER detected--->");
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] fileHeaderBuffer = new byte[toRead];
                                    rois.read(fileHeaderBuffer, 0, toRead);
                                    fh = new FileHeader(blockHead, fileHeaderBuffer);
                                    fileNames.add(fh.getFileNameW());
                                    newpos = fh.getPositionInFile() + fh.getHeaderSize()
                                                    + fh.getFullPackSize();
                                    rois.setPosition(newpos);
                                    break;
                                case ProtectHeader:
                                {
                                    logger.log(Level.INFO, "<---PROTECT HEADER detected--->");
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] protectHeaderBuffer = new byte[toRead];
                                    rois.read(protectHeaderBuffer, 0, toRead);
                                    ProtectHeader ph = new ProtectHeader(blockHead,
                                                    protectHeaderBuffer);

                                    newpos = ph.getPositionInFile() + ph.getHeaderSize()
                                                    + ph.getDataSize();
                                    rois.setPosition(newpos);
                                    break;
                                }
                                case SubHeader: 
                                {
                                    logger.log(Level.INFO, "<---SUB HEADER detected--->");
                                    byte[] subHeadbuffer = new byte[SubBlockHeader.SubBlockHeaderSize];
                                    rois.read(subHeadbuffer,
                                                    0, SubBlockHeader.SubBlockHeaderSize);
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
                                    logger.log(Level.WARNING, "<---UNKNOWN HEADER--->");
                                    throw new RarException(RarExceptionType.notRarArchive);
                                }
                            }
                        }
                    }
                    logger.log(Level.INFO, "<--- end reading --->");
            }
            return fileNames;
	}
        
        public boolean extractFile(InputStream is, String fileName, OutputStream os) throws IOException, RarException
        {
            rois = new InputStreamReader(is);
            int toRead = 0;
            boolean result = false;

            while (result == false) {
                    int size = 0;
                    long newpos = 0;
                    byte[] baseBlockBuffer = new byte[BaseBlock.BaseBlockSize];

                    long position = rois.getPosition();

                    logger.log(Level.INFO, "<---reading header--->");
                    size = rois.read(baseBlockBuffer, 0, BaseBlock.BaseBlockSize);
                    if (size == 0) {
                            break;
                    }
                    BaseBlock block = new BaseBlock(baseBlockBuffer);

                    block.setPositionInFile(position);

                    switch (block.getHeaderType())
                    {
                        case MarkHeader:
                            logger.log(Level.INFO, "<---MARK HEADER detected--->");
                            markHead = new MarkHeader(block);
                            if (!markHead.isSignature()) {
                                throw new RarException(RarException.RarExceptionType.badRarArchive);
                            }
                            break;
                        case MainHeader:
                            logger.log(Level.INFO, "<---MAIN HEADER detected--->");
                            toRead = block.hasEncryptVersion() ? MainHeader.mainHeaderSizeWithEnc
                                            : MainHeader.mainHeaderSize;
                            byte[] mainbuff = new byte[toRead];
                            rois.read(mainbuff, 0, toRead);
                            MainHeader mainhead = new MainHeader(block, mainbuff);
                            this.newMhd = mainhead;
                            if (newMhd.isEncrypted()) {
                                throw new RarException(RarExceptionType.rarEncryptedException);
                            }
                            break;
                        case SignHeader:
                            logger.log(Level.INFO, "<---SIGN HEADER detected--->");
                            toRead = SignHeader.signHeaderSize;
                            rois.skip(toRead);
                            break;

                        case AvHeader:
                            logger.log(Level.INFO, "<---AV HEADER detected--->");
                            toRead = AVHeader.avHeaderSize;
                            rois.skip(toRead);
                            break;

                        case CommHeader:
                            logger.log(Level.INFO, "<---COMM HEADER detected--->");
                            toRead = CommentHeader.commentHeaderSize;
                            byte[] commBuff = new byte[toRead];
                            rois.read(commBuff, 0, toRead);
                            CommentHeader commHead = new CommentHeader(block, commBuff);
                            newpos = commHead.getPositionInFile()
                                            + commHead.getHeaderSize();
                            rois.setPosition(newpos);
                            break;
                        case EndArcHeader:
                            logger.log(Level.INFO, "<---END ARC HEADER detected--->");
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
                            rois.read(blockHeaderBuffer, 0, BlockHeader.blockHeaderSize);
                            BlockHeader blockHead = new BlockHeader(block,
                                            blockHeaderBuffer);
                            FileHeader fh = null;
                            switch (blockHead.getHeaderType())
                            {
                                case NewSubHeader:
                                    logger.log(Level.INFO, "<---NEW SUB HEADER detected--->");
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] subHeaderBuffer = new byte[toRead];
                                    rois.read(subHeaderBuffer, 0, toRead);
                                    fh = new FileHeader(blockHead, subHeaderBuffer);
                                    newpos = fh.getPositionInFile() + fh.getHeaderSize()
                                                    + fh.getFullPackSize();
                                    rois.setPosition(newpos);
                                    break;
                                case FileHeader:
                                    logger.log(Level.INFO, "<---FILE HEADER detected--->");
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] fileHeaderBuffer = new byte[toRead];
                                    rois.read(fileHeaderBuffer, 0, toRead);
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
                                    logger.log(Level.INFO, "<---PROTECT HEADER detected--->");
                                    toRead = blockHead.getHeaderSize()
                                                    - BlockHeader.BaseBlockSize
                                                    - BlockHeader.blockHeaderSize;
                                    byte[] protectHeaderBuffer = new byte[toRead];
                                    rois.read(protectHeaderBuffer, 0, toRead);
                                    ProtectHeader ph = new ProtectHeader(blockHead,
                                                    protectHeaderBuffer);

                                    newpos = ph.getPositionInFile() + ph.getHeaderSize()
                                                    + ph.getDataSize();
                                    rois.setPosition(newpos);
                                    break;
                                }
                                case SubHeader: 
                                {
                                    logger.log(Level.INFO, "<---SUB HEADER detected--->");
                                    byte[] subHeadbuffer = new byte[SubBlockHeader.SubBlockHeaderSize];
                                    rois.read(subHeadbuffer,
                                                    0, SubBlockHeader.SubBlockHeaderSize);
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
                                    logger.log(Level.WARNING, "<---UNKNOWN HEADER--->");
                                    throw new RarException(RarExceptionType.notRarArchive);
                                }
                            }
                        }
                    }
                    logger.log(Level.INFO, "<--- end reading --->");
            }
            
            return result;
	}

	private void doExtractFile(FileHeader hd, OutputStream os)
			throws RarException, IOException {
		ComprDataIO dataIO = new ComprDataIO(hd, rois, os, markHead.isOldFormat());
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
			long actualCRC = ~dataIO.getUnpFileCRC();
			int expectedCRC = hd.getFileCRC();
			if (actualCRC != expectedCRC) {
				throw new RarException(RarExceptionType.crcError);
			}
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

        public InputStreamReader getRois()
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
}