/*
 * public domain as of http://rsbweb.nih.gov/ij/disclaimer.html
 */
package com.github.junrar.io;

import java.io.*;

public final class RandomAccessStream extends InputStream
{
	private InputStream src;
	private long pointer;
	private int length;
	private boolean foundEOS;

	public RandomAccessStream(InputStream inputstream) {
            pointer = 0L;
            length = 0;
            foundEOS = false;
            src = inputstream;
	}

	public long getLongFilePointer() throws IOException
        {
            return pointer;
	}

	public int read() throws IOException
        {
            return src.read();
	}

	public int read(byte[] bytes, int off, int len) throws IOException
        {
            if (bytes == null)
                    throw new NullPointerException();
            if (off < 0 || len < 0 || off + len > bytes.length)
                    throw new IndexOutOfBoundsException();
            if (len == 0)
                    return 0;
            int count = src.read(bytes, off, len);
            pointer += count;

            return count;
	}

	public final void readFully(byte[] bytes) throws IOException
        {
            readFully(bytes, bytes.length);
	}

	public final void readFully(byte[] bytes, int len) throws IOException
        {
            pointer += src.read(bytes, 0, len);
	}

	public void seek(long loc) throws IOException
        {
            if (loc < 0L)
                pointer = 0L;
            else
            {   
                long div = loc - pointer;
                long skipped = 0;

                while (div != 0)
                {
                    skipped = src.skip(div);
                    div = div - skipped;
                }
                pointer = loc;
            }
	}
        
        public void guaranteedSkip(long num) throws IOException {
            long div = num;
            long skipped = 0;
                    
            while (div != 0)
            {
                skipped = src.skip(div);
                div = div - skipped;
            }
            pointer += num;
        }
}