/*
 * AtomClasses2: Additional species-specific classes.
 * OBSOLETE FOLLOWING THE INTRODUCTION OF GENETIC BEHAVIOURS!
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 * 
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/
 

/*
 * AtomVelThresh: Outputs a sinewave when above a fixed velocity.
 *-------------------------------------------------------------------*/
 
class AtomVelThresh extends AtomSynth
{
  boolean gate = false;
  
  AtomVelThresh (AtomSwarmGenetic swarm)
  {
    super(swarm);
    
    synth_name = "atom_velsine";
    name = "vel_thresh";
    
    this.play();
  }
  
  void play ()
  {
    synth.set("on", 0);
    super.play();
  }

  void move ()
  {
    super.move();
    
    if (velocity > w_velthresh_limit)
    {
      if (!gate)
      {
        int partial = (int) linrand(20);
      
        gate = true;
        this.flash();
        col = color(40, 250, 200);
      
        synth.set("freq", midicps(tuning_basenote - 12) * partial + random2(2.0));
        synth.set("on", 1);
      }
    }
    else {
      if (gate)
      {
        gate = false;
        col = color(255);
        synth.set("on", 0);
      }
    }
    
    if (gate)
    {
      this.autoPan(0.5);
    }
  }
  
  void display ()
  {
    super.display();
    if (gate)
    {
      fill(col, 70); 
      noStroke();
      ellipse(x, y, 14, 14);
    }
  }
}


/*
 * AtomBuzzer: Alternative synth graph to VelThresh.
 *-------------------------------------------------------------------*/

class AtomBuzzer extends AtomVelThresh
{
  AtomBuzzer (AtomSwarmGenetic swarm)
  {
    super(swarm);

    name = "buzzer";
  }
  
  void play()
  {
    synth_name = "atom_ps";
    super.play();
  }
}


/*
 * AtomDoppler: Drone, with doppler effect relative to listener.
 *-------------------------------------------------------------------*/

class AtomDoppler extends AtomSynth
{
  float nearest_dist = 0;
  
  AtomDoppler (AtomSwarmGenetic swarm)
  {
    super(swarm);
    
    synth_name = "hornet";
    name = "hornet";
    map_vel = new Spec("amp", 0.1, 0.4, 0);
    
    this.play();
  }
 
  void play ()
  {
    int octave = (int)random(4) + 2;
    synth.set("octave", octave);
    super.play();
  }
  
  void move()
  {
    Atom nearest;
    float nearest_dist_new,
          nearest_delta;
    float freq;
          
    super.move();
  
    // nearest = this.nearestAtom(); 
    nearest = swarm.listener;
    if (nearest != null)
    {
      nearest_dist_new = dist(this.x, this.y, nearest.x, nearest.y);
    } else {
      nearest_dist_new = 0;
    }
    
    nearest_delta = nearest_dist_new - nearest_dist;
    nearest_dist = nearest_dist_new;
    
    freq = (midicps(tuning_basenote) * 2) + (nearest_delta * 5.0);
    synth.set("freq", clip(freq, 40, 10000));
    
    this.autoPan(0.4);
  }
}


/*
 * AtomDeposit: Triggers after travelling x pixels.
 *-------------------------------------------------------------------*/

class AtomDepositor extends AtomSynth
{
  float deposit_counter = 0,
        deposit_period = 200;
  
  AtomDepositor (AtomSwarmGenetic swarm)
  {
    super(swarm);
    
    synth_name = "inst_pluck";
    name = "depositor";
    col = color(180, 255, 200);
  }
  
  void move()
  {
    super.move();
    
    deposit_counter += velocity; // * env.rate
    if (deposit_counter > deposit_period)
    {
      this.trigger();
      deposit_counter = deposit_counter - deposit_period;
    }
  }
  
