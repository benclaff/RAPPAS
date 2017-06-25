/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alignement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import inputs.Fasta;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author ben
 */
public class Alignment implements Serializable {
    
    private static final long serialVersionUID = 1000L;

    //base def of the alignment
    private char[][] charMatrix=null; //char[row][column]
    private int[] colPartitionIds=null;
    private String[] rowLabels=null;

    //metadata
    private ArrayList<Partition> partitions=null;
    

    /**
     * simplest constructor, no partitions
     * @param charMatrix
     * @param colLabels
     * @param rowLabels 
     */
    public Alignment(char[][] charMatrix, String[] rowLabels) {
        this.charMatrix=charMatrix;
        this.rowLabels=rowLabels;
    }
    
    /**
     * constructor copy 
     * @return 
     */
    public Alignment copy(){
        return new Alignment(charMatrix, rowLabels);
    }

    /**
     * build an alignment from a list of fasta objects,\n
     * order is kept 
     * @param fastas 
     */
    public Alignment(List<Fasta> fastas) {
        for (int i = 0; i < fastas.size(); i++) {
            Fasta f = fastas.get(i);
            //init matrix 
            if (i==0) {
                charMatrix=new char[fastas.size()][f.getSequence().length()];
                colPartitionIds=new int[f.getSequence().length()];
                rowLabels=new String[fastas.size()];
            }
            
            for (int j = 0; j < f.getSequence().length(); j++) {
                charMatrix[i][j]=f.getSequence().charAt(j);
            }
            rowLabels[i]=f.getHeader();
        }
    }

    /**
     * build an alignment from a list of fasta objects,\n
     * order is kept, partitions define the column labels
     * @param fastas 
     */
    public Alignment(List<Fasta> fastas, ArrayList<Partition> partitions) {
        for (int i = 0; i < fastas.size(); i++) {
            Fasta f = fastas.get(i);
            
            //init matrix 
            if (i==0) {
                charMatrix=new char[fastas.size()][f.getSequence().length()];
                colPartitionIds=new int[f.getSequence().length()];
                rowLabels=new String[fastas.size()];
            }
            
            for (int j = 0; j < f.getSequence().length(); j++) {
                charMatrix[i][j]=f.getSequence().charAt(j);
            }
            rowLabels[i]=f.getHeader();
        }
        this.partitions=partitions;
        //fill column names with partitions
        for (int i = 0; i < partitions.size(); i++) {
            Partition p=partitions.get(i);
            for (int j = p.getStart(); j < p.getEnd()+1; j++) {
                colPartitionIds[j]=i;
            }
            
        }
    }

    public void addSequence(String label, char[] seq) {
        //reinstantiate table with a new line
        char[][] newCharMatrix=new char[charMatrix.length+1][charMatrix[0].length];
        for(int i=0; i<charMatrix.length; i++)
            System.arraycopy(charMatrix[i], 0, newCharMatrix[i], 0, charMatrix[i].length);
        System.arraycopy(seq, 0, newCharMatrix[charMatrix.length], 0, charMatrix[0].length);
        charMatrix=newCharMatrix;
        //reinstantiate table with a new line
        String[] newRowLabels=new String[rowLabels.length+1];
        System.arraycopy(rowLabels, 0, newRowLabels, 0, rowLabels.length);
        newRowLabels[rowLabels.length]=label;
        rowLabels=newRowLabels;
    }
    
    /**
     * use with parsimony, this reinstanciates the arrays
     * @param label 
     */
    public void removeSequence(String label) {
        boolean found=false;
        //copy table, remove only the concerned sequence
        char[][] newMatrix=new char[charMatrix.length-1][charMatrix[0].length];
        String[] newLabels=new String[rowLabels.length-1];
        int shift=0;//line index shift
                
        for (int i = 0; i < rowLabels.length; i++) {
            String rowLabel = rowLabels[i];
            if (rowLabel.equals(label)) {
                found=true;
                shift++;
                continue;
            }
            //copy data
            newLabels[i-shift]=rowLabels[i];
            for (int k = 0; k < charMatrix[i].length; k++) {
                newMatrix[i-shift][k]=charMatrix[i][k];
            }
        }
        //update this object
        rowLabels=null;
        charMatrix=null;
        rowLabels=newLabels;
        charMatrix=newMatrix;
        
        
        assert found==true;
    }
    
