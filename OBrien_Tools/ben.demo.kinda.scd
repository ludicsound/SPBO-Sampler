//1. init - go line by line and you'll need to file in your own dir
~path = "/Users/Sean/Music/Samples/crotales/"
a = BenUtils.new;

/*2. - looks in directory and makes a .txt file with the /path/to/audio/file and channel # */
/* if different file formats exist in dir, it will select the files with the most */
/* OPTIONALLY, you can specify extension with second arg (as a string). choices (currently) include  "au", "wav", "aiff", "aif" */
a.makeTxt(~path); //file name is: order.FILE FORMAT.txt, so, for example, order.wav.txt

/* optional */
a.display(~path) //displays all files in dir - check to make sure the txt file exists!

a.openFile(~path) //opens all files in dir
a.openFile("path/to/audio/file") //opens a specific file
a.openFile(~path, "txt") //open all txt files in dir

a.readTxt(~path) //reads all txt files in dir

/*3 - Pitchtrack by using the txt file you just made! */
/* will output a txt file, where each line includes:*/
/* (0) path/to/file (1) # of channels (2) rounded fund freq (3) fund freq (4) pvc mean freq (5) low freq (6) highest freq*/
/* NOTE THIS IS WHERE I NEED YOU HELP! */
PVC_Pitch(~path ++ "order.wav.txt");

//you can specify different args - see file
PVC_Pitch(~path ++ "order.wav.txt", fft: 512, ref: 500, low: 100, high: 1000)

/* the txt file will be named the first soundfile's name (minus extension) ++ .order.txt and will be in the /Users/benji/pvc_pitch dir (which will automatically be made if needed) */
/* example: /Users/benji/pvc_pitch/Crotales.order.txt */

/* optional - can 'group' different txt files in directory into a single txt file */
/* can specify strings */
~pvc_path = "/Users/benji/pvc_pitch";
a.groupTxt(~pvc_path) //groups all .txt files into one single txt file: /Users/benji/pvc_pitch/group.order.txt
a.group(~pvc_path, "Viola");
a.group(~pvc_path, ["Viola", "Pizz"] ); //will include ALL - Union - that contain these strings

/*optional you can remove files from .txt file */
a.removeTxt(~pvc_path, "Pizz") // removes all lines that include "pizz" in the txt file and SAVES OVER the org.

/* 4 - read txt file and order it by specifying different values */
~final_path = "/Users/benji/pvc_pitch/group.order.txt"
a.orderFromTxt(~final_path, 2) //will order all the lines given (2) fund freq
a.orderFromTxt(~final_path, 6) //will order all the lines given (6) high freq - not sure why you would do this . . .
/* output file: /Users/benji/pvc_pitch/group.reorder.txt */


/* 5. concat using the .txt file -- you can also just try to concat given a directory of audio files */
a.cat("/Users/benji/pvc_pitch/group.reorder.txt");

/*-- you can also just try to concat given a directory of audio files */
a.cat(~path, "au", norm: 1) //given audio files in ~path dir, choose all those that have the same # of channels and .au and normalize before concatenating

/* option you can remove files - REMOVES COMPLETELY FROM DISC - BE CAREFUL AND CHECK MY WORK? IT WORKS FOR ME, BUT MAYBE YOU HAVE A BETTER METHOD */
a.remove(~path, "txt") //removes all txt files from ~path dir