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
    
    private static final String dirName = "C:\\Users\\UltraNB\\Downloads";
    private static final String archName = "fias_xml.rar";
    
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
        expResult.add("AS_ACTSTAT_20180701_7668aa98-b5d9-4ac3-8e71-bf9a53e76ec4.XML");
        expResult.add("AS_ADDROBJ_20180701_487013c5-400a-4e20-a388-da9af1e14e4b.XML");
        expResult.add("AS_CENTERST_20180701_1e522bf4-e101-48ee-95d9-60f902155411.XML");
        expResult.add("AS_CURENTST_20180701_541a4017-5dc9-4f41-9489-791b0d353eba.XML");
        expResult.add("AS_DEL_ADDROBJ_20180701_44384a73-4c22-40e9-a688-1cd759015f0d.XML");
        expResult.add("AS_DEL_HOUSE_20180701_aba964b8-80c3-4528-864b-c0d066f0e8b6.XML");
        expResult.add("AS_DEL_NORMDOC_20180701_a565e47b-c126-4a77-a415-f8553ac2c632.XML");
        expResult.add("AS_ESTSTAT_20180701_f44cdf8a-93a1-4aac-a698-c264b6293cf6.XML");
        expResult.add("AS_FLATTYPE_20180701_66ef9fff-6a4a-4c23-9c88-62796f14dca0.XML");
        expResult.add("AS_HOUSE_20180701_a5d1d9bb-e02b-44ce-b9d4-f7b341cc1b5e.XML");
        expResult.add("AS_HSTSTAT_20180701_01b584bc-fb90-4846-9a47-ee45cf90221c.XML");
        expResult.add("AS_INTVSTAT_20180701_f5c610ec-758d-4019-881d-0aa6bd53f790.XML");
        expResult.add("AS_NDOCTYPE_20180701_0f38bd80-b075-447e-abd5-733e0ca01ba6.XML");
        expResult.add("AS_NORMDOC_20180701_c44f777f-b781-49c1-865b-6818f7382289.XML");
        expResult.add("AS_OPERSTAT_20180701_09cb5c97-7f1a-4885-bbb0-6a99cd8f8701.XML");
        expResult.add("AS_ROOM_20180701_79173c3d-20da-4da9-a28b-7e1047c661d0.XML");
        expResult.add("AS_ROOMTYPE_20180701_8b8869fb-1cfb-4590-9dc9-3ec0b3f40ea5.XML");
        expResult.add("AS_SOCRBASE_20180701_73c2a7ab-59c4-4b3e-bc42-06b08ae24841.XML");
        expResult.add("AS_STEAD_20180701_01c773aa-37fb-411e-8f9c-69da50df0217.XML");
        expResult.add("AS_STRSTAT_20180701_c49f3469-7efb-46b6-9abd-e2b97c5ff43e.XML");
        
        try
        {
            archiveFile = new File(dirName + "\\" + archName);
            fis = new FileInputStream(archiveFile);
            instance = new Archive(fis);
            List<FileHeader> result = instance.getFileHeaders();
            fis.close();
            
            assertEquals(expResult.size(), result.size());
            
            for (int i = 0; i < expResult.size(); i++)
            {
                assert(expResult.contains(result.get(i).getFileNameW()));
            }
        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }    
}
