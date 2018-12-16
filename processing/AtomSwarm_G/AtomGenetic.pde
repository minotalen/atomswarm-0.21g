/*
 * AtomGenetic: Extends Atom with genetic/metabolic functions.
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 * 
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/

class AtomGenetic extends Atom
{
  float [] gene,
           hormone;

  float trigger_counter = 0.0;
  Atom  trigger_atom = null;

  AtomSwarmGenetic swarm;
  
  boolean alive = true;
  
  AtomGenetic parent = null;
  
  AtomGenetic (AtomSwarmGenetic swarm)
  {
    super(swarm);
    
    this.swarm = (AtomSwarmGenetic) super.swarm;
    
    gene = new float[G_GENE_COUNT];
    for (int i = 0; i < G_GENE_COUNT; i++)
      gene[i] = random(1.0);
        
    hormone = new float[H_HORMONE_COUNT];
    for (int i = 0; i < H_HORMONE_COUNT; i++)
      hormone[i] = random(-0.1, 0.1);
    
    col = color(255 * gene[G_COLOUR], 100, 240);
    
    int map_from     = (int) (gene[G_SON_MAP_FROM] * (float) G_SON_MAP_FROM_COUNT);
    int map_to       = (int) (gene[G_SON_MAP_TO] * (float) G_SON_MAP_TO_SPECS.length);
    
    int gen_id = (int) (gene[G_SON_GEN] * G_SON_GEN_NAMES.length);
    int dsp_id = (int) (gene[G_SON_DSP] * G_SON_DSP_NAMES.length);
  }
  
  void display()
  {
    super.display();
    if ((age < 50) && (parent != null))
    {
      stroke(0, 200, 200, 200 - (age * 4));
      strokeWeight(1.0);
      dottedLine(x, y, parent.x, parent.y, 20);
    }
  }
  
  void move()
  {
    super.move();
    
    this.calculateMetabolism();
    if (!alive)
       return;
  }
  
  void calculateMetabolism()
  {
    if ((age > (w_lifespan * (1.5 - gene[G_AGE]))) && (random(1) < 1))
    {
      trace("die: old age");
      this.destroy();
      return;
    }

    
    // CYC: CIRCADIAN
    // 1 day = 2500 ticks = 100s (25fps)
    //------------------------------------------------
    hormone[H_SEROTONIN] += (0.5 * gene[G_CYC_S]) * 2 * PI * cos(2 * PI * swarm.age / w_day_length) / (w_day_length);
    hormone[H_MELATONIN] += (0.5 * gene[G_CYC_M]) * 2 * PI * -cos(2 * PI * swarm.age / w_day_length) / (w_day_length);


    // CYC: MATURITY
    //------------------------------------------------
    if (age > (w_lifespan * (1.5 - gene[G_AGE]) * 0.1))
    {
      if (age < (w_lifespan * (1.5 - gene[G_AGE]) * 0.5))
      {
        // young - increase testosterone
        hormone[H_TESTOSTERONE] = hormone[H_TESTOSTERONE] + 0.0001;
      }
      else {
        hormone[H_TESTOSTERONE] = hormone[H_TESTOSTERONE] - 0.0001;
      }
    }

    // ACT: EAT
    //-------------------------------------------------
    if (hormone[H_LEPTIN] < random(0, 0.5))
    {
      for (int i = 0; i < swarm.food_count; i++)
      {
        float distance = dist(swarm.food[i].x, swarm.food[i].y, x, y);
        if (distance < 10)
        {
          this.eat(swarm.food[i]);
        }
      }
    }

    // ACT: REPRODUCE
    //-------------------------------------------------
    if ((hormone[H_TESTOSTERONE] > 0.5) &&
        (hormone[H_LEPTIN] > 0))
    {
      float random_thresh = 0.01;
      
      // limit chances of reproduction when swarm is near its limit
      if (swarm.size > 0.75 * w_swarm_limit)
      {
        random_thresh *= (w_swarm_limit - swarm.size) / (0.25 * w_swarm_limit);
      }

      if (random(1) < random_thresh)
      {
        this.reproduce();
        
        // was 0.5 - lowered to increase breeding
        hormone[H_TESTOSTERONE] -= 0.4;
      }
    }
    

    // CYC: ADRENAL REGULATION
    //-------------------------------------------------
    hormone[H_ADRENALINE] = hormone[H_ADRENALINE] * (1.0 - (0.01 * gene[G_CYC_A]));
    


    // CYC: HUNGER
    //-------------------------------------------------
    hormone[H_LEPTIN] = hormone[H_LEPTIN] - 0.001 * gene[G_CYC_L];
    

    // happier when full / unhappier when hungry
    hormone[H_SEROTONIN] += 0.0001 * hormone[H_LEPTIN];

    // ACT: STARVE
    //-------------------------------------------------
    if (hormone[H_LEPTIN] < -0.9)
    {
      trace("die: starvation");
      this.destroy();
      return;
    }

    // ACT: OVERLOAD
    //-------------------------------------------------
    if ((hormone[H_ADRENALINE] > 0.9) ||
        (hormone[H_TESTOSTERONE] > 0.9) ||
        (hormone[H_SEROTONIN] < -0.9))
     {
       trace("die: hormone overload");
       this.destroy();
       return;
     }
  }


  void calculateVector()
  {
    // RULE: INERTIA
    //-------------------------------------------------
    float w_inertia_local = clip1(
      w_inertia *
      (hormone[H_ADRENALINE] * 0.1 + 1) *
      (1 - hormone[H_MELATONIN] * 0.3)
    );
    vx = w_inertia_local * vx;
    vy = w_inertia_local * vy;
  
  
    // RULE: COHESION
    //-------------------------------------------------
    vx += w_cohesion * ((swarm.meanX - x) / 200.0) * (2 - gene[G_INT] * 2) * (1 + hormone[H_SEROTONIN] * 0.5);
    vy += w_cohesion * ((swarm.meanY - y) / 200.0) * (2 - gene[G_INT] * 2) * (1 + hormone[H_SEROTONIN] * 0.5);
    
    
    // RULE: ALIGNMENT
    //-------------------------------------------------
    vx += w_alignment * ((swarm.meanvX - vx) / 20.0) * (1 - gene[G_INT]);
    vy += w_alignment * ((swarm.meanvY - vy) / 20.0) * (1 - gene[G_INT]);
    
    
    // RULE: SEPARATION
    //-------------------------------------------------
    float w_separation_threshold_local = w_separation_threshold * (2 * gene[G_PERCEPTION]);
    
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;
      
      if (swarm.atoms[i] == this)
        continue;
      
      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
      if (distance < w_separation_threshold_local)
      {
        vx -= w_separation * 0.2 * (swarm.atoms[i].x - this.x) * (gene[G_INT] * 2);
        vy -= w_separation * 0.2 * (swarm.atoms[i].y - this.y) * (gene[G_INT] * 2);
        
        // ACT: COLLIDE
        //-------------------------------------------------
        hormone[H_TESTOSTERONE] += 0.002 * gene[G_UP_T];
        hormone[H_ADRENALINE] += 0.001 * gene[G_UP_A];
      }
    }
    
    // RULE: GRAVITY_BOUNDED
    //-------------------------------------------------
    float w_gravity_threshold_local = w_gravity_threshold * (2 * gene[G_PERCEPTION]);
    
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;
      
      if (swarm.atoms[i] == this)
        continue;
      
      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
      if (distance < w_gravity_threshold_local)
      {
        vx += w_gravity * 10 * (swarm.atoms[i].x - this.x) / sq(distance) * (1.5 - gene[G_INT]);
        vy += w_gravity * 10 * (swarm.atoms[i].y - this.y) / sq(distance) * (1.5 - gene[G_INT]);
      }
    } 


    // RULE: HUNGER
    //-------------------------------------------------
    float w_hunger_threshold_local = w_gravity_threshold * (2 * gene[G_PERCEPTION]);
    
    for (int i = 0; i < swarm.food_count; i++)
    {
      float distance = dist(this.x, this.y, swarm.food[i].x, swarm.food[i].y);
      if (distance < w_hunger_threshold_local)
      {
        vx += w_gravity * 50 * (swarm.food[i].x - this.x) / sq(distance) * (0 - 2 * hormone[H_LEPTIN]);
        vy += w_gravity * 50 * (swarm.food[i].y - this.y) / sq(distance) * (0 - 2 * hormone[H_LEPTIN]);
      }
    } 
        
    
    // RULE: BROWNIAN
    //-------------------------------------------------
    float w_brownian_freq_local = sq(w_brownian_freq * 0.2)
        * (hormone[H_ADRENALINE] + 1)
        * (hormone[H_TESTOSTERONE] + 1);
        
    if (random(1.0) < w_brownian_freq_local)
    {
      float w_brownian_local = w_brownian
          * ((hormone[H_ADRENALINE] + 1));
          
      vx += w_brownian_local * (random(20.0) - 10);
      vy += w_brownian_local * (random(20.0) - 10);
    }
    
    // center
//    vx += w_centre * ((width / 2) - this.y) / width;
//    vy += w_centre * ((height / 2) - this.y) / height;
    
    if (x < w_bound_threshold)
       vx += w_bound * ((w_bound_threshold - x) / 50.0);
    if (x > width - w_bound_threshold)
       vx -= w_bound * ((x - (width - w_bound_threshold)) / 50.0);

    if (y < w_bound_threshold)
       vy += w_bound * ((w_bound_threshold - y) / 50.0);
    if (y > height - w_bound_threshold)
       vy -= w_bound * ((y - (height - w_bound_threshold)) / 50.0);

    // follow the mouse
    if ((w_drag_swarm > 0) && (mousePressed))
    {
      vx += w_cursor * (mouseX - this.x) / 200;
      vy += w_cursor * (mouseY - this.y) / 200;
    };
    
    velocity = sqrt(sq(vx) + sq(vy));
    if (velocity > w_max_velocity)
    {
      vx = (w_max_velocity / velocity) * vx;
      vy = (w_max_velocity / velocity) * vy;
    }
  }
  
  
  // reproduce: Creates and returns offspring.
  //------------------------------------------------
  AtomGenetic reproduce()
  {
    Class thisClass = this.getClass();
    Class[] args = new Class[2];
    args[0] = swarm.getClass().getDeclaringClass();
    args[1] = swarm.getClass();
    try
    {
      Constructor ctor = thisClass.getDeclaredConstructor(args);
      AtomGenetic atom = (AtomGenetic) ctor.newInstance(new Object[] { applet, swarm });
      
      atom.x = this.x + random(-10, 10);
      atom.y = this.y + random(-10, 10);
      atom.parent = this;
      
      for (int i = 0; i < G_GENE_COUNT; i++)
      {
        // genetic reproduction/mutation
        if (random(1) < w_gene_mutate_chance)
          atom.gene[i] = random(1);
        else
          atom.gene[i] = clip1(gene[i] + random(-w_gene_variation, w_gene_variation));
      }
      
      swarm.add(atom);
      return atom;
    }
    catch (Exception e)
    {
      warn("reproduce failed: " + e);
      return null;
    }
  }
  
  
  // eat: 
  //------------------------------------------------

  void eat(AtomFood food)
  {
    swarm.removeFood(food);
    this.flash(20, color(40, 100, 200));
    hormone[H_LEPTIN] = hormone[H_LEPTIN] + 0.25 + (0.5 * gene[G_UP_L]);
  }      
}


/*
 * AtomSynthGenetic: Adds support for sound generation genes.
 *-------------------------------------------------------------------*/

