(
Geno.dspDo({ |id, ugen|

SynthDef('atom_throb_tr_' ++ id, { |outbus = 0, amp = 0.5, freq = 50, width = 0.5, t_trig = 1, gate = 1, xa = 0, xb = 0|
	var data, env;

	data = Mix.fill(5, { |n| SinOsc.ar(freq * Rand(1, n * 2), 0, 0.5 / (n + 1)) });
//	data = Mix(SinOsc.ar([ freq, freq * 2, freq * 7, freq * 5 ], 0, 0.2));
//	data = OnePole.ar(SinOsc.ar(freq).cubed, 0.9);
	env = EnvGen.kr(Env.perc(0.1, 0.2, 1, \sine), t_trig) *
	      EnvGen.kr(Env.cutoff(2), gate, doneAction: 2);
	data = MoogFF.ar(data, LinExp.kr(env, 0, 1, Rand(25, 60), 800), 1.8) * amp * env * 0.7;

	data = SynthDef.wrap(ugen, nil, [ data, xa, xb ]);

	Out.ar(outbus, data);
}).store;

});
)