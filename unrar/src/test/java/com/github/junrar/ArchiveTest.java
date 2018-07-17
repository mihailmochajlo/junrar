/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.junrar;

import com.github.junrar.rarfile.FileHeader;
import java.io.FileInputStream;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import static junit.framework.Assert.assertEquals;
import junit.framework.TestCase;

/**
 *
 * @author UltraNB
 */
public class ArchiveTest extends TestCase {

    private static final String archName = "test.rar";
    
    public ArchiveTest(String testName) {
        super(testName);
    }
    
    /**
     * Test of getFileHeaders method, of class Archive.
     */
    public void testGetFileHeaders() {
        System.out.println("<--- TEST ---> getFileHeaders <--- TEST--->");
        
        File archiveFile = null;
        FileInputStream fis;
        Archive instance = null;
        
        List<String> expResult = new ArrayList<String>();
        expResult.add("boromir.png");
        expResult.add("cat.jpg");
        expResult.add("chan.jpg");
        expResult.add("may.jpg");
        
        try
        {
            archiveFile = new File(archName);
            fis = new FileInputStream(archiveFile);
            instance = new Archive(fis);
            List<String> result = instance.readFileHeaders(fis);
            fis.close();
            
            assertEquals(expResult.size(), result.size());
            
            for (int i = 0; i < expResult.size(); i++)
            {
                assert(expResult.contains(result.get(i)));
            }
        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }    
}
