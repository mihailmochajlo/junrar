/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.junrar;

import com.github.junrar.rarfile.FileHeader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
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

    private static final String testFolderName = "test";
    private static final String archName = "test.rar";
    private static final String fileName = "cat.jpg";
    
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
            archiveFile = new File(testFolderName + "\\" + archName);
            fis = new FileInputStream(archiveFile);
            instance = new Archive(fis);
            List<String> result = instance.readFileHeaders(fis);
            fis.close();
            
            assertEquals(expResult.size(), result.size());
            
            for (int i = 0; i < expResult.size(); i++)
            {
                assert(expResult.contains(result.get(i)));
            }
            
            System.out.println("\tSUCCESS");
        }
        catch(Exception ex)
        {
            System.out.println("\tERROR: " + ex.getMessage());
        }
    }
    
    public void testExtractFile()
    {
        System.out.println("<--- TEST ---> extractFile <--- TEST--->");
        
        FileInputStream fis;
        ByteArrayOutputStream baos;
        Archive instance = null;
        byte[] expResult;
        byte[] result;
        int diffCount = 0;
        
        try
        {
            File expFile = new File(testFolderName + "\\" + fileName);
            expResult = new byte[(int)expFile.length()];
            FileInputStream expFileStream = new FileInputStream(expFile);
            expFileStream.read(expResult);
            expFileStream.close();
            
            fis = new FileInputStream(testFolderName + "\\" + archName);
            instance = new Archive(fis);
            baos = new ByteArrayOutputStream();
            instance.extractFile(fis, fileName, baos);
           
            result = baos.toByteArray();
            
            fis.close();
            baos.close();
            
            assertEquals(expResult.length, result.length);
            
            for (int i = 0; i < expResult.length; i++)
            {
                if (expResult[i] != result[i])
                {
                    diffCount++;
                }
            }
            
            assertEquals(diffCount, 0);
            
            System.out.println("\tSUCCESS");
        }
        catch (Exception ex)
        {
            System.out.println("\tERROR: " + ex.getMessage());
        }
    }
}