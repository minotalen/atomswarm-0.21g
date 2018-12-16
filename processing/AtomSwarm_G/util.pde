/*
 * Utility functions, some derived from SuperCollider, including
 *  - converting MIDI note values to cps
 *  - random number generation with nonuniform distribution 
 *  - value clipping
 *  - mapping between value ranges (linear and exponential)
 *  - support for Spec objects, including a global named Spec register
 *
 * Many thanks to James McCartney for many of the functions that 
 * these were ported from - see http://www.audiosynth.com/.
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 * 
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/



/*-------------------------------------------------------------------
 * midicps: Map from MIDI note to frequency
 *-------------------------------------------------------------------*/
float midicps (float note)
{
  return 440.0 * pow(2, (note - 69) / 12);
}

/*-------------------------------------------------------------------
 * cpsmidi: Map from frequency to MIDI note
 *-------------------------------------------------------------------*/
float cpsmidi (float freq)
{
  return (log(freq / 440.0) / log(2.0)) * 12 + 69;
}





/*-------------------------------------------------------------------
 * random2: Generate uniformly random number between [-limit..limit]
 *-------------------------------------------------------------------*/
float random2 (float limit)
{
  return random(limit * 2) - limit;
}

/*-------------------------------------------------------------------
 * linrand: Random number up to limit with linear distribution
 *-------------------------------------------------------------------*/
float linrand (float limit)
{
  return min(random(limit), random(limit));
}


/*-------------------------------------------------------------------
 * bilinrand: Random number up to limit with bilinear distribution
 *-------------------------------------------------------------------*/
float bilinrand (float limit)
{
  return (random(1) - random(1)) * limit;
}




/*-------------------------------------------------------------------
 * clip: Clips value between [v_min, v_max]
 *-------------------------------------------------------------------*/
float clip (float value, float v_min, float v_max)
{
 if (value < v_min)
    return v_min;
 else if (value > v_max)
    return v_max;
 else 
    return value;
}

/*-------------------------------------------------------------------
 * clip1: Clips value between [0..1]
 *-------------------------------------------------------------------*/
float clip1 (float value)
{
  if (value < 0)
    return 0;
  else if (value >= 1)
    return 1;
  else
    return value;
}

/*-------------------------------------------------------------------
 * clip2: Clips value between [-1..1]
 *-------------------------------------------------------------------*/
float clip2 (float value)
{
  if (value < -1)
    return -1;
  else if (value > 1)
    return 1;
  else
    return value;
}

/*-------------------------------------------------------------------
 * clip2: Clips value between [-limit..limit]
 *-------------------------------------------------------------------*/
float clip2 (float value, float limit)
{
  if (value < -limit)
    return -limit;
  else if (value > limit)
    return limit;
  else
    return value;
}
