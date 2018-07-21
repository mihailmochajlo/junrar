/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.junrar;

import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import static junit.framework.Assert.assertEquals;
import junit.framework.TestCase;

/**
 *
 * @author UltraNB
 */
public class ArchiveTest extends TestCase {

    private static final String testFolderName = "test";
    private static final String archName = "test.rar";
    private static final String[] fileNames = {"AS_ACTSTAT_20180701_7668aa98-b5d9-4ac3-8e71-bf9a53e76ec4.XML",
                                               "AS_CENTERST_20180701_1e522bf4-e101-48ee-95d9-60f902155411.XML",
                                               "AS_CURENTST_20180701_541a4017-5dc9-4f41-9489-791b0d353eba.XML",
                                               "AS_DEL_ADDROBJ_20180701_44384a73-4c22-40e9-a688-1cd759015f0d.XML",
                                               "AS_DEL_HOUSE_20180701_aba964b8-80c3-4528-864b-c0d066f0e8b6.XML",
                                               "AS_DEL_NORMDOC_20180701_a565e47b-c126-4a77-a415-f8553ac2c632.XML",
                                               "AS_ESTSTAT_20180701_f44cdf8a-93a1-4aac-a698-c264b6293cf6.XML",
                                               "AS_FLATTYPE_20180701_66ef9fff-6a4a-4c23-9c88-62796f14dca0.XML",
                                               "AS_HSTSTAT_20180701_01b584bc-fb90-4846-9a47-ee45cf90221c.XML",
                                               "AS_INTVSTAT_20180701_f5c610ec-758d-4019-881d-0aa6bd53f790.XML",
                                               "AS_NDOCTYPE_20180701_0f38bd80-b075-447e-abd5-733e0ca01ba6.XML",
                                               "AS_OPERSTAT_20180701_09cb5c97-7f1a-4885-bbb0-6a99cd8f8701.XML",
                                               "AS_ROOMTYPE_20180701_8b8869fb-1cfb-4590-9dc9-3ec0b3f40ea5.XML",
                                               "AS_SOCRBASE_20180701_73c2a7ab-59c4-4b3e-bc42-06b08ae24841.XML",
                                               "AS_STRSTAT_20180701_c49f3469-7efb-46b6-9abd-e2b97c5ff43e.XML"};
    
    public ArchiveTest(String testName) {
        super(testName);
        Arrays.sort(fileNames);
    }
    
    /**
     * Test of getFileHeaders method, of class Archive.
     */
    public void testGetFileHeaders() {
        System.out.println("<--- TEST ---> getFileHeaders <--- TEST--->");
        
        File archiveFile = null;
        FileInputStream fis;
        Archive instance = null;
        
        try
        {
            archiveFile = new File(testFolderName + "\\" + archName);
            fis = new FileInputStream(archiveFile);
            instance = new Archive();
            List<String> result = instance.readFileHeaders(fis);
            Collections.sort(result);
            fis.close();
            
            assertEquals(fileNames.length, result.size());
            
            for (int i = 0; i < fileNames.length; i++)
            {
                assertEquals(fileNames[i], result.get(i));
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
        
        try
        {
            for (int i = 0; i < fileNames.length; i++)
            {
                File expFile = new File(testFolderName + "\\" + fileNames[i]);
                expResult = new byte[(int)expFile.length()];
                FileInputStream expFileStream = new FileInputStream(expFile);
                expFileStream.read(expResult);
                expFileStream.close();

                fis = new FileInputStream(testFolderName + "\\" + archName);
                instance = new Archive();
                baos = new ByteArrayOutputStream();
                instance.extractFile(fis, fileNames[i], baos);

                result = baos.toByteArray();

                fis.close();
                baos.close();

                assertEquals(expResult.length, result.length);

                for (int j = 0; j < expResult.length; j++)
                {
                    assertEquals(expResult[j], result[j]);
                }
            }
            
            System.out.println("\tSUCCESS");
        }
        catch (Exception ex)
        {
            System.out.println("\tERROR: " + ex.getMessage());
        }
    }
}