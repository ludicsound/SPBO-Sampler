/*
0. fix pvc_pitch to wait til all analysis is done
1. need to re-write order functions to only include those that have equal channels
2. write remove paths from reorder text (if channels don't line up)
2.need to write analysis data to datafile
3. divide algorithm
*/

BenUtils{
	var format_array;
	*new{ ^super.new.init }

	init{ //init global vars
		format_array = ["wav", "aiff", "aif", "au"];
	}

	//•••••••••••••••••••••INSTANCE METHODS•••••••••••••••••••••••••••••••••
	//display files in a dir; specify extension (ext)
	display{|dir, ext|
		if(PathName(dir).isFolder, {
			PathName(dir).files.do{|file| if(ext.isNil or: {ext.isNil.not && (file.extension == ext)}, { file.fullPath.postln; }); };
		}, { "Please enter a directory".postln; });
	}

	//open ALL TYPES of the files in a w/ default application (most likely TextEdit)
	openFile{|dir, string|
		var open = {|file| ("Opening " ++ file).postln; ("open " ++ file).unixCmd; };

		if(PathName(dir).isFile and: { PathName(dir).extension == "txt"}, { open.value(PathName(dir).fullPath); });

		if(PathName(dir).isFolder and: { PathName(dir).files.isNil.not }, {
			PathName(dir).files.do{|file|
				if(string.isNil or: { file.fileName.contains(string) }, { open.value(file.fullPath); });
			};
		});
	}

	//writes a .txt file of the paths/to/recording in "dir"; "ext" specifies files that have extension
	makeTxt{|dir, ext|
		var outputName, prepare = this.prepare(dir, ext), file;
		if(prepare[0], {
			outputName = dir.withoutTrailingSlash ++ "/order." ++ prepare[1].asString ++ ".txt";

			file = File.new(outputName, "w");
			prepare[3].size.do{|i| file.write(prepare[3][i].asString ++ " " ++ prepare[2] ++ "\n"); };
			file.close;
			("WRITING FILE:  " ++ outputName).postln;
		}, { "inconsistent # of channels".postln; });
	}

	/* finds .txt files in dir to be used for ordering recordings and returns array of /paths/to/file (.txt)*/
	/* optional - only include .txt files that contain "string" */
	findTxt{|dir, string|
		var flag = List.new, tmp;
		"FINDING FILES . . .".postln;
		if(PathName(dir).isFolder and: { PathName(dir).files.isNil.not }, {
			PathName(dir).files.do{|file|
				tmp = this.parse(file.fileName);
				if(tmp.includesEqual("txt"), {
					if(string.isNil or: { file.fileName.contains(string) }, { flag.add(file.fullPath); });
				});
			};
			if(flag.asArray.size == 0, { flag.add(false); });
		});
		^flag.asArray;
	}

	/* finds all .txt files in a dir and returns 3D array - [list of files [file [items] ] ] */
	/* optional - only include .txt files that contain "string" */
	readTxt{|dir, string|
		var find, order = List.new;
		if(PathName(dir).isFolder, {
			find = this.findTxt(dir, string);
			if(find != false, {
				order = Array.fill(find.size, {|item| item = List.new});
				find.do{|file, i|
					("Reading: " ++ file).postln;
					order[i] = this.fileToArray(file);
					if(order[i] != false, {
						for(0, order[i].size - 1, {|j| order[i][j] = order[i][j].split($ ); });
					});
				};
			},{
				if(order.asArray.size == 0, { order.add(false); });
				"no file found".postln;
			});
		});
		^order.asArray;
	}

	/* reads all .txt files in a dir and writes a single file in dir */
	/* optional - searches each /path/to/recording and only includes those that contain a specified string from 'string' */
	/* NOTE: 'string' can be a single string or an array of "strings" */
	groupTxt{|dir, string|
		var find = this.findTxt(dir), list = List.new, tmp, path, bool;
		var outputName, val = false;

		if(find != false and: { this.getNum(find) != false}, {
			for(0, find.size - 1, {|i|
				tmp = this.fileToArray(find[i]);
				if(tmp != false, {
					for(0, tmp.size - 1, {|j|
						path = tmp[j];
						if(path.includesEqual($ ), { path = tmp[j].split($ )[0]; });

						bool = string.isNil or: { this.checkString(PathName(path).fileName, string) };

						if(list.asArray.includesEqual(tmp[j]).not and: { bool }, { list.add(tmp[j]); });
					});
				});
			});

			outputName = dir ++ "group.order.txt";
			tmp = list.asArray.copy;
			list = nil; list = List.new;

			tmp.do{|file| list.add([file])};

			this.arrayToFile(list.asArray, outputName);
			val = true;
		}, { "files can't be grouped because they are not similarily formatted".postln; });
		^val;
	}

	/* reads an "order.txt" file and, given 'val', writes a new order to file */
	/* val changes order; 0 defaults to org. order */
	orderFromTxt{|dir, val = 0|
		var cond = Condition(false), prepare, contents, tmp = List.new, outputName, size;
		if(dir.isNil.not and: { dir.contains("order") }, {
			contents = this.fileToArray(PathName(dir).fullPath);
			prepare = this.prepare(dir);

			if(contents != false and: { prepare[0] }, {

				contents.size.do{|i| contents[i] = contents[i].split($ ); };
				size = contents[0].size;

				if(val >= size, { val = val % size; });

				for(0, contents.size - 1, {|i|
					for(0, contents[i].size - 1, {|j|
						if(j == val, { tmp.add(contents[i][j].asFloat)});
					});
				});

				//reorder
				contents = contents[tmp.asArray.order];

				//create name for output file
				tmp = PathName(dir).fileName.split($.);

				for(0, tmp.size - 3, {|i| outputName = outputName ++ tmp[i]; });
				outputName = PathName(dir).pathOnly ++ outputName ++ ".reorder.txt";

				this.arrayToFile(contents, outputName);

				cond.test = true;
				cond.signal;
			}, { "contains nil; see fileToArray class method".postln; });
		},{ "please enter *.order.txt file to re-order items for concatenations\nFor examples, see PVC_Pitch, etc. . .".postln; });
		^cond
	}

	//normalizes all recordings in a directory; specify extension (ext)
	norm{|dir, ext|
		var prepare = this.prepare(dir, ext);
		if(prepare[0], {
			prepare[3].size.do{|i| this.normalize(prepare[3][i]; )};
			"DONE: NORMALIZE".postln;
		});
	}

	/*either concatenates recordings by 1. reading a directory of audio files with same extension (optional: specify ext) */
	/* OR reads a .txt file and concats files */
	/* pre-concat normalization of audio files (norm) for both */
	cat{|dir, ext, norm|
		var outputName, prepare = this.prepare(dir, ext), paths, tmp;

		if(prepare[0], { //if dir with files that have ext
			//if txt file is in suitable format
			if(PathName(dir).isFile and: { PathName(dir).fileName.contains("order") }, {
				paths = this.fileToArray(PathName(dir).fullPath);
				if(paths != false, {
					paths.size.do{|i| paths[i] = paths[i].split($ )[0]; };
					outputName = PathName(dir).pathOnly ++ "concat." ++ PathName(paths[0]).extension.asString;
				}, { outputName = dir.withoutTrailingSlash ++ "/concat." ++ prepare[1].asString; });

				("CONCAT FILE PATH:  " ++ outputName).postln;
				this.concatFiles(outputName, prepare[2], prepare[3], norm);
			}, { "can't concat; either select a dir with suitable recordings or .txt file with paths".postln; });
		});
	}

	//remove files from dir that include string
	//BE CAREFUL - removes files from dir COMPLETELY
	remove{|dir, string|
		var files, tmp;
		if(PathName(dir).isFolder and: (string.isNil.not), {
			files = PathName(dir).files;
			files.size.do{|i|
				tmp = this.parse(files[i].fileName);
				if(tmp.includesEqual(string.asString), {
					("REMOVING: " ++ files[i].fullPath).postln;
					("rm " ++ files[i].fullPath).unixCmd;
				});
			};
			"DONE: REMOVE".postln;
		}, { "INCLUDE string TO REMOVE FILE(s)".postln; });
	}

	/* given an order.txt file, remove all lines that contain string*/
	removeFromTxt{|path, string|
		var file, list = List.new, t;
		if(PathName(path).extension == "txt", {
			file = File(PathName(path).fullPath, "r");
			while({ (t = file.getLine).notNil}, { if(t.split($ )[0].contains(string).not, { list.add(t); }); });
			file.close;
			("Removed all paths from: " ++ PathName(path).fullPath ++ "\t that contain: " ++ string).postln;
			list = list.asArray;
			file = File(PathName(path).fullPath, "w");
			for(0, list.size - 1, {|i| file.write(list[i] ++ "\n"); });
			("Wrote NEW file: " ++ PathName(path).fullPath ++ "\t that DOESN'T contain: " ++ string).postln;
			file.close;
		});
	}

	//••••••••••••••••••••••CLASS METHODS••••••••••••••••••••••••••••••••
	/*general method that checks if directory exists; files with extension exists*/
	//returns array with [true/false, soundfile extension, channel num of soundfiles, array of soundfiles]
	prepare{|dir, ext|
		var path = PathName(dir), fileFormat, file_array, check = Array.fill(4, false), val = 0;

		if(path.isFile, { //if dir is a .txt file
			file_array = this.fileToArray(path.fullPath);
			if(file_array != false, {
				file_array.do{|file, i|
					if(file.includesEqual($ ), { file_array[i] = file.split($ )[0]; });
				};
				val = 1;
			}, { "false".postln; });
		});

		if(path.isFolder, { //if dir is directory
			fileFormat = this.getFileFormat(path, ext);
			if((fileFormat == 0) or: { fileFormat.isNil }, {
				"ENTER FILE DIRECTORY WITH THE FOLLOWING EXTENSIONS: ".postln;
				format_array.size.do{|i| format_array[i].postln; };
			},{
				file_array = this.getFiles(path, fileFormat);
				if(file_array != false, { val = 1; });
			});
		});

		if(val == 1, {
			check = this.checkChannel(file_array);
			if(check[0] == false, {
				check[1][1].size.do{|i| ("" ++ check[1][1][i] ++ " has " ++ check[1][0][i] ++ " of channels").postln; };
			}, {
				if(fileFormat.isNil, { fileFormat = PathName(file_array[0]).extension; });
				check = [check[0], fileFormat, check[1], file_array];
			});

		}, { "Please enter a .txt file for ordering or a directory with recordings".postln; });

		^check;
	}

	//returns ext of recording or false
	getFileFormat{|dir, ext|
		var files = dir.files, format = Array.fill(4, 0), index, tmp;
		files.size.do{|i|
			tmp = this.parse(files[i].fileName);
			for(0, format_array.size - 1, {|j|
				if(tmp.includesEqual(format_array[j]), { format[j] = format[j] + 1; });
			});
		};

		if(ext.isNil.not and: format_array.includesEqual(ext), {
			for(0, format_array.size - 1, {|i|
				if(ext.asString == format_array[i], { index = i; });
			});
			if(format[index] != 0, { index = format_array[index]; }, { index = 0; });
		},{
			if(format.maxItem != 0, { index = format_array[format.indexOf(format.maxItem)]; });
		});
		^index;
	}

	//returns array of soundfiles from dir with extension
	getFiles{|dir, ext|
		var files = dir.files, array = List.new;
		files.size.do{|i|
			if(files[i].extension.asString == ext, { array.add(files[i].fullPath); });
		};
		^array.asArray;
	}

	//returns true/false if all recordings have the same # of channels
	checkChannel{|array|
		var sFile, masterSoundFileNumberOfChannels, chanTest = true, list = Array.fill(2, {|item| item = List.new; });
		array.size.do{|i|
			sFile = SoundFile.new;
			sFile.openRead(array[i]);

			if(i == 0, {
				masterSoundFileNumberOfChannels = sFile.numChannels;
				("NUMBER OF OUTPUT CHANNELS: " ++ masterSoundFileNumberOfChannels).postln;
			},{
				if(sFile.numChannels != masterSoundFileNumberOfChannels, {
					("************* ERROR **************").postln;
					("INCONSISTENT NUMBER OF CHANNELS: " ++ sFile.numChannels).postln;
					("---->   " ++  array[i]  ++ "    <----").postln;
					chanTest = false;
				});
			});

			list[0].add(sFile.numChannels); list[1].add(array[i]);

			sFile.close;
		};

		if(chanTest == false, {
			list.size.do{|i| list[i] = list[i].asArray; };
			masterSoundFileNumberOfChannels = list.copy;
		});

		^[chanTest, masterSoundFileNumberOfChannels]
	}

	//given an array of .txt files, make sure they are similarily formatted; INSTANCE METHOD for *groupFiles
	getNum{|array|
		var check = List.new, tmp, val = true;
		array.do{|file, i|
			tmp = this.fileToArray(file);
			if(tmp != false, {
				for(0, tmp.size - 1, {|j| check.add(tmp[j].split($ ).size); });
			});
		};
		check.asArray;

		if(check.sum / check.size != check[0], { tmp = false; }, { tmp = check[0]; });
		^tmp
	}

	//see groupTxt - needs to be cleaned up
	checkString{|path, array|
		var val = false;
		if(array.class == String, { array = [ array ]; });

		for(0, array.size - 1, {|i|
			if(path.contains(array[i]) or: { path.split($.).includesEqual(array[i])}, { val = true; });
		});

		^val
	}

	//returns path to normalized recording for *norm, *cat INSTANCE METHOD
	normalize{|path|
		var norm = PathName(path), sFile;
		norm = (norm.pathOnly ++ norm.fileNameWithoutExtension ++ ".norm." ++ norm.extension;);
		sFile = SoundFile.normalize(path, norm);
		("NORMALIZED FILE: " ++ norm.asString).postln;

		sFile.close;
		^norm
	}

	//NEED TO WRITE FILE TXT / BIN FILE THAT INCLUDES: path, start frame, duration - and anything else (all the freq info?)
	//concatenates recordings for *cat INSTANCE METHOD
	concatFiles{|name, chan, files, norm|
		var path, norm_path, sFile, sFileDataTemp, dataFile;
		var outSFile, masterSoundFile, masterSoundFileNumberOfFrames = 0;
		fork{

		dataFile = File(name ++ ".data.txt", "w");
		dataFile.write(chan.asString ++ "\n"); //write the number of channels
		dataFile.write(files.size.asString ++ "\n"); //write the # of recordings

		files.size.do{|i|
			path = files[i];
			dataFile.write(path.asString ++ " "); //but don't need the path - need the pitch - if avail!

			sFile = SoundFile.new;

			if(norm.isNil.not, {
				norm_path = this.normalize(path);
				sFile.openRead(norm_path);
			},{
				sFile.openRead(path);
			});

			dataFile.write(masterSoundFileNumberOfFrames.asString ++ " "); //write start frame
			masterSoundFileNumberOfFrames = (sFile.numFrames * sFile.numChannels) + masterSoundFileNumberOfFrames;

			dataFile.write((sFile.numFrames / sFile.sampleRate).asString ++ "\n"); //write duration of the sample

			sFileDataTemp = FloatArray.fill((sFile.numFrames * sFile.numChannels), {0.0});
			sFile.readData(sFileDataTemp);

			if(i == 0, { masterSoundFile = sFileDataTemp.deepCopy; },{ masterSoundFile = masterSoundFile ++ sFileDataTemp; });

			sFile.close;
		};

		outSFile = SoundFile.new.numChannels_(chan);
		outSFile.openWrite(name);
		outSFile.writeData(masterSoundFile);

		"DONE: CONCATENTATING".postln;
		outSFile.close;
		dataFile.close;
		};
	}

	//given a file name, reads line in file and stores into an array
	fileToArray{|file|
		var tmp = File(file, "r"), array = List.new, t;
		while({ (t = tmp.getLine).notNil}, { array.add(t); });
		array = array.asArray;
		tmp.close;

		if(array[0].isNil, { array = false; });
		^array
	}

	//given a 2D array - [# of items[item]] - and output name, writes array contents to file
	arrayToFile{|array, name|
		var file = File.new(name, "w");
		for(0, array.size - 1, {|i|
			for(0, array[i].size - 1, {|j|
				file.write("" ++ array[i][j].asString);
				if(j == (array[i].size - 1), { file.write("\n"); }, { file.write(" "); });
			});
		});
		file.close;
		("File: " ++ name ++ " has been written").postln;
	}

	//Class method for .remove INSTANCE METHOD
	parse{|item|
		^PathName(item).fileName.split($.);
	}

	//divide and sample -- HAVE NOT MADE THIS YET - next step 11.13.15
	div{}
}