    /**
     * return a particular sequence as a fasta (keeping gaps)
     * @param label
     * @return 
     */
    public Fasta getFasta(String label, boolean withGaps) {
        Fasta f=null;
        for (int i = 0; i < rowLabels.length; i++) {
            if (rowLabels[i].equals(label)) {
                String seq=new String(charMatrix[i]);
                if (withGaps) {seq.replaceAll("-", "");}
                f=new Fasta(label, seq);
                break;
            }
            
        }
        return f;
    }
    
    public List<Fasta> getAllFasta(boolean withGaps) {
        List<Fasta> l=new ArrayList<>(rowLabels.length);
        for (int i = 0; i < rowLabels.length; i++) {
            String seq=new String(charMatrix[i]);
            if (withGaps) {seq.replaceAll("-", "");}
            l.add(new Fasta(rowLabels[i], seq));  
        }
        return l;
    }
    
    
    public char[][] getCharMatrix() {
        return charMatrix;
    }

    public String[] getRowLabels() {
        return rowLabels;
    }

    public void setRowLabels(String[] rowLabels) {
        this.rowLabels = rowLabels;
    }
    
    public int getLength() {
        return charMatrix[0].length;
    }
    

    public void writeAlignmentAsFasta(File f) throws IOException {
        FileWriter fw=new FileWriter(f);
        for (int i = 0; i < charMatrix.length; i++) {
            fw.append(">");
            fw.append(rowLabels[i]);
            fw.append("\n");
            fw.append(String.copyValueOf(charMatrix[i]));
            fw.append("\n");
        }
        fw.close();
    }
    
    public void writeAlignmentAsPhylip(File f) throws IOException {
        
        
        //here choose 50 because PAML don't allow more !
        int allowedLabelSize=50; //in fact(48 +2 spaces)
        int numColumns=2;
        
        FileWriter fw=new FileWriter(f);
        fw.append(String.valueOf(charMatrix.length)+" "+String.valueOf(charMatrix[0].length)+"\n");     
        for (int i = 0; i < charMatrix.length; i++) {
            //add label
            String label=rowLabels[i];
            if (label.length()>allowedLabelSize) {
                label=label.substring(0, allowedLabelSize-2);
            }
            fw.append(label);

            for (int j = 0; j < (allowedLabelSize-label.length()); j++)  {
                fw.write(' ');
            }
            //add sequence
            for (int j=0;j<charMatrix[0].length;j++) {
                if (j!=0 && (j%allowedLabelSize)==0)
                    if (allowedLabelSize*numColumns==0)
                        fw.write('\n');
                    else
                        fw.write(' ');
                fw.write(charMatrix[i][j]);
            }
            fw.append("\n");
        }
        fw.close();
    }
    
    public String describeAlignment(boolean extended) {
        StringBuilder sb=new StringBuilder();
        sb.append("Dimension: "+charMatrix[0].length+"x"+charMatrix.length+" (colxline)");
        if (extended) {
            if (partitions!=null) {
                sb.append("\nPartitions: "+partitions.size()+"\n");
                for (int i = 0; i < partitions.size(); i++) {
                    sb.append("P"+i+" :"+partitions.get(i)+"\n");
                }
                sb.append("P assigned: ");
                if (colPartitionIds!=null) {
                    sb.append("YES\n");
                } else {
                    sb.append("NO\n");
                } 
            }else {
                sb.append(" Partitions: none");
            }
        } 
        return sb.toString();
    }
    
    
    
}
