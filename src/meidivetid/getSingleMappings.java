/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meidivetid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import utils.SortByChr;

/**
 *
 * @author Derek.Bickhart
 */
public class getSingleMappings {
    
    public static void getTransChrIDs(Path file, Path output){
        Set<String> unique = new HashSet<>();
        try(BufferedReader input = Files.newBufferedReader(file, Charset.defaultCharset())){
            String line;
            String lastread = "NA"; 
            int readcount = 0;
            while((line = input.readLine()) != null){
                line = line.trim();
                String[] segs = line.split("\t");
                
                if(!segs[9].equals("transchr"))
                    continue;
                
                if(lastread.equals("NA")){
                    lastread = segs[0];
                    readcount = 1;
                }else if(!lastread.equals(segs[0])){
                    if(readcount == 1){
                        unique.add(segs[0]);
                    }
                    readcount = 1;
                    lastread = segs[0];
                }else{
                    readcount++;
                }
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        
        // Reopen the file and create a sorted bedpe
        // Always sorting lesser chr first
        try(BufferedReader input = Files.newBufferedReader(file, Charset.defaultCharset())){
            BufferedWriter out = Files.newBufferedWriter(output, Charset.defaultCharset());
            String line;
            while((line = input.readLine())!= null){
                line = line.trim();
                String[] segs = line.split("\t");
                
                if(!unique.contains(segs[0]))
                    continue;
                
                boolean first = (SortByChr.GetChrOrder(segs[1], segs[5]) < 0)? true: false;
                String chr1 = (first)? segs[1] : segs[5];
                int start1 = (first)? Integer.parseInt(segs[2]) : Integer.parseInt(segs[6]);
                int end1 = (first)? Integer.parseInt(segs[3]) : Integer.parseInt(segs[7]);
                
                String chr2 = (first)? segs[5] : segs[1];
                int start2 = (first)? Integer.parseInt(segs[6]) : Integer.parseInt(segs[2]);
                int end2 = (first)? Integer.parseInt(segs[7]) : Integer.parseInt(segs[3]);
                
                StringBuilder str = new StringBuilder();
                str.append(chr1).append("\t").append(start1).append("\t").append(end1).append("\t");
                str.append(chr2).append("\t").append(start2).append("\t").append(end2).append(System.lineSeparator());
                
                out.write(str.toString());
            }
            out.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
        
        System.err.println("Finished identifying events and sorting by chr");
    }
}
