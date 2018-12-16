/*
 * AtomClasses: Subclasses of Atom adding further functions.
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 * 
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/
 
import java.util.Set;


/*
 * AtomListener: Singleton object for spatial positioning.
 *-------------------------------------------------------------------*/

class AtomListener extends AtomGenetic
{
  AtomListener (AtomSwarmGenetic swarm)
  {
    super(swarm);

    // Reset listener to neutral genome
    for (int i = 0; i < G_GENE_COUNT; i++)
      gene[i] = 0;
    
    name = "listener";
    col = color(0, 255, 180);
    
    x = width / 2;
    y = height / 2;
  }
  
  void move()
  {
    if (w_static_listener > 0)
      return;
    
    if ((w_record_pip > 0) && ((int) swarm.age < 30))
      return;
    
    super.move();
  }
  
  void display ()
  {
    super.display();
    
    noFill();
    stroke(col, 200);
    strokeWeight(1.0);
    ellipse(x, y, 9.0, 9.0);
    
    noStroke();
    fill(col, 80);
    ellipse(x, y, 18.0, 18.0);
  }
  
  void destroy()
  {
    return;
  }
  
  AtomGenetic reproduce()
  {
    return null;
  }
}


/*
 * AtomSynth: Adds support for SC synthesis objects.
 *-------------------------------------------------------------------*/

class AtomSynth extends AtomTuned
{
  Synth   synth;
  String  synth_name;
  
  Synth   panner_synth;
  Bus     panner_bus;
  
  int     outbus = (int) w_outbus;
  
  boolean playing = false;
  Spec [] map_axis;
  Spec    map_vel;
  
  AtomSynth (AtomSwarmGenetic swarm)
  {
    super(swarm);
    
    synth = new Synth(synth_name);
    map_axis = new Spec[2];
    outbus = swarm.outbus;

    panner_bus = new Bus("audio", Server.local, 1);
    panner_synth = new Synth("atompanner_1p" + (int) w_channels);
    panner_synth.set("inbus", panner_bus.index);
    panner_synth.set("outbus", outbus);
    panner_synth.addToHead();
  }
  
  void destroy()
  {
    panner_bus.free();
    panner_synth.set("gate", 0);
    synth.set("gate", 0);
    
    super.destroy();
  }
  
  void play()
  {
    if (!playing)
    {
      synth.synthname = synth_name;
      synth.set("outbus", panner_bus.index);
      synth.addToHead();
      playing = true;
    }
  }
  
  void stop()
  {
    if (playing)
    {
      synth.set("gate", 0);
      println("setting gate to 0");
      synth.free();
      playing = false;
      
      synth = new Synth(synth_name);
    }
  }
  
  void set (String key, float value)
  {
    if (key == "outbus")
    {
       outbus = (int) value;
       panner_synth.set("outbus", (int) value);
    }
    else
    {
      if (synth != null)
        synth.set(key, value);
    }
  }
  
  void move()
  {
    super.move();
    
    if (map_axis[0] != null)
    {
      float value = map_axis[0].map(x / width);
      this.set(map_axis[0].name, value);
    }
    
    if (map_axis[1] != null)
    {
      float value = map_axis[1].map(y / height);
      this.set(map_axis[1].name, value);
    }

    if (map_vel != null)
    {
      float value = map_vel.map(velocity / w_max_velocity);
      this.set(map_vel.name, value);
    }
  }
  
  float vecToPan(float vx, float vy)
  {
    if (w_channels == 2)
    {
      // stereo panning
      float pan_range = w_pan_range;
      return(vx / pan_range);
    }
    else
    {
      // azimuth panning
      vy = -vy;
      return atan2(vx, vy) / PI;
    }
  }
  
  float vecToAmp(float vx, float vy)
  {
    float distance = dist(0, 0, vx, vy);

    if (distance > w_audio_range)
       return 0;
       
    float amp = (w_audio_range - distance) / w_audio_range;
    amp = amp * amp * amp;
    
    return amp;
  }

  float vecToPanWidth(float vx, float vy)
  {
    // inefficient - same calculation as vecToPan...
    float distance = dist(0, 0, vx, vy);

    if (distance > w_audio_range)
       return 1;
       
    float panwidth = linexp(w_audio_range - distance, 0, w_audio_range, 2, w_channels);
    
    return panwidth;
  }
  
  void autoPan()
    { this.autoPan(0.5); }
  
  void autoPan (float weight)
  {
    Atom listener = swarm.listener;
    
    float pan = this.vecToPan(x - listener.x, y - listener.y);
    panner_synth.set("pan", pan);


    // not using for the moment - why?
    
    if (w_channels > 2)
    {
      float panwidth = this.vecToPanWidth(x - listener.x, y - listener.y);
      panner_synth.set("panwidth", panwidth);
//      println("pw: " + panwidth);
    }

    
    float amp = this.vecToAmp(x - listener.x, y - listener.y);
    panner_synth.set("amp", (1 - weight) + (weight * amp));
  }

  
  void eat(AtomFood food)
  {
    super.eat(food);
    
    Synth eat = new Synth("atom_formlet");
    eat.set("outbus", panner_bus.index);
//    eat.set("freq", tuning_basenote);
    eat.addToHead();
  }
}




/*
 * AtomTuned: Is tuned to a MIDI base note, and can infect others.
 *-------------------------------------------------------------------*/

class AtomTuned extends AtomGenetic
{
  
  
  float tuning_basenote = 33.0, // A3
        tuning_mutate_chance = 0.001,
        tuning_infect_chance = 0.01;
  
  AtomTuned (AtomSwarmGenetic swarm)
  {
    super(swarm);

    ring_color = color(random(255), 255, 200);
  }
  
  void display()
  { 
    super.display();
    strokeWeight(1.0);
    stroke(ring_color);
    ellipse(x, y, 6.0, 6.0);
  }
  
  void setBaseNote (float value)
  {
    // value is midi note
    tuning_basenote = value;
    ring_color = color((value % 12) * 255 / 12, 255, 200);
  }
  
  void move ()
  {
    super.move();
    
    if (random(1.0) < tuning_mutate_chance)
    {
      this.setBaseNote(21 + (int) random(36));
    }
    
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;
      
      if ((swarm.atoms[i] == this) || !(swarm.atoms[i] instanceof AtomTuned))
        continue;
      
      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
      if ((distance < 25.0) && (random(1.0) < tuning_infect_chance))
      {
        this.flash(40.0, color(80, 250, 200));
        ((AtomTuned) swarm.atoms[i]).setBaseNote(tuning_basenote);
      }
    }
  }
  
  void scatter (float amp)
  {
    super.scatter(amp);
    if (random(1.0) < 0.2)
       this.setBaseNote(tuning_basenote + random2(amp * 5.0));
  }
}