class AtomSynthGenetic extends AtomSynth
{
  AtomMeme meme = null;
  
  AtomSynthGenetic (AtomSwarmGenetic swarm)
  {
    super(swarm);
    
    ring_color = color(gene[G_SON_GEN] * 255, 255, 200);
    int n = floor(gene[G_SON_TRIG_T] * (float) G_SON_TRIG_T_COUNT);
  }
  
  void play()
  {
    // beware! gene[G_SON_GEN] could == 1 if created via reproduce()
    // (because clip1 clips to a max == 1.0)
    int gen_id = (int) (gene[G_SON_GEN] * G_SON_GEN_NAMES.length);
    if (gen_id == G_SON_GEN_NAMES.length)
        gen_id = G_SON_GEN_NAMES.length - 1;
        
    int dsp_id = (int) (gene[G_SON_DSP] * G_SON_DSP_NAMES.length);
    if (dsp_id == G_SON_DSP_NAMES.length)
        dsp_id = G_SON_DSP_NAMES.length - 1;

    String gen_name = G_SON_GEN_NAMES[gen_id];
    String dsp_name = G_SON_DSP_NAMES[dsp_id];
    
    synth_name = "atom_" + gen_name + "_tr_" + dsp_name;
    
    super.play();
  }

  AtomGenetic reproduce()
  {
    AtomSynthGenetic atom = (AtomSynthGenetic) super.reproduce();
    if (atom != null)
    {
      atom.play();
    }
    return atom;
  }
  
