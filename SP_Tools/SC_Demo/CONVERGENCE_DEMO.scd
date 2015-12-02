//CONVERGENCE + SAMPLER DEMO

//STEP 1
//A. start the server.
s.boot;
//B. Drag and Drop the /Python directory into the object constructor to pass the path to the SC class.
~il = InstrumentLibrary.new(s, "/Users/Sean/Software/PEUQUET_TechnicalProject_CCT_TechDir/Python");
//C. load samples (wait for them to finish in the post window)
(
Routine.new{
	~il.load([\STEINWAYGRANDPIANO]).wait;
	~il.load([\CROTALES]).wait;
	~instr = ~il.lib[\STEINWAYGRANDPIANO];
}.play(SystemClock);
)

//STEP 2
//pass two synthdefs to the server.
(
//generic "simple" playback
SynthDef("simple", { | bufnum |
	var sig = PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum), doneAction: 2);
	Out.ar(0, sig);
}).add;

//just like "simple", but with amp & panning control
SynthDef("instrStereo", { |bufnum, amp = 1.0, pan = 0.0, dbOffset = 0|
	var sig = (dbOffset.dbamp * amp) * PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum), doneAction: 2);
	sig = Pan2.ar(sig, pan, 1);
	Out.ar(0, sig);
}, variants:
	(
	STEINWAYGRANDPIANO: [dbOffset: -3]
	)
).add;

//mono version for crotales
SynthDef("instrMono", { |bufnum, amp = 1.0, pan = 0.0, dbOffset = 0|
	var sig = (dbOffset.dbamp * amp) * PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum), doneAction: 2);
	sig = Pan2.ar([sig, sig], pan, 1);
	Out.ar(0, sig);
}, variants:
	(
	CROTALES: [dbOffset: -3]
	)
).add;
)

//STEP 3
//play a piano note: middle C, sensitive to microtonal pitch deviation, limited transposition within a range of + or - 3 semitones.
a = Synth("simple", [\bufnum, ~instr.getLimitedBuf(60, 3)]);

//STEP 4
//Do something more advanced, i.e. play all the notes in sequence running from low to high.
(
r = Routine.new({
	88.do{arg pitch;
		pitch = pitch + 21;
		a = Synth(~instr.getName(), [
			\bufnum, ~instr.getLimitedBuf(pitch, 3),
			\amp, 0.7,
			\pan, ~instr.getPan(pitch); //get interpolated pan value(moving left to right)for the given pitch index
			]
		);
		(0.15).wait;
	};
});
r.play(SystemClock);
)

//STEP 5
//Use a ConvergentArray to control pitch and amp.
(
var pitch, amp;
var cp = ConvergentArray((84..96).collect{|x| x}, steps: 10, flag: 1, quantLevel: 0, target: 'choose');
var ca = ConvergentArray(Array.fill(25, {|x| rrand(0.01, 1.0)}), steps: 10, flag: 1, quantLevel: 0, target: 'choose');
~instr=~il.lib[\STEINWAYGRANDPIANO];
r = Routine.new({
	while(true, {
		pitch = cp.next;
		amp = ca.next;
		("pitch: " + pitch + " / amp: " + amp).postln;
		a = Synth(~instr.getName(), [
			\bufnum, ~instr.getLimitedBuf(pitch, 3),
			\amp, amp,
			\pan, ~instr.getPan(pitch); //get interpolated pan value(moving left to right)for the given pitch index
			]
		);
		(0.15).wait;

		//Check if the ConvergentArrays have hit an extreme and then flip direction and switch instruments
		if(cp.converged, {cp.diverge_; ca.diverge_; ~instr=~il.lib[~il.instrNames[0]]});
		if(cp.diverged, {cp.converge_; ca.converge_; ~instr=~il.lib[~il.instrNames[1]]});
	};
	);
});
r.play(SystemClock);
)
