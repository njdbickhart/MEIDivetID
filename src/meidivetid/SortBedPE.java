/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meidivetid;

import file.BedMap;
import file.BedSimple;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import utils.BinBed;
import utils.LineIntersect;
import static utils.LineIntersect.ovCount;
import utils.SortByChr;

/**
 *
 * @author Derek.Bickhart
 */
public class SortBedPE {
    
    public static <T extends BedSimple> void condenseBedPE(Path bedpe, Path output, final BedMap<T> repeats){
        BedMap<T> collector = new BedMap<>();
        
        MapIntersect intersector = new MapIntersect();
        try(BufferedReader input = Files.newBufferedReader(bedpe, Charset.defaultCharset())){
            String line; 
            
            while((line = input.readLine()) != null){
                line = line.trim();
                String[] segs = line.split("\t");
                
                
                RepeatMatch useful = intersector.returnPEMapIntersect(repeats, segs[0], Integer.parseInt(segs[1]), Integer.parseInt(segs[2]),
                        segs[3], Integer.parseInt(segs[4]), Integer.parseInt(segs[5]));
                
                if(!useful.use)
                    continue;
                else{
                    String anChr = (useful.firstRep)? segs[3] : segs[0];
                    int anStart = (useful.firstRep)? Integer.parseInt(segs[4]) : Integer.parseInt(segs[1]);
                    int anEnd = (useful.firstRep)? Integer.parseInt(segs[5]) : Integer.parseInt(segs[2]);
                    
                    collector = IntersectIncrement(collector, anChr, anStart, anEnd, useful.repname);
                }
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        
        try(BufferedWriter out = Files.newBufferedWriter(output, Charset.defaultCharset())){
            Set<String> chrs = collector.getListChrs();
            List<String> sortChrs = SortByChr.ascendingChr(chrs);
            
            for(String c : sortChrs){
                for(T bed : collector.getSortedBedAbstractList(c)){
                    if(bed.Type() > 2)
                        out.write(bed.Chr() + "\t" + bed.Start() + "\t" + bed.End() + "\t" + bed.Name() + "\t" + bed.Type() + System.lineSeparator());
                }
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
    public static <T extends BedSimple> BedMap<T> IntersectIncrement(BedMap<T> collector, String anChr, int anStart, int anEnd, String repname){
        if(collector.containsChr(anChr)){
            boolean stop = false;
            for(int b : BinBed.getBins(anStart, anEnd)){
                if(collector.containsBin(anChr, b)){                    
                    for(T bed : collector.getBedAbstractList(anChr, b)){
                        if(ovCount(bed.Start(), bed.End(), anStart, anEnd) > 0 && bed.Name().equals(repname)){
                            bed.setType(bed.Type() + 1);
                            stop = true;
                            break;
                        }
                    }
                }else{
                    collector.addBedData((T) new BedSimple(anChr, anStart, anEnd, repname));
                }
                
                if(stop)
                    break;
            }
        }else{
            collector.addBedData((T) new BedSimple(anChr, anStart, anEnd, repname));
        }
        
        return collector;
    }
    
    protected static class MapIntersect extends LineIntersect{
        public static <T extends BedSimple> RepeatMatch returnPEMapIntersect(BedMap<T> a, String chr1, int start1, int end1, String chr2, int start2, int end2){
            
            boolean firstMap = false, secondMap = false;
            String repname = "NA";
            if(a.containsChr(chr1)){
                Set<Integer> bins = BinBed.getBins(start1, end1);
                for(int b : bins){
                    if(a.containsBin(chr1, b)){
                        for(BedSimple bed : a.getBedAbstractList(chr1, b)){
                            if(ovCount(bed.Start(), bed.End(), start1, end1) > 0){
                                firstMap = true;
                                repname  = bed.Name();
                                break;
                            }
                        }
                    }
                }
            }
            
            if(a.containsChr(chr2)){
                Set<Integer> bins = BinBed.getBins(start2, end2);
                for(int b : bins){
                    if(a.containsBin(chr2, b)){
                        for(BedSimple bed : a.getBedAbstractList(chr2, b)){
                            if(ovCount(bed.Start(), bed.End(), start2, end2) > 0){
                                secondMap = true;
                                repname  = bed.Name();
                                break;
                            }
                        }
                    }
                }
            }
            
            boolean use = ((!firstMap && secondMap) || (firstMap && !secondMap))? true : false;
            return new RepeatMatch(firstMap, secondMap, use, repname);
        }
    }
    
    protected static class RepeatMatch{
        public boolean firstRep = false;
        public boolean secondRep = false;
        public boolean use = false;
        public String repname;
        
        public RepeatMatch(boolean firstRep, boolean secondRep, boolean use, String repname){
            this.firstRep = firstRep;
            this.secondRep = secondRep;
            this.use = use;
            this.repname = repname;
        }
    }
}
