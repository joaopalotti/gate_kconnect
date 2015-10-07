/*
 *  BatchProcessLog.java
 *
 *
 * Copyright (c) 2006, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  Ian Roberts, March 2006
 *
 *  $Id: BatchProcessLog.java,v 1.5 2006/06/11 19:17:57 ian Exp $
 */

import gate.Document;
import gate.corpora.DocumentContentImpl;
import gate.DocumentContent;
import gate.Corpus;
import gate.CorpusController;
import gate.AnnotationSet;
import gate.Annotation;
import gate.Gate;
import gate.Factory;
import gate.FeatureMap;
import gate.util.*;
import gate.util.persistence.PersistenceManager;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;

/**
 * This class ilustrates how to do simple batch processing with GATE.  It loads
 * an application from a .gapp file (created using "Save application state" in
 * the GATE GUI), and runs the contained application over one or more files.
 * The results are written out to XML files, either in GateXML format (all
 * annotation sets preserved, as in "save as XML" in the GUI), or with inline
 * XML tags taken from the default annotation set (as in "save preserving
 * format").  In this example, the output file names are simply the input file
 * names with ".out.xml" appended.
 *
 * To keep the example simple, we do not do any exception handling - any error
 * will cause the process to abort.
 */
public class BatchProcessLog {

    /** Index of the first non-option argument on the command line. */
    //private static int firstFile = 0;

    /** Path to the saved application file. */
    private static File gappFile = null;

    /** 
     * List of annotation types to write out.  If null, write everything as
     * GateXML.
     */
    private static List annotTypesToWrite = null;

    /**
     * The character encoding to use when loading the docments.  If null, the
     * platform default encoding is used.
     */
    private static String encoding = null;
    
    private static String inputFile = null;
    private static String outputFile = null;

    /**
     * The main entry point.  First we parse the command line options (see
     * usage() method for details), then we take all remaining command line
     * parameters to be file names to process.  Each file is loaded, processed
     * using the application and the results written to the output file
     * (inputFile.out.xml).
     */
    public static void main(String[] args) throws Exception {
        parseCommandLine(args);

        // initialise GATE - this must be done before calling any GATE APIs
        Gate.init();

        // load the saved application
        CorpusController application =
            (CorpusController)PersistenceManager.loadObjectFromFile(gappFile);

        Corpus corpus = Factory.newCorpus("BatchProcessLog Corpus");
        application.setCorpus(corpus);

        CSVParser parser = new CSVParserBuilder().withSeparator(',').withEscapeChar('\\').withQuoteChar('\"').withIgnoreLeadingWhiteSpace(false).withStrictQuotes(true).withIgnoreQuotations(false).build();  
        final CSVReader reader = new CSVReaderBuilder(new FileReader(inputFile)).withSkipLines(0).withCSVParser(parser).build();
        
        CSVWriter writer = new CSVWriter(new FileWriter(outputFile), ',', '"', '\\' );

        String [] nextLine;

        while ((nextLine = reader.readNext()) != null) {
            if(nextLine.length == 6){
                String query = nextLine[2];

                // Creates a temporary file and writes the query there
                File temp = File.createTempFile("_gate_", ".tmp");
                BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
                bw.write(query);
                bw.close();

                Document doc = Factory.newDocument(temp.toURL(), encoding);

                // remove the document from the corpus again
                corpus.clear();
                
                // put the document in the corpus
                corpus.add(doc);

                // run the application
                application.execute();

                System.out.println("Query: " + query);
                System.out.println("Writing file " + temp.getName());

                // Set temporary file to be deleted
                temp.deleteOnExit();


                String docXMLString = "";
                // if we want to just write out specific annotation types, we must
                // extract the annotations into a Set
                if(annotTypesToWrite != null) {
                    // Create a temporary Set to hold the annotations we wish to write out
                    Set annotationsToWrite = new HashSet();

                    // we only extract annotations from the default (unnamed) AnnotationSet
                    // in this example
                    AnnotationSet defaultAnnots = doc.getAnnotations("Output");
                    Iterator annotTypesIt = annotTypesToWrite.iterator();
                    while(annotTypesIt.hasNext()) {
                        // extract all the annotations of each requested type and add them to
                        // the temporary set
                        AnnotationSet annotsOfThisType = defaultAnnots.get((String)annotTypesIt.next());
                        if(annotsOfThisType != null) {
                            annotationsToWrite.addAll(annotsOfThisType);
                        }
                    }

                    // create the XML string using these annotations
                    //System.out.println(doc.getNamedAnnotationSets());
                    //System.out.println(doc.getAnnotationSetNames()); //[Output]
                    //System.out.println(annotationsToWrite);
                    
                    Iterator annot = annotationsToWrite.iterator();
                    while(annot.hasNext()){
                        Annotation s = (Annotation) annot.next();
                        FeatureMap fm = s.getFeatures();
                        
                        DocumentContentImpl dc = (DocumentContentImpl) doc.getContent();
                        String annotatedPart = ((DocumentContentImpl) dc.getContent(s.getStartNode().getOffset(),
                                    s.getEndNode().getOffset())).getOriginalContent();
                        String type = s.getType();
                        String cui = (String) fm.get("cui");
                        String tui = (String) fm.get("tui");
                        
                        //System.out.println("Content: " + annotatedPart);
                        //System.out.println("Type: " + s.getType() );
                        //System.out.println("CUI: " + fm.get("cui") );
                        //System.out.println("TUI: " + fm.get("tui") );
                        
                        docXMLString = docXMLString + annotatedPart + "|" + s.getType() + ";" + fm.get("cui") + ";" + fm.get("tui") + "$";
                    }
                    //docXMLString = doc.toXml(annotationsToWrite, true);
                }
                // otherwise, just write out the whole document as GateXML
                else {
                    docXMLString = doc.toXml();
                }

                // Release the document, as it is no longer needed
                Factory.deleteResource(doc);

                String [] toWrite = new String[nextLine.length + 1];
                int field = 0;
                for(; field < nextLine.length; field++)
                    toWrite[field] = nextLine[field];
                
                toWrite[field] = docXMLString;
                writer.writeNext(toWrite);
            }
        } // for each line of query log

        writer.close();
        System.out.println("All done");
    
    } // void main(String[] args)


