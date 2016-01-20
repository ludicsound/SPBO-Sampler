/*
fix formatOnsets method - group and remove duplicate segments:
A question of either take all audio - including silences - or only include "meaningful" onsets and their grouping. . .
*/

BenUtils{
	var server, format_array, markers;
	*new{ ^super.new.init }

	init{ //init global vars
		server = Server.default;
		format_array = ["wav", "aiff", "aif", "au", "AIFF", "WAV"];
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
			prepare = this.prepare(PathName(dir).fullPath);

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
				if(val == -1, { contents = contents.scramble; }, { contents = contents[tmp.asArray.order]; });

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

	/*concatenates recordings by either (1) reading a directory of audio files with same extension (optional: specify ext) */
	/* OR (2) reading a .txt file with the paths to files - in order */
	/* pre-concat normalization of audio files (norm) for both */
	cat{|dir, ext, norm|
		var outputName, prepare = this.prepare(dir, ext), paths, file_val;

		if(PathName(dir).isFile and: { PathName(dir).fileName.contains("order") }, {
			paths = this.fileToArray(PathName(dir).fullPath);
			if(paths != false, {
				paths.size.do{|i| paths[i] = paths[i].split($ ); };
				outputName = PathName(dir).pathOnly ++ "concat." ++ PathName(paths[0][0]).extension.asString;
				file_val = paths.copy;
			},
			{ outputName = dir.withoutTrailingSlash ++ "/concat." ++ prepare[1].asString; file_val = 0});
		});

		if(prepare[0] and: { file_val.isNil }, {
			outputName = dir.withoutTrailingSlash ++ "/concat." ++ prepare[1].asString;
			file_val = 0;
		});

		if(file_val.isNil.not, {
			("CONCAT FILE PATH:  " ++ outputName).postln;
			this.concatFiles(outputName, prepare[2], prepare[3], norm, file_val);
		}, { "can't concat; either select a dir with suitable recordings or .txt file with paths".postln; });
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

	//given a soundfile, divide it
	//hop_size is in frames
	div{|path, fft_size = 512, thresh = 0.5, hop_size = 5|
		var header = this.loadSndFile(path);
		if(header[0] != false, {
			fork{
				//this method defines global var markers
				this.findOnsets(header, fft_size, thresh).wait;
				markers.postln;

				this.formatOnsets(header, hop_size).wait;
				markers.postln;

				//this method uses header data and global var markers to write samples
				this.makeSamples(header).wait;
				"done".postln;
			};
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
		var files = dir.files, format = Array.fill(format_array.size, 0), index, tmp;
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
			array[i].postln;

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

	//concatenates recordings for *cat INSTANCE METHOD
	concatFiles{|name, chan, files, norm, file_val|
		var path, norm_path, sFile, sFileDataTemp, dataFile;
		var outSFile, masterSoundFile, masterSoundFileNumberOfFrames = 0;
		fork{

		dataFile = File(name ++ ".data.txt", "w");
		dataFile.write(chan.asString ++ "\n"); //write the number of channels
		dataFile.write(files.size.asString ++ "\n"); //write the # of recordings

		files.size.do{|i|
			path = files[i];
			dataFile.write(path.asString ++ " "); //write path/to/sndfile

			sFile = SoundFile.new;

			if(norm.isNil.not, {
				norm_path = this.normalize(path);
				sFile.openRead(norm_path);
			},{
				sFile.openRead(path);
			});

			dataFile.write(masterSoundFileNumberOfFrames.asString ++ " "); //write start frame
			masterSoundFileNumberOfFrames = (sFile.numFrames * sFile.numChannels) + masterSoundFileNumberOfFrames;

			dataFile.write((sFile.numFrames / sFile.sampleRate).asString); //write duration of sndFile

			//if concat from file, write remaining data to file
			if(file_val != 0 and: { file_val[i].size > 2 }, {
				dataFile.write(" ");
				for(2, file_val[i].size - 2, {|j| dataFile.write(file_val[i][j].asString ++ " "); });
				dataFile.write(file_val[i][file_val[i].size - 1].asString ++ "\n");
			}, { dataFile.write("\n"); });

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

	//class method for .div INSTANCE METHOD
	loadSndFile{|path|
		var sndFile = SoundFile.new, soundFileArray, val = Array.fill(7, { false });
		var i, filePeakAmp = (-99999999.0);

		if(sndFile.openRead(path), {
			soundFileArray = FloatArray.fill(sndFile.numFrames * sndFile.numChannels, {0.0});
			sndFile.readData(soundFileArray);

			val = [path, sndFile.numChannels, sndFile.numFrames, sndFile.sampleRate, sndFile.headerFormat, sndFile.sampleFormat, soundFileArray].copy;
		});

		sndFile.close;
		^val;
	}

	//class method for .div INSTANCE METHOD
	findOnsets{arg array, fft_size = 512, thresh = 0.5, cond = Condition.new(false);
		fork{
			var score, tmp_sndFile, duration, sampRate, c;
			var snd_path, osc_path, result_buf, size, data;

			tmp_sndFile = SoundFile.openRead(array[0]);
			sampRate = tmp_sndFile.sampleRate;
			duration = tmp_sndFile.duration;
			tmp_sndFile.close;

			snd_path = PathName.tmp +/+ UniqueID ++ ".aiff";
			osc_path = PathName.tmp +/+ UniqueID ++ ".osc";

			score = Score([
				[0, (result_buf = Buffer.new(server, 1000, 1, 0)).allocMsg],
				[0, [\d_recv, SynthDef(\onsets, {
						var sig, fft, trig, i, timer;
						sig = SoundIn.ar(0);
						fft = FFT(LocalBuf(fft_size, 1), sig);
						trig = Onsets.kr(fft, thresh);
						i = PulseCount.kr(trig);
						//timer = Sweep.ar(1); //count in sec
						timer = (Sweep.ar(1) * sampRate).round(1); //count in frames
						BufWr.ar(timer, result_buf, K2A.ar(i), loop: 0);
						BufWr.kr(i, result_buf, DC.kr(0), 0);
					}).asBytes]
				],
				[0, Synth.basicNew(\onsets, server, 1000).newMsg],
				[duration, result_buf.writeMsg(snd_path, headerFormat: "AIFF", sampleFormat: "float")]
			]);

			c = Condition.new;

			score.recordNRT(osc_path, "/dev/null", array[0], sampleRate: tmp_sndFile.sampleRate,
				options: ServerOptions.new
					.verbosity_(-1)
					.numInputBusChannels_(tmp_sndFile.numChannels)
				    .numOutputBusChannels_(tmp_sndFile.numChannels)
				    .sampleRate_(tmp_sndFile.sampleRate),
        		action: { c.unhang }
			);

    		c.hang;

			tmp_sndFile = SoundFile.openRead(snd_path);
			tmp_sndFile.readData(size = FloatArray.newClear(1));
			size = size[0];
			tmp_sndFile.readData(data = FloatArray.newClear(size));
			tmp_sndFile.close;

			File.delete(osc_path); File.delete(snd_path);

			markers = data.copy;

			cond.test = true;
			cond.signal;
		};
		^cond;
	}

	//because the onsets were only detected in the first channel, we only look at amps in first channel
	formatOnsets{arg array, hop_size, cond = Condition.new(false);
		var size, startEnd_list = List.new;
		fork{
			//# of frames
			("frames: " ++ array[2] ++ "\tdata_size: " + array[6].size).postln;
			("hop_size: " ++ hop_size).postln;

			//convert hope_size from ms to frames
			//size = ( (hop_size / 100) * array[3]).round(1);

			markers.size.do{|i|
				var avg, len, start, end;
				start = end = markers[i];

				avg = array[6][start].abs;
				while({ (start > 0) && (avg > -80.dbamp) }, {
					avg = 0;
					if( (start - hop_size) > 0, { len = hop_size; }, { len = start; });

					for(0, len - 1, {|j| avg = avg + array[6][start - j].abs; });

					avg = avg / len;
					if( (start - hop_size) > 0, { start = start - hop_size; }, { start = 0; });
				});

				avg = array[6][end].abs;
				while({ (end < (array[2] - 1)) && (avg > -80.dbamp) }, {
					avg = 0;
					if( (end + hop_size) < (array[2] - 1), { len = hop_size; }, { len = (array[2] - 1) - end; });

					for(0, len - 1, {|j| avg = avg + array[6][end + j].abs; });

					avg = avg / len;
					if( (end + hop_size) < (array[2] - 1), { end = end + hop_size; }, { end = array[2] - 1; });
				});

				if(start != end, { startEnd_list.add([start, end])});
			};

			markers = startEnd_list.asArray.copy;

			cond.test = true;
			cond.signal;
		};
		^cond;
	}

	makeSamples{arg array, cond = Condition.new(false);
		fork{
			var folder, startTimesFile, durationsFile, decibelsFile;

			//make dir for soundfiles - if needed: check if they are there
			folder = PathName(array[0]).pathOnly ++ "/sounds";
			if(PathName(folder).isFolder.not, { ("mkdir " ++ folder ).unixCmd; });

			//OPEN FILES FOR ASCII SAMPLE DATA
			startTimesFile = File(PathName(array[0]).pathOnly ++ "startTimes.txt", "w");
			durationsFile = File(PathName(array[0]).pathOnly ++ "durations.txt", "w");
			decibelsFile = File(PathName(array[0]).pathOnly ++ "decibels.txt", "w");

			//use markers
			markers.do{|startEndFrames, soundFileNumber|
				var sndFileArray, sampleNumberOfFrames, sampleDurationInSeconds;
				var peaks, attackLength, releaseLength;
				var newSoundFile, thisFileName;

				sndFileArray = Array.new( (startEndFrames[1] - startEndFrames[0]) * array[1]);
				sampleNumberOfFrames = startEndFrames[1] - startEndFrames[0] + 1;
				sampleDurationInSeconds = (sampleNumberOfFrames.asFloat / array[3].asFloat);

				//FIND PEAK AMP
				peaks = Array.fill(array[1], { -inf });
				for(startEndFrames[0], startEndFrames[1], {|frame|
					var amp;
					array[1].do{|chan|
						amp =  array[6][ (frame * array[1])  + chan];
						sndFileArray.add(amp);
						if(amp.abs > peaks[chan], { peaks[chan] = amp.abs; });
					};
				});

				attackLength = (array[1] * 0.005).round.asInteger;
				releaseLength = (array[1] * 0.005).round.asInteger;

				if( (attackLength + releaseLength) > sampleNumberOfFrames, {
					//SCALE ATTACK AND RELEASE TO AVAILABLE SAMPLE LENGTH
					attackLength = (sampleNumberOfFrames * 0.005 / (0.005 + releaseLength)).asInteger;
					releaseLength = sampleNumberOfFrames - attackLength;
				});

				//ATTACK
				for(0, attackLength, {|frame|
					var scaler =  frame / (attackLength - 1).asFloat ;
					array[1].do{|chan|
						sndFileArray[(frame * array[1]) + chan] = sndFileArray[(frame * array[1]) + chan] * scaler;
					};
				});

				//RELEASE
				for(0, releaseLength, {|frame|
					var scaler = frame / (releaseLength - 1).asFloat;
					array[1].do{|chan|
						var n = (((sndFileArray.size / array[1]).asInteger - 1 - frame) * array[1]) + chan;
						if(n < 0, { n = 0; });
						sndFileArray[n] = sndFileArray[n] * scaler;
					};
				});

				//PUT IN FLOAT ARRAY
				sndFileArray = FloatArray.fill(sndFileArray.size, {|k| sndFileArray[k]; });

				//ADD SILENCE
				if( (sampleNumberOfFrames.asFloat / array[3].asFloat ) < 0.0, {
					sndFileArray = sndFileArray ++ FloatArray.fill( ( (0.0 * array[3].asFloat).asInteger - (sndFileArray.size / array[1]) ) * array[1], { 0.0 });
				});

				//SAVE TO A FILE
				if(peaks.maxItem.ampdb != -inf, {
					newSoundFile = SoundFile.new.headerFormat_(array[4]).sampleFormat_(array[5]).numChannels_(array[1]);

					thisFileName = folder ++ "/" ++ PathName(array[0]).fileNameWithoutExtension ++ ".";

					if(soundFileNumber < 10, { thisFileName = thisFileName ++ "00"; });
					if(soundFileNumber < 100 and: {soundFileNumber >= 10}, { thisFileName = thisFileName ++ "0"; });

					thisFileName = thisFileName ++ soundFileNumber.asString ++ "." ++ array[4].asString;

					newSoundFile.openWrite(thisFileName);
					newSoundFile.writeData(sndFileArray);
					newSoundFile.close;

					//WRITE SOUND FILE SAMPLE START TIME TO FILE
					startTimesFile.write((startEndFrames[0].asFloat / array[3]).asString ++ "\n");
					//WRITE SOUND FILE SAMPLE DURATION
					durationsFile.write(sampleDurationInSeconds.asString ++ "\n");
					//WRITE SOUND FILE SAMPLE PEAK DECIBELS LEVEL
					decibelsFile.write(peaks.maxItem.ampdb.asString ++ "\n");
				});
			};

			startTimesFile.close;
			durationsFile.close;
			decibelsFile.close;

			cond.test = true;
			cond.signal;
		};
		^cond;
	}
}


