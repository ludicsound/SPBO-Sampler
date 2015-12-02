InstrBuf {
	var instrName;
	var bufDictionary;
	var >panMap;
	var <keys;

	*new { arg name, bufs;
		^super.new.init(name, bufs)
	}

	init { arg name, bufs;
		instrName = name;
		bufDictionary = bufs;
		keys = bufs.keys.asArray.asFloat.sort;
		this.setPanMap();
	}

	getBuf {arg pitchIndex;
		var buf = bufDictionary.at(keys[keys.indexIn(pitchIndex)].asSymbol);
		if(buf.isKindOf(SFRand), {^buf.next;}, {^buf;});
	}

	getLimitedBuf {arg pitchIndex, limit = 3;
		var buf = bufDictionary.at(keys[keys.indexIn(this.limitPitch(pitchIndex, limit))].asSymbol);
		if(buf.isKindOf(SFRand), {^buf.next;}, {^buf;});
	}

	getLimitedAll {arg pitchIndex, limit = 3;
		var buf = this.getLimitedBuf(pitchIndex, limit);
		var trans = this.getLimitedTransposition(pitchIndex, limit);
		^IdentityDictionary[
			\buf -> buf,
			\dur -> ((60.0 + trans).midicps/60.0.midicps * (buf.numFrames/buf.sampleRate)),
			\trans -> trans,
			\pan -> this.getPan(pitchIndex);
		];
	}

	getTransposition {arg pitchIndex;
		var pitchOfBuf;
		pitchOfBuf = keys[keys.indexIn(pitchIndex)];
		^(pitchOfBuf - pitchIndex).neg;
	}

	getLimitedTransposition { arg pitchIndex, limit = 3;
		var pitchOfBuf;

		pitchIndex = this.limitPitch(pitchIndex, limit);

		pitchOfBuf = keys[keys.indexIn(pitchIndex)];
		^(pitchOfBuf - pitchIndex).neg;
	}

	/*
	Get Transposition Factor for the Closest Harmonic Match, within the specified depth.

	Find the harmonic frequency match with the smallest transposition factor
	between the instrument dictionary and the given pitch, limited by
	the specified harmonic depth.  Default depth is the 5th harmonic.
	Returns and array in the format of:
	[Bufnum, Semitone_Transposition, Numbered_Harmonic]
	*/
	getTransposition_CH {arg pitch, harmDepth = 5;

		var keysIndex = keys.indexOfEqual(pitch);
		var returnCol, harm, trans;

		if(keysIndex == nil, {
			//initialize return col
			returnCol = [keys[keys.indexIn(pitch)], (keys[keys.indexIn(pitch)] - pitch).neg, 1];

			//iterate through the harmonics
			(harmDepth-1).do({arg i;
				harm = (i+2);
				keys.do({arg item, j;
					//get transposition factor of the partial in relation to the target pitch.
					trans = ((item.midicps * harm).cpsmidi - pitch).neg;
					//if the transposition factor is smaller than the  current factor, choose it.
					if(trans.abs < returnCol[1].abs, {
						returnCol = [item, trans, harm];
					});
				});
			});
		}, {
			returnCol = [pitch, 1, 0];
		});
		returnCol[0] = bufDictionary.at(returnCol[0].asSymbol);
		^returnCol;
	}

	/*
	Find the Lowest Harmonic Match to the given pitch, with a
	semi-tone transposition factor less than the specified value;	*/
 	getTransposition_LH {arg pitch, transThresh = 1.0;

		var keysIndex = keys.indexOfEqual(pitch);
		var returnCol, trans, harm = 2, done = false;

		if(keysIndex == nil, {
			//iterate through the harmonics
			while({done.not;}, {
				keys.do({arg item, j;
					//get transposition factor of the partial in relation to the target pitch.
					trans = ((item.midicps * harm).cpsmidi - pitch).neg;
					//if the transposition factor is smaller than the  current factor, choose it.
					if(trans.abs < transThresh, {
						returnCol = [item, trans, harm];
						done = true;
					});
				});
				harm = harm + 1;
			});
		}, {
			returnCol = [pitch, 1, 0];
		});
		returnCol[0] = bufDictionary.at(returnCol[0].asSymbol);
		^returnCol;
	}

	limitPitch {arg pitchIndex, limit = 3;
		while( { pitchIndex > (keys.last + limit) }, {
			pitchIndex = pitchIndex - 12;
		});

		while( { pitchIndex < (keys.first - limit) }, {
			pitchIndex = pitchIndex + 12;
		});

		^pitchIndex;
	}

	getLimitedPitch {arg pitchIndex, limit = 3;
		^keys[keys.indexIn(this.limitPitch(pitchIndex, limit))];
	}

	getPitch {arg pitchIndex;
		^keys[keys.indexIn(pitchIndex)];
	}

	getDuration {arg pitch;
		var buf = this.getBuf(pitch);
		^((60.0 + this.getTransposition(pitch)).midicps/60.0.midicps * (buf.numFrames/buf.sampleRate));
	}

	getLimitedDuration {arg pitch, limit = 3;
		var buf = this.getLimitedBuf(pitch, limit);
		^((60.0 + this.getLimitedTransposition(pitch, limit)).midicps/60.0.midicps * (buf.numFrames/buf.sampleRate));
	}

	interpolatePanMap {arg dict;
		var keys = dict.keys.asArray.sort;

		^Env.new(
			[dict[keys[0]]] ++ Array.fill(keys.size, {|n|
				dict[keys[n]];
			}),
			Array.fill(keys.size, {|n|
				keys.differentiate[n];
			})
		);
	}

	setPanMap {arg percentOfField = 1.0, offset = 0, direction = 1;
		var size = keys.size;
		var vals = Distribution.new.makeLinear(size, [(offset - 1), (offset - 1) + (2 * percentOfField)]);
		var panDict = Dictionary[];

		if(direction < 0, {vals = vals.reverse});

		keys.do({arg item, i;
			panDict.put(item.asFloat, vals[i]);
		});

		panMap = this.interpolatePanMap(panDict);
	}

	getPan {arg pitchIndex;
		^panMap[pitchIndex];
	}

	getName {
		^instrName;
	}

	getRange {
		^[keys.first, keys.last];
	}

	free {
		keys.do{arg item, n;
			bufDictionary.at(item.asSymbol).free;
		};
	}
}