    /**
     * Parse command line options.
     */
    private static void parseCommandLine(String[] args) throws Exception {
        int i;
        // iterate over all options (arguments starting with '-')
        for(i = 0; i < args.length && args[i].charAt(0) == '-'; i++) {
            switch(args[i].charAt(1)) {
                // -a type = write out annotations of type a.
                case 'a':
                    if(annotTypesToWrite == null) annotTypesToWrite = new ArrayList();
                    annotTypesToWrite.add(args[++i]);
                    break;

                // -g gappFile = path to the saved application
                case 'g':
                    gappFile = new File(args[++i]);
                    break;

                // -e encoding = character encoding for documents
                case 'e':
                    encoding = args[++i];
                    break;

                // -i dir = dir where the input files should be
                case 'i':
                    inputFile = args[++i];
                    break;

                // -o dir = dir where the output files will be
                case 'o':
                    outputFile = args[++i];
                    break;

                default:
                    System.err.println("Unrecognised option " + args[i]);
                    usage();
            }
        }

        // set index of the first non-option argument, which we take as the first
        // file to process
        //firstFile = i;

        // sanity check other arguments
        if(gappFile == null) {
            System.err.println("No .gapp file specified");
            usage();
        }
    }

    /**
     * Print a usage message and exit.
     */
    private static final void usage() {
        System.err.println(
                "Usage:\n" +
                "   java sheffield.examples.BatchProcessLog -g <gappFile> [-e encoding]\n" +
                "            [-a annotType] [-a annotType] file1 file2 ... fileN\n" +
                "\n" +
                "-g gappFile : (required) the path to the saved application state we are\n" +
                "              to run over the given documents.  This application must be\n" +
                "              a \"corpus pipeline\" or a \"conditional corpus pipeline\".\n" +
                "\n" + 
                "-e encoding : (optional) the character encoding of the source documents.\n" +
                "              If not specified, the platform default encoding (currently\n" +
                "              \"" + System.getProperty("file.encoding") + "\") is assumed.\n" +
                "\n" + 
                "-a type     : (optional) write out just the annotations of this type as\n" +
                "              inline XML tags.  Multiple -a options are allowed, and\n" +
                "              annotations of all the specified types will be output.\n" +
                "              This is the equivalent of \"save preserving format\" in the\n" +
                "              GATE GUI.  If no -a option is given the whole of each\n" +
                "              processed document will be output as GateXML (the equivalent\n" +
                "              of \"save as XML\").\n" +
                "-i filename : (required) reads this log file\n" +
                "-o filename : (required) outputs a processeed file with this name"
                );

        System.exit(1);
    }

}
