********************
CONVERGER

Sean Peuquet, 2013
seanpeuquet[at]ludicsound.com
www.ludicsound.com
********************

A DYNAMIC SAMPLE-BASED SYNTHESIS INSTRUMENT + CONVERGENT-ARRAY CONTROL 

Build Dependencies:
1. libsndfile

Easiest to install via home-brew (enter the following into terminal):

	$ /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

once home-brew is on your machine, enter: 

	$ brew install flac

	$ brew install libsndfile

2. SuperCollider (3.6.5), tested through IDE. 

	http://supercollider.sourceforge.net/

3. Python 2.7 (or higher)

4. PVCPlus (optional: only if you want to use automatic pitch detection when populating 
	audio sample library)

	http://sourceforge.net/projects/pvcplus/

*********************************

INSTRUCTIONS:

1. from within the /Python directory (you must "cd" into it), run the following command to execute the LibraryManager.py Python script which builds and manages the sample library. Crotales and SteinwayGrandPiano samples are included as a test-case for the library:

	$ python LibraryManager.py build

2. Move the subfolders inside the /SC_Extensions directory (Classes & HelpSource) to "~/Library/Application\ Support/Extensions".

3. Launch SuperCollider, or recompile (command-shift-l) SCLang. Open the CONVERGENCE_DEMO.scd file in the /SC_Demo directory and check out the examples. Further instructions are provided as comments above functional code examples. WARNING: you must boot the server before you attempt to load audio samples into memory. 

4. The ConvergentArray algorithm is discussed in detail both theoretically and technically in the PEUQUET_S_Convergence.pdf file included in the main directory. Please review for a fuller understanding of some aesthetic considerations regarding its use. Also, refer to the SC Help files (command-d) for ConvergentArray and SFRand for more information.




	

	