  void move()
  { 
    float map_value  = 0,
          map_mapped = 0;
          
    int map_from     = (int) (gene[G_SON_MAP_FROM] * (float) G_SON_MAP_FROM_COUNT);
    int map_to       = (int) random(G_SON_MAP_TO_SPECS.length);
    Spec map_spec    = G_SON_MAP_TO_SPECS[map_to];
    
    super.move();
    
    if (!alive)
       return;
   
    this.processTriggers();
       
    switch (map_from)
    {
      case G_SON_MAP_FROM_X: map_value = x / width; break;
      case G_SON_MAP_FROM_Y: map_value = y / height; break;
      case G_SON_MAP_FROM_V: map_value = velocity / w_max_velocity; break;
      case G_SON_MAP_FROM_PROX: break;
      case G_SON_MAP_FROM_DIR: map_value = atan2(vx, vy) / PI; break;
    };
    
    map_value = clip1(map_value);
    map_mapped = map_spec.map(map_value);
    synth.set(map_spec.name, map_mapped);
    this.autoPan();
    
    
    // ACT: MEMIFY
    //------------------------------------------------
    if ((meme == null) && (random(1.0) < 0.0001))
    { 
      int      meme_gene = G_MEME_GENES[(int) random(G_MEME_GENES.length)];
      float    meme_value = gene[meme_gene];
      
      meme = new AtomMeme(meme_gene, meme_value);
      
      this.flash(30, color(100, 200, 200));
    }
    else if (meme != null)
    {
      meme.age += 1;
      if (meme.age > meme.age_max)
      {
        this.meme = null;
      }
      else {
      
        // XXX: Inefficient to be continually calculating nearestAtom - we are detecting
        // "collisions" in 3 different places...
        Atom nearest = this.nearestAtom();
        if ((nearest != null) &&
            (nearest.getClass() == this.getClass()) &&
            (dist(this.x, this.y, nearest.x, nearest.y) < 50) &&
            (random(1.0) < meme.strength * 0.2))
        {
          ((AtomSynthGenetic) nearest).infectMeme(meme);
        }
      }
    }
  }
  
