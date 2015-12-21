/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meidivetid;

import GetCmdOpt.SimpleCmdLineParser;
import file.BedMap;
import file.BedSimple;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import utils.BedClosest;
import utils.SortByChr;

/**
 *
 * @author Derek.Bickhart
 */
public class MEIDivetID {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SimpleCmdLineParser cmd = setUpCmd();
        cmd.GetAndCheckOpts(args, 
                "i:r:o:", 
                "iro", 
                "iro", 
                "input", "repeat", "outbase");
        
        BedMap<BedSimple> map = new BedMap<>();
        try(BufferedReader input = Files.newBufferedReader(Paths.get(cmd.GetValue("repeat")), Charset.defaultCharset())){
            String line;
            while((line = input.readLine()) != null){
                line = line.trim();
                String[] segs = line.split("\t");
                
                map.addBedData(new BedSimple(segs[0], Integer.parseInt(segs[1]), Integer.parseInt(segs[2]), segs[3]));
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        System.err.println("Created RepeatMask intersection map");
        
        Path singleMaps = Paths.get(cmd.GetValue("outbase") + ".trans.singles");
        getSingleMappings.getTransChrIDs(Paths.get(cmd.GetValue("input")), singleMaps);
        
        Path oneEndRepAnchors = Paths.get(cmd.GetValue("outbase") + ".trans.oneend");
        SortBedPE.condenseBedPE(singleMaps, oneEndRepAnchors, map);
        
        System.err.println("Identified one end repetitive anchors");
        
        BedClosest closeCheck = new BedClosest(5000);
        BedMap<BedClosest.BedCompare> results = closeCheck.RetrieveClosestNameComp(map, oneEndRepAnchors);
        
        Path finalOut = Paths.get(cmd.GetValue("outbase") + ".putative.mei");
        Path failedOut = Paths.get(cmd.GetValue("outbase") + ".failed.mei");
        try(BufferedWriter output = Files.newBufferedWriter(finalOut, Charset.defaultCharset())){
            BufferedWriter failed = Files.newBufferedWriter(failedOut, Charset.defaultCharset());
            Set<String> chrs = results.getListChrs();
            for(String c : SortByChr.ascendingChr(chrs)){
                for(BedClosest.BedCompare b : results.getSortedBedAbstractList(c)){
                    List<String> values = b.getOutStringList(true);
                    if(Integer.parseInt(values.get(8)) < 1000 && (Integer.parseInt(values.get(6)) != 0 && !values.get(7).equals("NA"))){
                        failed.write(StrUtils.StrArray.Join((ArrayList<String>)values, "\t") + System.lineSeparator());
                        continue;
                    }
                    output.write(StrUtils.StrArray.Join((ArrayList<String>)values, "\t") + System.lineSeparator());
                }
            }
            failed.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
    private static SimpleCmdLineParser setUpCmd(){
        return new SimpleCmdLineParser("MEIDivetID usage: -i <input divet> -r <input repeat bed file> -o <outbase name>" + System.lineSeparator());
    }
}
