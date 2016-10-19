===================================================================
   Ajavt (Ajaväljendite tuvastaja): 
   Temporal Expression Tagger for Estonian
===================================================================

  Temporal Expression Tagger is a program that detects time
  referring expressions (timexes) from natural language text and 
  normalises semantics of these expressions in a standard format. 
  
  This repository contains Ajavt, a rule-based language-specific 
  temporal expression tagger for Estonian. The tool uses an
  annotation format that is based on the TimeML's TIMEX3 tag
  (http://www.timeml.org), and it is currently tuned for temporal 
  tagging in news domain.
    
  Previous versions of this tool have been introduced in two 
  publications. Technical details about the implementation were 
  covered in (Orasmaa 2010), and a more general overview, 
  accompanied with evaluation of the tagger on different text 
  genres, was provided in (Orasmaa 2012).


===========================================
   Temporal expression tagging: an example
===========================================
  The sentence:

    Potsataja ütles eile, et vaatavad nüüd Genaga viie aasta plaanid 
    uuesti üle. 
    'Potsataja said yesterday that he and Gena will now check over 
    the plans for the five years.'

  is tagged for temporal expressions in a following way (assuming 
  that the creation time of the text is 2014-10-06):

    Potsataja ütles 
    <TIMEX tid="t1" type="DATE" value="2014-10-05">
    eile
    </TIMEX>, 
    et vaatavad 
    <TIMEX tid="t2" type="DATE" value="PRESENT_REF" anchorTimeID="t0">
    nüüd
    </TIMEX> 
    Genaga 
    <TIMEX tid="t3" type="DURATION" value="P5Y">
    viie aasta 
    </TIMEX> 
    plaanid uuesti üle. 


=========================
   Requirements
=========================
 For building the program (JAR file):
  ** Java JDK (at least version 1.8.x is expected);
  ** Apache Ant (at least version 1.8.2);
  
 For using the program:
  ** A sentence segmentator;
  ** A word tokenizer;
  ** Estonian morphological analyzer and disambiguator, possible 
     options:
     -- Filosoft's Vabamorf: https://github.com/Filosoft/vabamorf
     -- PyVabamorf:          https://github.com/estnltk/pyvabamorf
     -- T3MESTA (a commercial morphological analyzer)
     (NB! The program also works on morphologically ambiguous input, 
      but the quality of the analysis is expected to be lower than 
      on the morphologically disambiguated text.)
 

=========================
   Building the program
=========================
   The most straightforward way for compiling the program is by using
  Apache Ant and the build script ("build.xml" in root dir);
  
   Before building, correct path to JDK must be set in the file 
  "build.properties" (variable "java.home.location"). Then, building
  and deploying can be evoked with the command:

      ant deploy

  (in the same directory where "build.xml" is located);
   This compiles the Java source code, makes the JAR file (Ajavt.jar), 
  and copies the JAR file along with required files into the folder 
  "test";


=========================
   Using the program
=========================
   Before Ajavt can be applied on a text, a number of text preprocessing 
  steps must be made: text must be split into sentences and tokens 
  (words), and words must be morphologically analysed (and 
  disambiguated).
   These functionalities are provided by EstNLTK toolkit, so the 
  easiest way to use the program is within this toolkit ( see 
  https://github.com/estnltk/estnltk   for more details ).
   
   Processing JSON format texts
  ------------------------------
   In the JSON processing mode (flag '-format json'), it is expected that 
  the input of the program is in the same format as the output of 
  Vabamorf's command 'etana analyze' - a JSON structured text in UTF8 
  encoding. Note that the minimum JSON structure that the input should 
  have is:
     {"words": [ {"analysis": [ ... ],
                   "text": "word1"},
                  {"analysis": [ ... ],
                   "text": "word2"},
                   ...
                  {"analysis": [ ... ],
                   "text": "wordN"} ]}
   That is, an object with key "words" must be present, indicating an 
  analysable sentence.
   Also note that Ajavt expects that word root analyses are 'clean', 
  i.e. without any phonetic markup symbols (which can be optionally added 
  in 'etana' with flag '-phonetic').

   An example of JSON input can be found in file "test/example_input.json";
  In the "test" folder, following command evokes Ajavt on the input file 
  "example_input.json" and outputs the results to standard input:
  
     java -jar Ajavt.jar -format json -in file example_input.json -pretty_print

     (flag "-pretty_print" switches on the pretty printing mode, otherwise, 
      all of the output JSON is on a single line);
  
   Alternatively, output can also be directed to a file by specifying:

     java -jar Ajavt.jar -format json -in file example_input.json -pretty_print -out file my_output.json

   Document creation time (DCT)
  ------------------------------
   By default, temporal expressions with relative semantics (such as 
  'eile' / yesterday, 'reedel' / on Friday) are normalised with respect 
  to execution time of the program. This setting can be overridden by 
  providing a separate document creation time as an input of the 
  program:
   
     java -jar Ajavt.jar 1999-01-01TXX:XX -format json -in file example_input.json -pretty_print

   The document creation time must be in the format "YYYY-MM-DDThh:mm".
  Date/time fields marked with X-es are considered as unspecified/unknown.
  For example, it can be specified that the creation time is only known 
  at month level (e.g. 1999-01-XXTXX:XX) or at year level (e.g. 
  1999-XX-XXTXX:XX). This also affects the normalisation, e.g. if DCT is
  only specified at the year level, date-granularity relative expressions
  (for example: 'reedel' / on Friday) will be normalised as unspecified 
  temporal references (XXXX-XX-XXTXX:XX);
 
   Other remarks
  ------------------------------
   The Ajavt.jar should be executed in a directory that contains other files 
  required by the program:
       javax.json-1.0.4.jar
       joda-time-1.6.jar
       reeglid.xml
   
   The program can be executed with a custom configuration of rules, using the
  flag "-r" followed by the full path to the XML rules file (e.g.
  "reeglid.xml"):
    
       java -jar Ajavt.jar -format json -in file example_input.json -pretty_print -r FULL/PATH/TO/reeglid.xml

  Flag "-pyvabamorf" evokes the program in a special standard input/output 
  processing mode, where the program reads a JSON formatted line from the 
  standard input, analyzes the line, and outputs the results (in a single JSON 
  formatted line) to the standard output:

     java -jar Ajavt.jar -pyvabamorf 


============================
   Interpreting the output
============================
  The annotation format used by the program is described in the file 
 "doc/margendusformaat_et.pdf" (currently only in Estonian). Here,
 we give a brief overview how this format is expressed in JSON.
  
   JSON format output
  ----------------------
  In JSON input/output format, the presence of identified temporal expression(s) 
 is indicated by adding object "timexes" to the token (at the same level as 
 objects "text" and "analysis"). The "timexes" is a list of objects and each object 
 has (at minimum) a following structure:
        {
          "tid":   string,
        }
  where "tid" is an unique identifier of the temporal expression (in form that 
  can be described by a regular expression "t[0-9]+" ).
   (Note that in "-pyvabamorf" processing mode, this uniqueness only holds 
    within a single input line, which is expected to be a single document);

  If the token begins a temporal expression phrase (either a single-word phrase 
  or a multiword phrase), additional attribute/value pairs will be specified in 
  the timex object:
        "text" : string  
            // full extent phrase of the temporal expression
        "type" : string  
            // one of the following: "DATE", "TIME", "DURATION", "SET"
        "value": string
            // calendrical value (largely follows TimeML TIMEX3 value format),
            // but see "doc/margendusformaat_et.pdf" for details;
        "temporalFunction": string ("true" or "false")
            // indicates whether the semantics of the expression are relative 
            // to the context:
            //  *) For DATE and TIME expressions:
            //      "true" indicates that the expression is relative and 
            //             semantics have been computed by heuristics;
            //      "false" indicates that the expression is absolute and 
            //             semantics haven't been computed by heuristics;
            //  *) For DURATION expressions, the value is mostly "false", 
            //     except for vague durations;
            //  *) For SET expressions, the value is always "true";

  Depending on the (semantics of the) temporal expression, there can also be 
  other attribute/value pairs:
         "mod" : string
            // largely follows TimeML TIMEX3 mod format, with two additional 
            // values used to mark first/second half of the date/time (e.g. "in 
            // the first half of the month"):  FIRST_HALF, SECOND_HALF;
         "anchorTimeID"
            // points to the temporal expression (by identifier) that this 
            // expression has been anchored to while calculating or determining 
            // the value;
            // "t0" -- means that the expression is anchored to document 
            // creation time;
         "beginPoint"
            // in case of DURATION: points to the temporal expression (by 
            // identifier) that serves as a beginning point of this duration;
            // "?" -- indicates problems on finding the beginning point;
         "endPoint"
            // in case of DURATION: points to the temporal expression (by 
            // identifier) that serves as an ending point of this duration;
            // "?" -- indicates problems on finding the ending point;
         "quant"
            // Quantifier; Used only in some SET expressions, e.g. quant="EVERY"
         "freq" 
            // Used in some SET expressions, marks frequency of repetition, 
            // e.g. "three days in each month" will be have freq="3D"
 
   An example
  --------------
    The sentence
      "Potsataja ütles eile, et vaatavad nüüd Genaga viie aasta plaanid uuesti üle."
      (created at 2014-10-06)
    
    will obtain following temporal expression annotations:
       {
            "words":[  { "analysis":[ ... ],
                         "text":"Potsataja"
                       },
                       { "analysis":[ ... ],
                         "text":"ütles"
                       },
                       { "analysis":[ ... ],
                         "text":"eile,",
                         "timexes":[ { "tid":"t1",
                                       "text":"eile,",
                                       "type":"DATE",
                                       "temporalFunction":"true",
                                       "value":"2014-10-05" } ]
                       },
                       { "analysis":[ ... ],
                         "text":"et"
                       },
                       { "analysis":[ ... ],
                         "text":"vaatavad"
                       },
                       {
                         "analysis":[ ... ],
                         "text":"nüüd",
                         "timexes":[ { "tid":"t2",
                                       "text":"nüüd",
                                       "type":"DATE",
                                       "temporalFunction":"true",
                                       "value":"PRESENT_REF",
                                       "anchorTimeID":"t0"  } ]
                       },
                       { "analysis":[ ... ],
                         "text":"Genaga"
                       },
                       { "analysis":[ ... ],
                         "text":"viie",
                         "timexes":[ { "tid":"t3",
                                       "text":"viie aasta",
                                       "type":"DURATION",
                                       "temporalFunction":"false",
                                       "value":"P5Y" } ]
                       },
                       { "analysis":[ ... ],
                         "text":"aasta",
                         "timexes":[ { "tid":"t3",
                                       "text":"viie aasta" } ]
                       },
                       { "analysis":[ ... ],
                         "text":"plaanid"
                       },
                       { "analysis":[ ... ],
                         "text":"uuesti"
                       },
                       { "analysis":[ ... ],
                         "text":"üle"
                       }
                    ]
        }

    which should be interpreted as:
       "eile," -- is a single-word temporal expression, which is from type 
                  "DATE", and which refers to the date "2014-10-05";
       "nüüd" --  is a single-word temporal expression, which is from type 
                  "DATE", and which has an uncertain calendaric value, but it 
                  refers to the present time (PRESENT_REF), contemporary to 
                  the document creation time (t0, which is 2014-10-06);
       "viie", "aasta" -- forms a multiword temporal expression phrase 
                  ("viie aasta"), referring to a period ("DURATION") of 
                  length 5 years;
                  
   Specifics
  --------------
  I. Note that there can also be timexes with no "text" value, i.e. timexes that 
  form an implicit duration (A), or mark implicit beginning or ending points (B):

    (A) e.g. "2001-2005" -- the period covering explicit timepoints "2001-" and 
                            "2005" is annotated as a timex (DURATION) with no 
                            textual content;

    (B) e.g. "following three years" -- beginning and ending timepoints of the 
                            explicit duration expression ("three years") are marked 
                            as timexes with no textual content;

  II. The program does not always resolve the ambiguities of possible multiple 
  readings of temporal expressions, e.g. "aastas 2000 tundi" can be interpreted 
  as "aastas 2000" (in year 2000) or as "2000 tundi" (2000 hours). In case of 
  ambiguities, "timexes" also lists multiple timex objects.


===============================
   Development and evaluation
===============================

   The structure
  ------------------
  The Ajavt project has following directory structure:
  
  [doc]                            <--- documentation about the annotation format
                                        and about format of the rules file;  

  [lib]                            <--- Java dependencies of the program;

  [res]                            <--- resources used by the program:
  [res\reeglid.xml]                <--- the rules file

  [src]                            <--- source of the program:
  [src\ee\ut\soras\ajavtV2]        <--- main source of the tagger;
  [src\ee\ut\soras\wrappers]       <--- wrappers for handling different input formats,
                                        and a common model for encapsulating 
                                        morphological analyses;

  [test-src]                       <--- methods for automated testing & evaluation;
  [test-src\ee\ut\soras\test_ajavt] --- tools for evaluating the tagger on an 
                                        annotated corpus;
      
  [test]                           <--- the testing folder;
                                        tagger's JAR file along with required 
                                        dependencies will be deployed here;
      
  readme.txt        <--- you are here :)
  build.properties  <--- configuration for the Ant build script;
  build.xml         <--- the Ant build script for compiling, deploying and testing 
                         the tagger;


   Automated testing and evaluation
  ----------------------------------
  This distribution also contains tools for automated testing/evaluating the tagger 
  against manually annotated TIMEX corpora. In order to set up and use the automatic 
  evaluation, proceed in following steps:
  
   I. Download Estonian TIMEX annotated corpora from following repository:
         https://github.com/soras/EstTimexCorpora

   II. Modify "build.properties" of this program and set the root directory of
       evaluation corpora:
         test.root=FULL/PATH/TO/EstTimexCorpora

   III. Modify "build.xml" of this program to enable automated testing: remove 
        the comments around properties "use.tml.corpus.04" and "use.t3o.corpus.03".
       
        The property "use.tml.corpus.04" enables the evaluation task "test-tml-04",
        and the property "use.t3o.corpus.03" enables the task "test-t3-olp-03";

   IV. Execute the automatic evaluation on all corpora with the command:
            ant test-all

       Alternatively, evaluation can be executed only on the TML format corpus:
            ant test-tml

       and only on the T3-OLP-AJAV format corpus:
            ant test-t3-olp

   V. The evaluation program will output a detailed analysis on matching (and 
      mismatching) TIMEX annotations for each document. Additionally, precisions 
      and recalls on TIMEX extents and attributes will be reported for each 
      document, and microaverages of these measures will be reported at the end 
      of the evaluation;
      
      The results of the evaluation will also be written into text files, marked 
      with the timestamp of evaluation. Each evaluation corpus has a subdirectory 
      "testlog" that stores these text files.


============================
   Acknowledgements and 
   license
============================
  Copyright (C) 2009-2016  University of Tartu
  Author:   Siim Orasmaa  ( siim . orasmaa {at} ut . ee )

  Ajavt is released under the GNU General Public License version 2.

  Dependency libraries have their own respective license terms, see 
  "lib/LIB_LICENSES.txt" for details.

  Development of this tool has been supported by the National Programme 
  for Estonian Language Technology under projects EKKTT09-66, EKT7 and 
  EKT57.
  
============================
   References
============================

 Orasmaa, S. (2010). Ajaväljendite tuvastamine eestikeelses tekstis 
 (Recognition and Resolution of Estonian Temporal Expressions). Master’s 
 thesis, University of Tartu. (in Estonian).
  ( url: http://comserv.cs.ut.ee/forms/ati_report/downloader.php?file=F0E53012D5F88F71DD6E2E84830460F334E14EA2 )

 Orasmaa, S. (2012) "Automaatne ajaväljendite tuvastamine eestikeelsetes tekstides" 
 (Automatic Recognition and Normalization of Temporal Expressions in Estonian 
 Language Texts). Eesti Rakenduslingvistika Ühingu aastaraamat 8: 153-169.