  void display()
  {
    super.display();
    if (meme != null)
    {
      noFill();
      stroke(60, 220, 180, 120);
      strokeWeight(4);
      ellipse(x, y, 10, 10);
    } 
  }
  
  void infectMeme(AtomMeme meme)
  {
    if (this.meme == null)
    {
      this.flash(30, color(100, 200, 200));
    
      this.meme = meme;
      this.gene[meme.gene] = meme.value;
      
      // update ring color if necessary
      ring_color = color(gene[G_SON_GEN] * 255, 255, 200);
      
      this.stop();
      this.play();
    }
  }
  
  void processTriggers()
  {
    int trigger_type = floor(gene[G_SON_TRIG_T] * (float) G_SON_TRIG_T_COUNT);    
    
    // TRIG: COLLIDE
    //------------------------------------------------
    if ((trigger_type == G_SON_TRIG_T_COLL) && (age > trigger_counter + 10))
    {
      for (int i = 0; i < swarm.size; i++)
      {
        float distance;
      
        if (swarm.atoms[i] == this)
          continue;
      
        distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
        if (distance < w_separation_threshold * (2 * gene[G_PERCEPTION]))
        {
          this.trigger();
          trigger_counter = age;
          return;
        }
      }
    }

    // TRIG: DEPOSIT
    //------------------------------------------------
    if (trigger_type == G_SON_TRIG_T_DEPOS)
    {
      float trigger_period = map1(gene[G_SON_TRIG_THRESH], 15, 100);
      trigger_counter += velocity;
      if (trigger_counter > trigger_period)
      {
        this.trigger();
        trigger_counter = trigger_counter - trigger_period;
      }
    }

    // TRIG: TURN
    //------------------------------------------------
    if (trigger_type == G_SON_TRIG_T_TURN)
    {
      float bearing = atan2(vx, vy) / PI;
      float bearing_thresh = map1(gene[G_SON_TRIG_THRESH], 0.2, 0.6);
      if (abs(bearing - trigger_counter) > bearing_thresh)
      {
        this.trigger();
        trigger_counter = bearing;
      }
    }

    // TRIG: PROXIM
    //------------------------------------------------
    if (trigger_type == G_SON_TRIG_T_PROX)
    {
      Atom nearest = this.nearestAtom();
      if (nearest != trigger_atom)
      {
        this.trigger();
        trigger_atom = nearest;
      }
    }

    // TRIG: VEL THRESH
    //------------------------------------------------
    if (trigger_type == G_SON_TRIG_T_VELTH)
    {
      float trigger_limit = map1(gene[G_SON_TRIG_THRESH], 4, 10);
      if (velocity > trigger_limit)
      {
        if (trigger_counter == 0)
        {
          int partial = (int) linrand(20);
          synth.set("freq", midicps(tuning_basenote - 12) * partial + random2(2.0));
          
          this.trigger();
          trigger_counter = 1;
          col = color(220);
        }
      } else {
        if (trigger_counter == 1)
        {
          this.trigger();
          trigger_counter = 0;
          col = color(255 * gene[G_COLOUR], 200, 200);
        }
      }
    } 
  }
  
