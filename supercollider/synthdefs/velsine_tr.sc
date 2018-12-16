(
Geno.dspDo({ |id, ugen|

SynthDef('atom_velsine_tr_' ++ id, { |outbus = 0, amp = 0.5, freq = 110, t_trig = 0, gate = 1, pan = 0, xa = 0, xb = 0|
	var out, env, on;

	freq = Lag.kr(freq, 0.01);
	on = ToggleFF.kr(t_trig);
	freq = freq * EnvGen.kr(Env.cutoff(0.3), gate: gate, doneAction: 2);
	env = EnvGen.kr(Env.adsr(1.0, 0.8, 1.0, 3.0), gate: on) * EnvGen.kr(Env.cutoff(0.3), gate: gate);

	out = SinOsc.ar(freq) * env * 0.1 * amp;

//	squiz = Lag.kr(squiz, 0.05);
//	out = Squiz.ar(out, 1 + squiz, 2 + squiz);

	out = SynthDef.wrap(ugen, nil, [ out, xa, xb ]);

	Out.ar(outbus, out);
}).store;

});
)