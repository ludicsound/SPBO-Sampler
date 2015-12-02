InstrumentLibrary {

	var m_path;
	var <>m_path_to_db;
	var m_sampleLibrary;
	var m_instrLibrary;
	var m_server;

	*new {arg server = Server.local, pyPath;
		^super.new.init(server, pyPath)
	}

	init {arg server, pyPath;
		m_path_to_db = pyPath.asString;
		m_instrLibrary = IdentityDictionary[];
		m_server = server;
		m_sampleLibrary = ("cd " + m_path_to_db + " && python instr_paths.py instruments").unixCmdGetStdOut.interpret;
	}

	makeInstr {arg buffer, cond, item, instr, instruments, index;
		"... making instrument".postln;
		if( buffer.numChannels == 1, {
			m_instrLibrary.add(item -> InstrBuf("instrMono." ++ item.asString, instr));
		}, {
			m_instrLibrary.add(item -> InstrBuf("instrStereo." ++ item.asString, instr));
		});
		if( index == (instruments.size - 1), {cond.test = true; cond.signal;});
		(" -> Finished Loading: " + item).postln;
	}

	load {arg instruments, cond = Condition.new(false);

		//get paths
		var py_path = m_path_to_db + " && python instr_paths.py ";

		instruments.do({arg item, index;

		var instr = ("cd " + py_path + " " + item.asString).unixCmdGetStdOut.interpret;

		if(instr.notNil, {
			//If loading the instrument... iterate through each of the pitchValueKeys and read sample path into buffer
			("Begin Loading: " + item + "...").postln;
			instr.keys.do({arg pitchKey, j;
				//load all the samples
				if(instr[pitchKey].isKindOf(String), {
					if(j == (instr.keys.size - 1), {
						instr[pitchKey] = Buffer.read(m_server, instr[pitchKey], action: {arg buffer; this.makeInstr(buffer, cond, item, instr, instruments, index);});
					}, {
						instr[pitchKey] = Buffer.read(m_server, instr[pitchKey]);
					});
				}, {
					if(j == (instr.keys.size - 1), {
						"... loading samples into a SFRand collection of buffers".postln;
						instr[pitchKey] = SFRand.new(Array.fill(instr[pitchKey].size,
						{|n|
							(n + " out of " + instr[pitchKey].size).postln;
							if(n == (instr[pitchKey].size - 1), {
								"... loading last sample".postln;
								Buffer.read(m_server, instr[pitchKey][n], action: {arg buffer; this.makeInstr(buffer, cond, item, instr, instruments, index);});
							}, {
								Buffer.read(m_server, instr[pitchKey][n]);
							});
						}));
					}, {
						instr[pitchKey] = SFRand.new(Array.fill(instr[pitchKey].size, {|n| Buffer.read(m_server, instr[pitchKey][n])}));
					});
				});
			});
		}, {
			("There are no samples associated with the the following instrument: " ++ item.asString).postln;
		});

		});

		^cond;
	}
	//Add an instrument(s) to the library by name
	add{arg instruments, cond = Condition.new(false);
		var col = [];
		instruments.do({arg item;
			if(m_instrLibrary.includesKey(item).not, {col = col.add(item);});
		});
		^this.load(col, cond);
	}
	//Free all buffers for the specified instruments. If no instruments are specified, free all instruments.
	free {arg unLoadList = m_instrLibrary.keys;
		unLoadList.do({arg item;
			m_instrLibrary[item].free;
			m_instrLibrary.removeAt(item);
		});
	}
	//get the library
	lib {
		^m_instrLibrary;
	}
	//get a list of instruments whose range encompasses the given pitch;
	instrWithinRange{arg pitch;
		var ranges = Array.fill(m_instrLibrary.size, {arg j;
			m_instrLibrary.keys.asArray[j].asArray ++ m_instrLibrary[m_instrLibrary.keys.asArray[j].asSymbol].getRange;
		});
		var col = Array.fill(ranges.size, {arg j;
			if( pitch >= ranges[j][1] and: {pitch <= ranges[j][2]}, {ranges[j][0];}, {0;});
		});
		col.removeEvery([0]);
		^col;
	}

	instrNames {
		^m_sampleLibrary;
	}
}