  void trigger()
  {
    if (synth != null)
    {
      synth.set("t_trig", 1);
      this.flash();
    }
  }
  
  void setBaseNote (float value)
  {

    tuning_basenote = value;
  }
}

class AtomVelThreshG extends AtomSynthGenetic
{
  AtomVelThreshG(AtomSwarmGenetic swarm)
  {
    super(swarm);
  }
  
  void play()
  {
    gene[G_SON_GEN] = 0;
    gene[G_SON_DSP] = 0;
    gene[G_SON_TRIG_T] = (float) G_SON_TRIG_T_VELTH / G_SON_TRIG_T_COUNT;
    super.play();
  }
}

class AtomFood
{
  float x, y;
  float size,
        alpha;
  
  AtomFood(float x, float y)
  {
    this.x = x;
    this.y = y;
    size = random(5.0, 15.0);
    //alpha = linlin(size, 0.8, 2.0, 50, 150);
    alpha = random(50, 150);
  }
  
  void display()
  {
    noFill();
    ellipseMode(CENTER);
    fill(0, 0, 100, alpha);
    noStroke();
    strokeWeight(2);
    ellipse(x, y, size / 4, size / 4);
    fill(0, 0, 100, alpha / 3);
    stroke(0, 0, 100, alpha);
    strokeWeight(1);
    ellipse(x, y, size, size);
  }
}


/*
 * AtomMeme: Object codifying a genetically-modifying virus.
 *-------------------------------------------------------------------*/

class AtomMeme
{
  int   gene;
  float value,
        strength,
        age_max,
        age;
  
  AtomMeme()
  {
    this(G_MEME_GENES[(int) random(G_MEME_GENES.length)], random(1.0));
  }
  
  AtomMeme(int gene, float value)
  {
    this.gene = gene;
    this.value = value;
    this.strength = random(1.0);
    this.age = 0;
    this.age_max = 500 + random(200);
  }
}
