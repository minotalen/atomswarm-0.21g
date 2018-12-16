(
Geno.dspDo({ |id, ugen|

SynthDef('atom_bump_tr_' ++ id, { |outbus = 0, amp = 0.5, freq = 300, t_trig = 1, gate = 1, pan = 0, xa = 0, xb = 0|
	var out, env;

	freq = Lag.kr(freq, 0.02);
	freq = freq * EnvGen.kr(Env.perc(0.001, 0.3), t_trig);
	out = SinOsc.ar(freq) * amp * 0.1;
//	out = OnePole.ar(Pulse.ar(freq), 0.99) * amp * 0.1;

	env = EnvGen.kr(Env.perc(0.1, 0.3), t_trig) *
	      EnvGen.kr(Env.cutoff(0.3), gate, doneAction: 2);
	out = out * env;

	out = SynthDef.wrap(ugen, nil, [ out, xa, xb ]);
	Out.ar(outbus, Pan2.ar(out, pan));
}).store;

})
)