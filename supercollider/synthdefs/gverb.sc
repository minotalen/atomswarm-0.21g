(
[2,4,6,8].do({ |n|
	var name = 'atom_gverb_' + n;
	if (n != 2) { name = name ++ '_' ++ n };
	SynthDef(name, { |inbus = 0, outbus = 0, wet = 0.5, fade = 1.0, roomsize = 50, reverbtime = 1.0, damp = 0.995,
	                  inputbw = 0.5, spread = 0.1, earlyreflevel = 0.7, taillevel = 0.5, amp = 1.0|
		var in, out;

		wet = Lag.kr(wet, fade);
		wet = wet * 0.5;

		reverbtime = Lag.kr(reverbtime, fade) * 0.5;
		in = In.ar(inbus, 8) * amp;
//		out = GVerb.ar(in, roomsize, reverbtime, damp, inputbw, spread, 0, earlyreflevel, taillevel);
		out = in;
		out = (wet * out) + ((1.0 - wet) * in);

		Out.ar(outbus, out);
	}).store;
});
)