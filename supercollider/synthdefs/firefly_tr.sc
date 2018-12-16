(
Geno.dspDo({ |id, ugen|

SynthDef('atom_firefly_tr_' ++ id, { |outbus = 0, amp = 0.5, freq = 50, t_trig = 1, gate = 1, releasetime = 1.2, xa = 0, xb = 0|
	var data, env, fenv;

	freq = Fold.ar(freq * 100, 6000, 20000);
	fenv = EnvGen.kr(Env.perc(0.001, releasetime), t_trig, TRand.kr(freq * -0.1, freq * 0.1, t_trig), freq);
	data = Impulse.ar(fenv);

//	freq = XLine.ar(freq, Rand(freq * 0.9, freq * 1.1), releasetime / 2);
//	data = Impulse.ar(freq);

	data = LeakDC.ar(data, 0.995);
//	data = data + CombN.ar(data, 0.1, 0.1, 1.0, 0.5);
	data = LPF.ar(data, 15000);

	env = EnvGen.kr(Env.perc(0.001, releasetime), t_trig) *
	      EnvGen.kr(Env.cutoff(releasetime), gate, doneAction: 2);
	data = data * env * amp * 0.08;

	data = SynthDef.wrap(ugen, nil, [ data, xa, xb ]);

	Out.ar(outbus, data);
}).store;

})
)