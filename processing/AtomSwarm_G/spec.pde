/*
 * spec: Support for mapping parameters based on data specs,
 *       derived from similar functions in SuperCollider.
 *
 * Requires util functions from same package.
 *
 * NOTE that map_init() must be called before attempting to access
 * global named Spec objects.
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 * 
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/

import java.util.Hashtable;

final int MAP_LIN = 0,
          MAP_EXP = 1,
          MAP_AMP = 2;  // squared

Hashtable map_index = new Hashtable(128);





/*-------------------------------------------------------------------
 * map: Maps a value across a predefined spec range
 *-------------------------------------------------------------------*/
float map (String map_name, float value)
{
  if (map_index.containsKey(map_name))
  {
    Spec spec = (Spec) map_index.get(map_name);
    return spec.map(value);
  }
  else
  {
    return 0;
  }
}

/*-------------------------------------------------------------------
 * linlin: Map linear range onto linear range
 *-------------------------------------------------------------------*/
float linlin (float x, float a, float b, float c, float d)
{
  if (x <= a) return c;
  if (x >= b) return d;
  return (x - a) / (b - a) * (d - c) + c;
}

/*-------------------------------------------------------------------
 * linexp: Map linear range onto exponential range
 *-------------------------------------------------------------------*/
float linexp (float x, float a, float b, float c, float d)
{
  if (x <= a) return c;
  if (x >= b) return d;
  return pow(d/c, (x-a)/(b-a)) * c;
}

/*-------------------------------------------------------------------
 * explin: Map exponential range onto linear range
 *-------------------------------------------------------------------*/
float explin (float x, float a, float b, float c, float d)
{
  if (x <= a) return c;
  if (x >= b) return d;
  return (log(x / a)) / (log(b / a)) * (d - c) + c;
}

/*-------------------------------------------------------------------
 * expexp: Map exponential range onto exponential range
 *-------------------------------------------------------------------*/
float expexp (float x, float a, float b, float c, float d)
{
  if (x <= a) return c;
  if (x >= b) return d;
  return pow(d / c, log(x / a) / log(b / a)) * c;
}

/*-------------------------------------------------------------------
 * map1: Linear map from [0..1] to [low..high]
 *-------------------------------------------------------------------*/
float map1 (float value, float low, float high)
{
  return low + value * (high - low);
}

/*-------------------------------------------------------------------
 * map_register: Registers a named Spec in the global hashtable,
 *               which can be subsequently used with new Spec("name");
 *-------------------------------------------------------------------*/
void map_register (String map_name, float v_min, float v_max, int v_type)
{
  map_index.put(map_name, new Spec(v_min, v_max, v_type));
}


/*-------------------------------------------------------------------
 * map_init: Populates global spec table with default specs.
 *-------------------------------------------------------------------*/
void map_init ()
{
  map_index.put("unipolar", new Spec("unipolar", 0, 1, MAP_LIN));
  map_index.put("freq",     new Spec("freq", 20, 20000, MAP_EXP));
  map_index.put("amp",      new Spec("amp", 0, 1, MAP_AMP));
  map_index.put("wet",      new Spec("wet", 0, 1, MAP_LIN));
  map_index.put("pan",      new Spec("pan", -1, 1, MAP_LIN));
}

/*-------------------------------------------------------------------
 * spec: Returns the global spec object associated with a given name
 *       (analogous to SC's \amp.asSpec)
 *-------------------------------------------------------------------*/
Spec spec (String name)
{
  return (Spec) map_index.get(name);
}


/*-------------------------------------------------------------------
 * class Spec: Analogous to SC's ControlSpec class.
 *-------------------------------------------------------------------*/
class Spec
{
  String name;
  float min,
        max;
  int type;

  
  Spec(float v_min, float v_max, int v_type)
  {
    name = "";
    min = v_min;
    max = v_max;
    type = v_type;
  }

  Spec(String v_name, float v_min, float v_max, int v_type)
  {
    name = v_name;
    min = v_min;
    max = v_max;
    type = v_type;
  }
  
  Spec (String v_name)
  {
    Spec peer = (Spec) map_index.get(v_name);
    if (peer != null)
    {
      this.name = peer.name;
      this.min = peer.min;
      this.max = peer.max;
      this.type = peer.type;
    }
    else
    {
      println("** Specification not found: " + v_name);
    }
  }
  
  float map (float value)
  {
    value = clip(value, 0.0, 1.0);
    float mapped = 0.0;
    switch (type)
    {
      case MAP_LIN: mapped = min + ((max - min) * value); break;
      case MAP_EXP: mapped = min * pow(max / min, value); break;
      case MAP_AMP: mapped = min + ((max - min) * sq(value)); break;
    }
    
    return mapped;
  }
  
  float unmap (float value)
  {
    float unmapped = 0.0;
    switch (type)
    {
      case MAP_LIN: unmapped = (value - min) / (max - min); break;
      case MAP_EXP: unmapped = log(value / min) / log(max / min); break;
      case MAP_AMP: unmapped = (value - min) / (max - min);
              if (unmapped > 0) unmapped = sqrt(unmapped);
              break;
    }
    
    return clip(unmapped, 0.0, 1.0);
  }
}
