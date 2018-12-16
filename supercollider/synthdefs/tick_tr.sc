(
Geno.dspDo({ |id, ugen|

SynthDef('atom_tick_tr_' ++ id , { |outbus = 0, amp = 0.5, t_trig = 1, gate = 1, dur = 0.0005, xa = 0, xb = 0|
	var out, env; // , fenv;

	out = Decay.ar(K2A.ar(t_trig), dur) * BrownNoise.ar(amp * 0.05);
	out = out * EnvGen.kr(Env.cutoff(dur), gate, doneAction: 2);
//	fenv = EnvGen.kr(Env([0, 1], [1], \exp), t_trig, 10000, 50, dur);
//	out = MoogFF.ar(out, fenv, 2);

	out = SynthDef.wrap(ugen, nil, [ out, xa, xb ]);

	Out.ar(outbus, out ! 2);
}).store;

});
)