  void trigger()
  {
    Atom nearest = this.nearestAtom();
    
    float damp = 0.95;
    float partial = linrand(10);
    float [] scale = { 0, 2, 5, 7, 11 };
    float freq = midicps(tuning_basenote + 12 + scale[(int) random(scale.length)]) * pow(2, (int)random(4) + 1);
//    float freq = midicps(tuning_basenote + 12) * pow(2, (int)random(4) + 1);
//    trace("freq " + freq);

    synth = new Synth(synth_name);
    synth.set("outbus", outbus);
    synth.set("freq", freq);
    
    if (nearest != null)
    {
      float distance = dist(x, y, nearest.x, nearest.y);
      damp = 1 - sq(clip(distance / 400.0, 0, 0.4));
    }

      
    this.flash(damp * 300 - 230);
    synth.set("damp", damp);
    synth.set("pan", random2(1));
    
    synth.addToHead();
  }
}


/*
 * AtomCollider: Triggers upon colliding with peers.
 *-------------------------------------------------------------------*/

class AtomCollider extends AtomSynth
{
  float last_collide_age = -1;
  
  AtomCollider (AtomSwarmGenetic swarm)
  {
    super(swarm);
    
    synth_name = "one_cricket";
    name = "collider";
  }
  
  void move()
  {
    super.move();
    
    if (age > last_collide_age + 10)
    {
    
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;
      
      if (swarm.atoms[i] == this)
        continue;
      
      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
      if (distance < w_separation_threshold)
      {
        this.trigger();
        last_collide_age = age;
        return;
      }
     }
    }
  }
  
  void trigger()
  {
    this.flash();
    
    synth = new Synth(synth_name);
    synth.set("outbus", panner_bus.index);
    
    this.autoPan();
    
    synth.addToHead();
  }
}


/*
 * AtomVisWarp: Currently under development.
 *-------------------------------------------------------------------*/

class AtomVisWarp extends AtomGenetic
{
  AtomVisWarp (AtomSwarmGenetic swarm)
  {
    super(swarm);
  }
  
  void move()
  {
    super.move();
    
    w_trail_max_length = 50 * (velocity / w_max_velocity);
    
    println("max length now " + w_trail_max_length);
  }
}


/*
 * AtomDirChange: Triggers after a large change in bearing.
 *-------------------------------------------------------------------*/

class AtomDirChange extends AtomSynth
{
  float bearing,
        bearing_prev,
        bearing_thresh = 0.4;
        
  AtomDirChange (AtomSwarmGenetic swarm)
  {
   super(swarm);
    
   synth_name = "atom_pulse";
   name = "firefly";
  }
 
  void move()
  {
    super.move();
    
    // 1 = upwards
    // 0 = downwards
    // 0.5 = right
    // -0.5 = left
    float bearing = atan2(vx, vy) / PI;
    
    if (abs(bearing - bearing_prev) > bearing_thresh)
    {
      this.trigger();
    }
    bearing_prev = bearing;
  }
  
  void trigger()
  {
    synth = new Synth(synth_name);
    synth.set("outbus", panner_bus.index);
    synth.set("freq", random(30, 800));
    synth.set("releasetime", 0.2 + (velocity / w_max_velocity));
    synth.addToHead();
    
    this.autoPan(0.8);
    
    this.flash();
  }
}



/*
 * AtomProxim: Triggers when the nearest atom is replaced.
 *-------------------------------------------------------------------*/

class AtomProxim extends AtomSynth
{
  Buffer buffer;
  Atom nearest_prev = null;
  
  AtomProxim (AtomSwarmGenetic swarm)
  {
    super(swarm);
    
    synth_name = "atomcollider_1p2";
    name = "proxim";
  }
  
  void move()
  {
    super.move();
    
    Atom nearest = this.nearestAtom();
    
    if (nearest != nearest_prev)
    {
      this.trigger();
      nearest_prev = nearest;
    }
  }
  
  void trigger()
  {
    synth = new Synth(synth_name);
    synth.set("outbus", panner_bus.index);
    
    int bufnum = (int) random(10) * 2;
    println("buffer: " + bufnum);
    // only want buffers that divide by 2 -
    // - stereo input so odd buffers unused
    synth.set("bufnum", bufnum);
    
    synth.set("amp", 0.4);
    synth.set("rate", 0.7 + (velocity / w_max_velocity));
    synth.addToHead();
    
    this.autoPan(0.8);
    
    this.flash();
  }
}
