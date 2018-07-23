/*
 * public domain as of http://rsbweb.nih.gov/ij/disclaimer.html
 */
package com.github.junrar.io;

import java.io.*;

public final class InputStreamReader
{
	private final InputStream src;
	private long pointer;

	public InputStreamReader(InputStream inputstream) {
            pointer = 0L;
            src = inputstream;
	}
        
        public long getPosition() throws IOException {
            return pointer;
	}
        
        public void setPosition(long pos) throws IOException {
            if (pos < 0L)
                pointer = 0L;
            else
            {   
                long div = pos - pointer;
                long skipped = 0;

                while (div != 0)
                {
                    skipped = src.skip(div);
                    div = div - skipped;
                }
                pointer = pos;
            }
	}
        
        public void skip(long count) throws IOException {
            long div = count;
            long skipped = 0;
                    
            while (div != 0)
            {
                skipped = src.skip(div);
                div = div - skipped;
            }
            pointer += count;
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
}