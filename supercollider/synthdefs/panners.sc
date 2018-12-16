(
[2,4,5,6,8].do({ |c|

// 5 channel output: first channel is front-center

SynthDef('atompanner_1p' ++ c, { |inbus = 16, outbus = 0, pan = 0, panwidth = 2, amp = 1.0, gate = 1|
	var in, out, env, orientation;

	pan = Lag.kr(pan, 0.05);
	in = In.ar(inbus, 1);
	env = EnvGen.kr(Env.cutoff(5), gate: gate, doneAction: 2);
	orientation = if ((c % 2) == 0, 0.5, 0);

	out = in * amp;
	if (c == 2)
	   { out = Pan2.ar(out, pan, env) }
	   { out = PanAz.ar(c, out, pan, env, panwidth, orientation) };

	Out.ar(outbus, out);
}).store;

});
)