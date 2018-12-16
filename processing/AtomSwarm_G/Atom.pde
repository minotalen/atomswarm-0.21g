/*
 * Atom: Base class for swarming agents.
 *   - Performs rule calculations and visual display routines.
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 *
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *--------------------------------------------------------------*/


class Atom
{
  float      x   = 0,
             y   = 0;
  float      vx  = 0,
             vy  = 0;

  float      velocity = 0;

  int        rate_counter = 0;

  float      age = 0;
  boolean    alive = true;

  AtomSwarm swarm;

  // Visual properties
  String     name = "atom";
  color      col;
  color      ring_color;

  boolean    flash_on = false;
  float      flash_width;
  color      flash_color;

  float [][] trail;
  int        trail_pos = 0,
             trail_counter = 0,
             trail_length = 0,
             trail_thresh = 1;  // threshold after which a trail segment is added


  Atom(AtomSwarm swarm)
  {
    x = random(width);
    y = random(height);

    col = color(255, 0, 255);

    trail = new float[200][2];

    this.swarm = swarm;
  }

  void destroy()
  {
    alive = false;
    this.flash(40, color(0, 200, 200));
  }



  //-------------------------------------------------
  // calculateVector: Called each frame to update vector
  //-------------------------------------------------
  void calculateVector()
  {

    //-------------------------------------------------
    // RULE: inertia
    //-------------------------------------------------
    vx = w_inertia * vx;
    vy = w_inertia * vy;

    //-------------------------------------------------
    // RULE: cohesion (from Reynolds)
    //-------------------------------------------------
    vx += w_cohesion * ((swarm.meanX - x) / 200.0);
    vy += w_cohesion * ((swarm.meanY - y) / 200.0);

    //-------------------------------------------------
    // RULE: alignment (from Reynolds)
    //-------------------------------------------------
    vx += w_alignment * ((swarm.meanvX - vx) / 20.0);
    vy += w_alignment * ((swarm.meanvY - vy) / 20.0);

    //-------------------------------------------------
    // RULE: separation (from Reynolds)
    //-------------------------------------------------
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;

      if (swarm.atoms[i] == this)
        continue;

      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
      if (distance < w_separation_threshold)
      {
        vx -= w_separation * (swarm.atoms[i].x - this.x) / 6;
        vy -= w_separation * (swarm.atoms[i].y - this.y) / 6;
      }
    }

    //-------------------------------------------------
    // RULE: gravity bounded
    //-------------------------------------------------
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;

      if (swarm.atoms[i] == this)
        continue;

      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
      if (distance < w_gravity_threshold)
      {
        vx += w_gravity * 10 * (swarm.atoms[i].x - this.x) / sq(distance);
        vy += w_gravity * 10 * (swarm.atoms[i].y - this.y) / sq(distance);
      }
    }

    //-------------------------------------------------
    // RULE: brownian motion
    //-------------------------------------------------
    if (random(1.0) < sq(w_brownian_freq * 0.2))
    {
        vx += w_brownian * (random(20.0) - 10);
        vy += w_brownian * (random(20.0) - 10);
    }

    //-------------------------------------------------
    // RULE: center
    //-------------------------------------------------
