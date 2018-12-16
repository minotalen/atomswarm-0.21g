Geno {
	classvar dsp;
	
	*initClass {
		dsp = Dictionary[
			\00 -> { |in| in },
			\sq -> { |in, a, b| Squiz.ar(in,
								LinLin.kr(a, 0, 1, 1, 30),
								LinLin.kr(b, 0, 1, 1, 10))
					},
			\qq -> { |in, a, b| Squiz.ar(in,
								LinLin.kr(in, -1, 1, 1, 30),
								LinLin.kr(in, -1, 1, 1, 10))
							  * SinOsc.ar(LinExp.kr(a, 0, 1, 30, 5000))
					},
					
			\pi -> { |in, a, b| PitchShift.ar(in, 0.1,
								LinExp.kr(a, 0, 1, 0.25, 8.0),
								LinLin.kr(b, 0, 1, 0, 20))
					},
			\ri -> { |in, a, b| in * 
						SinOsc.ar(LinExp.kr(a, 0, 1, 30, 5000)).fold2(1 - b)
					},
			\de -> { |in, a, b| Degrader.ar(in,
						LinExp.kr(a, 0, 1, 5000, 20000),
						LinLin.kr(b, 0, 1, 5, 16)) * 0.6
					},
			\gr -> { |in, a, b|
				var shift = LinExp.kr(a, 0, 1, 0.1, 10);
				PitchShift.ar(PitchShift.ar(in, 0.1, shift.reciprocal), 0.1, shift)
			},
			\wl -> { |in, a, b|
				WaveLoss.ar(in, LinLin.kr(a, 0, 1, 0, 20), 20, 2)
			},
		];
	}

	*dspDo { |fn|
		var n = 0;
		dsp.keysValuesDo({ |name, ugen|
			fn.value(name, ugen);
			n = n + 1;
		});
	}
}
