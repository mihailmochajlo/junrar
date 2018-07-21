/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.junrar;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

/**
 *
 * @author UltraNB
 */
public class Unrar
{
    static String archiveName;
    static String fileName;
    
    static Archive archive;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        FileInputStream fis;
        FileOutputStream fos;
        
        try
        {
            switch (args.length) {
                case 0:
                    System.out.println("args: <archive> [file-to-extract]");
                    break;
                case 1:
                    archiveName = args[0];
                    archive = new Archive();
                    fis = new FileInputStream(archiveName);
                    List<String> headers = archive.readFileHeaders(fis);
                    for (String header: headers)
                    {
                        System.out.println("\t" + header);
                    }
                    fis.close();
                    break;
                default:
                    archiveName = args[0];
                    fileName = args[1];
                    archive = new Archive();
                    fis = new FileInputStream(archiveName);
                    fos = new FileOutputStream(fileName);
                    archive.extractFile(fis, fileName, fos);
                    fos.flush();
                    fos.close();
                    fis.close();
                    break;
            }
        }
        catch (Exception ex)
        {
            System.out.println(ex.toString());
        }
    }
    
}
