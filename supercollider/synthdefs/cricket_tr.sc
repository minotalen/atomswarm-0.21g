(
Geno.dspDo({ |id, ugen|

SynthDef('atom_cricket_tr_' ++ id, { |outbus = 0, amp = 0.5, freq = 1000, pan = 0, dur = 0.5, t_trig = 1, gate = 1, xa = 0, xb = 0|
	var out, env, delays;
	dur = Rand(0.1, 0.4);
	dur = dur + TRand.kr(-0.1, 0.1, t_trig);
	env = EnvGen.ar(Env([ 0, 1, 1, 0 ], [ 0.01, 1, 0.01 ]), t_trig, timeScale: dur);

//	out = Decay.ar(K2A.ar(t_trig), 0.1) * SinOsc.ar(TRand.kr(6000, 15000, t_trig));
	freq = LinLin.kr(freq, 40, 2000, 6000, 15000);
	out = Decay.ar(K2A.ar(t_trig), 0.1) * SinOsc.ar(freq);

	out = Lag.ar(out, 0.0005) * amp * 0.02;
	out = out * env;
	delays = Mix(Array.fill(5, { CombL.ar(out, 0.1, LFDNoise1.kr(1.0.rand, 0.04, 0.05)) }));
	out = out + (delays * 0.5);
//	DetectSilence.ar(out, doneAction: 2);

//	out = Squiz.ar(out, 1 + squiz, 2 + squiz);

	out = SynthDef.wrap(ugen, nil, [ out, xa, xb ]);

	out = out * EnvGen.kr(Env.cutoff(1), gate, doneAction: 2);
	Out.ar(outbus, out);
}).store;

});
)