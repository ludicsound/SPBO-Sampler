/* 	Runs PVCplus pitchtracker SCRIPT and makes a .txt file that includes:
	(spaces between items; new lines between files):

	1. path/to/file (string)
	2. fund. freq (int)
	3. mean freq of "related" frequencies (float)
	4. mean freq from PVC (float)
	5. lowest freq (float) highest freq (float)
*/

PVC_Pitch{
	var file, commands, soundFiles, numChannels, pitchTrackFiles, cond;

	*new{|path, fft, ref, low, high| ^super.new.init(path, fft, ref, low, high) }

	init{|path, fft, ref, low, high|
		var tmp, t;

		//init global vars
		soundFiles = List.new;
		numChannels = List.new;
		commands = "";
		cond = Condition(false);

		if(PathName(path).isFile and: { PathName(path).fullPath.contains("order") }, {
			tmp = File(PathName(path).fullPath, "r");
			while({ (t = tmp.getLine).notNil }, {
				t = t.split($ );
				//just get the path/to/sndfile and # of channels
				soundFiles.add(t[0]); numChannels.add(t[1]);
			});
			tmp.close;
		});

		//print files available for pitchtracking
		soundFiles.do({|file| (" SOUND FILE: " ++ file).postln; });

		//check that there is at least one file to pt
		if(soundFiles.size > 0, {
			//init global vars
			pitchTrackFiles = soundFiles.copy;

			pitchTrackFiles.do({|file, i|
				pitchTrackFiles[i] = file.replace("soundFiles", "pitchTracks").replace("." ++ PathName(soundFiles[0]).extension.asString,".pt");
			});

			//define args - if needed
			if(fft.isNil, { fft = 1024; });
			if(ref.isNil, { ref = 1000; });
			if(low.isNil, { low = 40.0 });
			if(high.isNil, { high = 16000.0; });

			fork{
				this.pitchtracker(iterations: soundFiles.size, fft_length: fft, ref_freq: ref, low_freq: low, high_freq: high);
				this.runCommands;
				this.checkFileExists(pitchTrackFiles[pitchTrackFiles.size - 1]).wait;
				this.readFiles;
			};

			"..........DONE!".postln;
		}, { "NO USABLE FILES\nTRY CHANGING THE EXTENSION OR DIRECTORY".postln; });
		^cond
	}

	findExtension{|files|
		var format_array = ["wav", "aiff", "aif", "au"], ext, num = Array.fill(4, 0);
		files.do{|file, i|
			ext = PathName(file).extension;
			for(0, format_array.size - 1, {|j|
				if(ext == format_array[j], { num[j] = num[j] + 1; });
			});
		};
		ext = format_array[num.indexOf(num.maxItem)];
		^ext;
	}

	addCommand{|command|
		command = command.replace( "\n", " " ).replace( "\t", " ");
		commands = commands ++ command ++ " \n";
	}

	runCommands{
		var homeDirectory = "HOME".getenv;
		commands = commands.replace("~/", homeDirectory ++ "/");
		commands.replace("\n", " ;\n\n" ).postln;
		commands.runInTerminal;
	}

	pitchtracker{|iterations = 1, fft_length = 1024, ref_freq = 1000, low_freq = 40, high_freq = 16000|
		var setRoutineVariables;
		var pitch_trajectory_output_file, plot_output__1_yes__0_no;
		var output_units, ouput_trajectory_sample_rate, output_trajectory_data_type;
		var sndfile, begintime, endtime, analysis_frames_per_second;
		var windowsize, analysis_channel_1_to_max, method_on_multiple_channels__average_0__peak_1, detection_method__0_1_or_2;
		var mode_filter_window_size_in_seconds;
		var detection_window_size_minimum, detection_window_size_maximum, detection_threshold_in_decibels;
		var frequency_response_time, amplitude_weighted_oversampling_factor;
		var release_time_in_seconds, attack_time_in_seconds;
		var compression_threshold_in_decibels, decibels_of_compression;
		var gate_threshold_in_decibels, amplitude_envelope_warp;

		/* the following line of commented out vars have been turned into args, which can be specified when PVC_Pitch is init
		var fft_length, reference_frequency_or_pitch (ref_freq);
		var low_freq_or_pitch_boundary (low_freq), high_freq_or_pitch_boundary (high_freq);
		*/

		setRoutineVariables = {|iteration = 0|
			//******************** OUTPUT **************************
			pitch_trajectory_output_file = pitchTrackFiles[iteration];
			plot_output__1_yes__0_no = 0;

			//************ OUTPUT SETTINGS ************************
			//......... OUTPUT UNITS ............................
			output_units = 0 ;
			// 0: freq
			// 1: octdec
			// 2: semitone deviation from reference
			// 3: inverted semitones of deviation from reference
			// 4: MIDI
			// 5: octave.pitchclass

			//.........REFERENCE FREQUENCY ............................
			//reference_frequency_or_pitch = 1000;
			//
			// for use with output formats 2 or 3 -- see above
			// Values less than 12 are treated as octave.pitchclass.
			// Function file of changing frequencies may be used with
			// inverted semitones of deviation to produce correction
			// files that impose reference frequency tracking onto sound.

			//........OUTPUT SAMPLE RATE ............................
			ouput_trajectory_sample_rate = 2000 ;
			// Must be equal to or greater than analysis frame rate.

			//........OUTPUT DATA TYPE ............................
			output_trajectory_data_type = 1;
			//( 0: ascii )
			//( 1: 32-bit floats )

			//******************** INPUT ***************************
			sndfile = soundFiles[iteration];

			//........ BEGIN/END TIMES .............................
			begintime = 0.0;
			endtime = -1;
			analysis_frames_per_second = 500;

			//======================================================
			//*** ANALYSIS PARAMETERS ******************************
			//fft_length = 1024;
			windowsize = 0;

			//*************** CHANNELS ****************************
			analysis_channel_1_to_max = 1;
			method_on_multiple_channels__average_0__peak_1 = 0;

			//======================================================
			//*********** DETECTION PARAMETERS *********************
			//======================================================
			//......... DETECTION METHOD ...........................
			detection_method__0_1_or_2 = 0;
			// 0 = the strongest formant, or the lowest harmonic
			//	 fundamental of it, detected within the frequency
			//	 and amplitude limits.
			// 1 = the strongest formant
			// 2 = band-limited centroid
			//	 Takes the amplitude (squared) weighted average of
			//	 the spectrum between the low-high boundaries.
			//******************************************************

			//......... DETECTION BOUNDARIES .......................
			//low_freq_or_pitch_boundary = 40 ;
			//high_freq_or_pitch_boundary = 16000 ;
			//Values less than 12 are treated as octave.pitchclass.

			// . . . . . MODE FILTER: FIRST PASS . . .
			mode_filter_window_size_in_seconds = 0.01;
			//
			// To limit non-uniform spikes, set time window
			// to a duration large enough to invoke a common value
			// region that will suppress deviant values or departures
			// from the current norm.

			//......... DETECTION TIME WINDOW -- SECOND PASS
			detection_window_size_minimum = 0.01;
			detection_window_size_maximum = 0.01;
			// (Keep range between .05 and .2, "I guess".....)
			// Functions like a mode filter--see above.

			//......... DETECTION THRESHOLD .......................
			detection_threshold_in_decibels = -30;
			//( -10 to -20, perhaps? ) ;

			//****** FREQUENCY TRAJECTORY ***********************
			//......... PITCH TRAJECTORY SMOOTHING .................
			frequency_response_time = 0.1;
			// Lowpass filter that smooths the changes to the tracked
			// pitch trajectory; larger values increase smoothness.
			// 0 == off.

			//........ AMPLITUDE WEIGHTED RESAMPLING ............
			amplitude_weighted_oversampling_factor = 0;
			//
			// Each value in the pitch trajectory will be over-sampled
			// up to an amplitude-determined proportion of the specified
			// amount. For use in identifying the strongest median
			// frequency. For normal pitch tracking, set to 0, which
			// will prevent oversampling.

			//***** AMPLITUDE ENVELOPE **************************
			//........RESPONSE TIME ............................
			release_time_in_seconds = 0.0;
			attack_time_in_seconds = 0.0;

			//........ COMPRESSOR ...............................
			compression_threshold_in_decibels = -0;
			decibels_of_compression = -0;

			//........GATE .....................................
			gate_threshold_in_decibels = -80;

			//........WARP .......................................
			amplitude_envelope_warp = 0 ;
			//====================================================
			//••••••••••••••••••••••••••••••••••••••••••••••••••••••
		};

		iterations.do({|n|
			setRoutineVariables.value(iteration: n);

			// ******** UNIX COMMAND LINE 0 ***********
			this.addCommand(
				"pitchtracker" ++ " " ++

				// * FLAGS *
				" -P" ++ plot_output__1_yes__0_no ++  " -g" ++ output_trajectory_data_type ++
				" -f" ++ low_freq ++  " -D" ++ analysis_frames_per_second ++
				" -C" ++ analysis_channel_1_to_max ++  " -m" ++ detection_method__0_1_or_2 ++
				" -j" ++ detection_window_size_minimum ++  " -d" ++ detection_threshold_in_decibels ++
				" -a" ++ frequency_response_time ++  " -M" ++ windowsize ++  " -o" ++ ref_freq ++
				" -F" ++ high_freq ++  " -G" ++ decibels_of_compression ++
				" -N" ++ fft_length ++  " -J" ++ detection_window_size_maximum ++  " -O" ++ output_units ++
				" -L" ++ release_time_in_seconds ++  " -T" ++ compression_threshold_in_decibels ++
				" -l" ++ attack_time_in_seconds ++  " -H" ++ mode_filter_window_size_in_seconds ++
				" -r" ++ ouput_trajectory_sample_rate ++  " -W" ++ amplitude_envelope_warp ++
				" -X" ++ method_on_multiple_channels__average_0__peak_1 ++  " -S" ++ gate_threshold_in_decibels ++
				" -b" ++ begintime ++  " -p1" ++  " -E" ++ amplitude_weighted_oversampling_factor ++
				" -e" ++ endtime ++

				// * INPUT/OUTPUT FILES *
				" "  ++ sndfile ++ "  " ++  " "  ++ pitch_trajectory_output_file
			);
		});
	}

	read{|file|
		var tmp = File(file, "rb"), fileLength = tmp.length / 4, array;
		array = FloatArray.fill(fileLength, {|i| 0.0 });
		tmp.readLE(array);
		tmp.close;
		^array;
	}

	findFund{|array|
		var fund, freq = List.new, mode = List.new, tmp, index;

		for(0, array.size - 1, {|i|
			tmp = array[i].round(1);
			if(freq.asArray.includesEqual(tmp), {
				index = freq.asArray.indexOf(tmp);
				mode.put(index, mode.asArray[index] + 1);
			}, {
				freq.add(tmp); mode.add(1);
			});
		});

		mode.asArray; freq.asArray;
		fund = freq[mode.indexOf(mode.maxItem)];
		freq = List.new;

		for(0, array.size - 1, {|i|
			if(array[i].cpsmidi.round(1) == fund.cpsmidi.round(1), { freq.add(array[i]); });
		});

		^[fund, freq.mean]
	}

	checkFileExists{arg file, cond = Condition.new(false);
		var unixCmd = "[[ -s " ++ file ++ " ]] && echo 1 || echo 0";

		fork{
			"waiting".postln;
			block{|break|
				loop{
					if( unixCmd.unixCmdGetStdOut.asInteger == 1, { break.value; });
					1.wait;
				};
			};
			cond.test = true;
			cond.signal;
		};
		^cond;
	}

	//read pitch track files and write to file - (if needed) makes dir to store order file
	readFiles{
		var test = 0, tmp, fund, order;

		//make .txt file to write to
		order = PathName(soundFiles[0]).pathOnly ++ "/pitch.order.txt";

		file = File(order, "w");

		pitchTrackFiles.size.do{|n|
			tmp = this.read(pitchTrackFiles[n]);
			fund = this.findFund(tmp);
			if(fund[0] != 0, {
				file.write("" ++ soundFiles[n] ++ " " ++ numChannels[n] ++ " " ++ fund[0] ++ " " ++ fund[1] ++ " " ++ tmp.mean ++ " " ++ tmp.minItem ++ " " ++ tmp.maxItem ++ "\n");
			});
		};

		file.close;
		"Done reading files\nWrote results to File".postln;
	}
}
