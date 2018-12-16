/*
 * AtomSpider: Captures its peers, passing them through a DSP unit.
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

float w_spider_capture_chance = 0.02,
      w_spider_capture_thresh = 100,
      w_spider_release_chance = 0.02,
      w_spider_release_thresh = 500;

class AtomSpider extends AtomSynth
{
  Set prey;
  Bus bus;
  
  AtomSpider (AtomSwarmGenetic v_swarm)
  {
    super(v_swarm);
    
    name = "spider";
    
    prey = new HashSet();
    bus = new Bus("audio", Server.local, 2);
    
    switch ((int) random(9))
    {
      case 0:
        synth_name = "fx_ringmod";
        map_axis[0] = new Spec("freq");
        map_axis[1] = new Spec("wet");
        break;
      case 1:
        synth_name = "fx_degrader";
	map_axis[0] = new Spec("samplerate", 5000, 20000, 1);
	map_axis[1] = new Spec("bits", 3, 16, 0);
        break;
      case 2:
        synth_name = "fx_pitchshift";
	map_vel = new Spec("pitchratio", 0.2, 2, 1);
        break;
      case 3:
        synth_name = "fx_delay_seaside";
        map_axis[0] = new Spec("delay_time", 0.01, 0.2, 0);
        map_axis[1] = new Spec("decay_time", 0.1, 5.0, 0);
        break;
      case 4:
        synth_name = "fx_feedli";
        map_axis[0] = new Spec("feedback", 0.6, 1.0, 0);
        map_axis[1] = new Spec("delaytime", 0.01, 0.5, 0);
        break;
      case 5:
        synth_name = "fx_ringmod";
        map_vel = new Spec("freq", 5, 30, 1);
        break;
      case 6:
        synth_name = "fx_rev_iron";
        map_axis[0] = new Spec("reverbtime", 2.0, 10.0, 0);

        map_vel = new Spec("wet");
        break;
      case 7:
        synth_name = "fx_granule";
        map_vel = new Spec("pitchratio", 0.1, 10, 1);
//        map_axis[0] = new Spec("windowsize", 0.01, 0.2, 0);
        map_axis[1] = new Spec("amp", 1, 2, 0);
        break;
        
        // can sometimes blow up and get load - avoid for now
      case 8:
        synth_name = "fx_charsiesis";
        map_vel = new Spec("feedback", 0.0, 0.4, 0);
        map_axis[0] = new Spec("rate_range", 0.01, 5.0, 1);
        map_axis[1] = new Spec("lpf_f", 300, 5000, 1);
        break;
    }

    synth.set("wet", 1.0);
    synth.set("inbus", bus.index);
    
    this.play();
  }
  
  void destroy()
  {
    Object [] prey_array = prey.toArray();

    // wet parameter has a Lag of 0.5s
    synth.set("wet", 0.0);

    for (int i = 0; i < prey.size(); i++)
    {
      Atom atom = (Atom) prey_array[i];
      atom.set("outbus", swarm.outbus);
    }
    
    super.destroy();
  }
  
  void play()
  {
    if (!playing)
    {
      synth.synthname = synth_name;
      synth.set("outbus", outbus);
      synth.addToTail();
      playing = true;
    }
  }

  void display()
  {
    super.display();
    
    noFill();
    stroke(0, 0, 255, 220);
    ellipse(x, y, 10.0, 10.0);
   
    stroke(0, 50, 200, 250);

    strokeWeight(1.0);
    
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;
      
      if (swarm.atoms[i] == this)
        continue;
        
      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);

      if (prey.contains(swarm.atoms[i]))
      {
        dottedLine(x, y, swarm.atoms[i].x, swarm.atoms[i].y, 20);
        
        if ((distance > w_spider_release_thresh) && (random(1.0) < w_spider_release_chance))
        {
          swarm.atoms[i].set("outbus", swarm.outbus);
          prey.remove(swarm.atoms[i]); 
        }
      }
      else {
        if ((distance < w_spider_capture_thresh) && (random(1.0) < w_spider_capture_chance))
        {
          // Can't capture other spiders
          Class peerClass = swarm.atoms[i].getClass();
          String peerClassName = peerClass.getName();
          Atom peer = swarm.atoms[i];
          
          if (peerClassName.indexOf("AtomSpider") == -1)
          {
            println("setting outbus of " + peerClassName);
            peer.set("outbus", bus.index);
            prey.add(peer);
            peer.flash(20, color(0, 200, 100));
          }
        }
      }
    }
  }
}