//    vx += w_centre * ((width / 2) - this.y) / width;
//    vy += w_centre * ((height / 2) - this.y) / height;

    //-------------------------------------------------
    // RULE: bound
    //-------------------------------------------------
    if (x < w_bound_threshold)
       vx += w_bound * ((w_bound_threshold - x) / 50.0);
    if (x > width + w_bound_threshold)
       vx -= w_bound * ((x - (width - w_bound_threshold)) / 50.0);

    if (y < w_bound_threshold)
       vy += w_bound * ((w_bound_threshold - y) / 50.0);
    if (y > height - w_bound_threshold)
       vy -= w_bound * ((y - (height + w_bound_threshold)) / 50.0);

    //-------------------------------------------------
    // RULE: cursor follow
    //-------------------------------------------------
    if ((w_drag_swarm > 0) && (mousePressed))
    {
      vx += w_cursor * (mouseX - this.x) / 200;
      vy += w_cursor * (mouseY - this.y) / 200;
    };

    //-------------------------------------------------
    // calculate velocity
    //-------------------------------------------------
    velocity = sqrt(sq(vx) + sq(vy));
    if (velocity > w_max_velocity)
    {
      vx = (w_max_velocity / velocity) * vx;
      vy = (w_max_velocity / velocity) * vy;
    }
  }


  //-------------------------------------------------
  // move: Update position and update trail variable
  //-------------------------------------------------
  void move()
  {
    age += swarm.rate;

    if (++rate_counter >= (1.0 / swarm.rate))
    {
      this.calculateVector();
      rate_counter = 0;
    }

    // move
    x += vx * swarm.rate;
    y += vy * swarm.rate;


    // add trail
    if (++trail_counter > trail_thresh)
    {
      trail_counter = 0;
      this.addTrailSegment();
    }
  }

  void addTrailSegment()
  {
          trail_counter = 0;
      trail[trail_pos][0] = x;
      trail[trail_pos][1] = y;

      if (trail_length < w_trail_max_length)
         trail_length++;

      // may occur if trail max length changes during execution
      if (trail_length > w_trail_max_length)
         trail_length = (int) w_trail_max_length - 1;

      if (++trail_pos >= 200)
        trail_pos = 0;
  }

  //-------------------------------------------------
  // scatter: Add random x/y vector.
  //-------------------------------------------------
  void scatter ()
    { this.scatter(1.0); }

  void scatter (float amp)
  {
    vx += amp * random2(100.0);
    vy += amp * random2(100.0);
  }


  //-------------------------------------------------
  // trigger: Placeholder for subclasses
  //-------------------------------------------------
  void trigger ()
  {
  }


  //-------------------------------------------------
  // set: Placeholder for subclasses
  //-------------------------------------------------
  void set (String arg, float value)
  {
  }


  //-------------------------------------------------
  // Display routines
  //-------------------------------------------------

  void display()
  {
    ellipseMode(CENTER);

    noFill();
    stroke(col, 250);
    strokeWeight(1.0);
    ellipse(x, y, 4.0, 4.0);
    stroke(col, 80);
    line(x, y, x - (vx * 1.0), y - (vy * 1.0));

    //-------------------------------------------------
    // Add text name
    //-------------------------------------------------
    if ((w_display_names > 0) && (age < 100))
    {
      // cast to int for clearer text display
      fill(0, 0, 255, 200);
      text(name, (int) x + 10, (int) y + 5);
    }

    this.drawTrail();

    //-------------------------------------------------
    // Display flash
    //-------------------------------------------------
    if (flash_on)
    {
       flash_on = false;
       noStroke();
       fill(flash_color, 50.0);
       ellipse(x, y, flash_width, flash_width);
       stroke(0, 0, 0, 50);
    }

    if (!alive)
    {
      swarm.remove(this);
    }
  }

  void drawTrail()
  {
    //-------------------------------------------------
    // Draw trail
    //-------------------------------------------------
    if (flash_on)
      stroke(col, 200);
    else
      stroke(col, 240);

    int pos,
        pos_last = -1;

    for (int i = 1; i < trail_length - 1; i++)
    {
      pos = trail_pos - i;
      if (pos < 0)
         pos = 200 + trail_pos - i;

      strokeWeight(25.4 * (trail_length - i) / trail_length);
      stroke(col, (flash_on ? 2 : 1) * 70 * (trail_length - i) / trail_length);
      if (pos_last > -1)
         line(trail[pos_last][0], trail[pos_last][1], trail[pos][0], trail[pos][1]);

      pos_last = pos;
    }
  }

  //-------------------------------------------------
  // flash: Display ring for next frame only
  //-------------------------------------------------

  void flash()
  {
    flash_on = true;
    flash_width = 50.0;
    flash_color = col;
  }

  void flash(float v_width)
  {
    flash_on = true;
    flash_width = v_width;
    flash_color = col;
  }

  void flash(float v_width, color v_color)
  {
    flash_on = true;
    flash_width = v_width;
    flash_color = v_color;
  }



  //-------------------------------------------------
  // nearestAtom: Returns closest peer
  //-------------------------------------------------
  Atom nearestAtom ()
  {
    float distance_prev = 0;
    Atom peer = null;
    int i;

    for (i = 0; i < swarm.size; i++)
    {
      if (swarm.atoms[i] == this)
        continue;

      float distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
      if ((peer == null) || (distance < distance_prev))
      {
        peer = swarm.atoms[i];
        distance_prev = distance;
      }
    }

    return peer;
  }

}
