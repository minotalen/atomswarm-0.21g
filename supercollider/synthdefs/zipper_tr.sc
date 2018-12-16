(
Geno.dspDo({ |id, ugen|

SynthDef('atom_zipper_tr_' ++ id, { |outbus = 0, amp = 0.5, dur = 0.4, freq = 500, t_trig = 1, gate = 1, xa = 0, xb = 0|
	var data, cfreq, env;

	env = EnvGen.kr(Env([0, 1, 1, 0], [0, dur, 0]), t_trig) *
	      EnvGen.kr(Env([0, 1, 1, 0], [0, dur, 0], 0, 2), gate, doneAction: 2);

	cfreq = Pulse.ar(LFSaw.ar(TExpRand.kr(2, 8, t_trig)) * 110); // + LFNoise1.kr(1, 40, 50));

	freq = Fold.kr(freq * 100, 8000, 15000);
	data = BPF.ar(cfreq, freq, Rand(0.005, 0.03), mul: 5 * amp);
	data = data * env;

	data = SynthDef.wrap(ugen, nil, [ data, xa, xb ]);

	Out.ar(outbus, data);
}).store;

})
)