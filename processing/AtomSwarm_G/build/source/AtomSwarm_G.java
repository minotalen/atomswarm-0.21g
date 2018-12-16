import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.opengl.*; 
import oscP5.*; 
import supercollider.*; 
import controlP5.ControlP5; 
import controlP5.Slider; 
import promidi.*; 
import java.lang.reflect.Constructor; 
import java.awt.GraphicsDevice; 
import java.awt.GraphicsEnvironment; 
import java.awt.Frame; 
import java.io.FilenameFilter; 
import java.util.HashSet; 
import java.awt.DisplayMode; 
import java.util.Set; 
import java.util.Hashtable; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class AtomSwarm_G extends PApplet {

/*
 * AtomSwarm/G: Environmental controls.
 *
 *  - Creates GUI, keyboard and MIDI control elements
 *  - Controls global audio effects and video/audio recording
 *  - Performs init tasks including sample loading
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 *
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *--------------------------------------------------------------*/








            // Required for MIDI controls
//import moviemaker.*;         // Required for video recording










MidiIO   midi;
boolean  midi_relative = true;
Object   applet = this;

AtomSwarmGenetic swarm;
Atom atom_selected = null;
Buffer [] buffers;

boolean finished = false;

PFont font;
ControlP5 control;
Slider s_cohesion,
  s_gravity,
  s_separation,
  s_sepprop,
  s_brownian,
  s_brownian_freq,
  s_alignment,
  s_inertia,
  s_rate,
  s_trail,
  s_amp;

Bus    fx_bus;
Buffer buffer_diskout;
Synth  synth_reverb,
  synth_diskout,
  synth_pip;

//MovieMaker mm;

float score = 44;
float highScore = 44;


/// Dodger
float changeVel = 3.85f;               // modifies all velocities
Dodger dodger;
int dodgerSize = 15;
float startVel;                       // beginning velocity of dodger, increases by scVel for every score
float scVel;
float rotVel;                         // rotation velocity of dodger
float rotAcc;                        // rotation acceleration of dodger, increases by scAcc for every score
float scAcc;
float rotMod = 1;                    // rotation modulation with q and e buttons
float rotDamp = 0.985f;                // rotation velocity dampening
boolean clockwise;                    // is the player turning clockwise
PVector currentPos;                   // holds the current pos of dodger
float currentAng;                     // holds the current rotation of dodger
PVector highScorePosition;

/// Aura
float circleFactor = 0.25f;             // size of aura per obstacle size
int circleAdd = 20;                  // added to size of aura
int circleTransparency = 20;
float bossCFactor = 1.5f;              // boss has smaller circle and no add


public void init()
 {
 //  randomSeed(20);
 if (w_fullscreen > 0)
 {
 frame.removeNotify();
 frame.setUndecorated(true);
 frame.addNotify();
 }

 super.start();
 }


public void initGraphics()
{
  trace("init: Graphics");

  //hint(ENABLE_OPENGL_4X_SMOOTH);
  //  hint(DISABLE_OPENGL_2X_SMOOTH);
  //  smooth();
  frameRate(30);
  colorMode(HSB, 255);

  font = loadFont("Silkscreen-8.vlw");
  textFont(font, 20);

  if (w_fullscreen > 0)
  {
    if (w_fullscreen_secondary > 0)
      frame.setLocation(1440, 0);
    //    else
    //      fullscreenOn();
  } else {
    frame.setLocation(0, 0);
  }

  //  if (w_record_video > 0)
  //    mm = new MovieMaker(this, width, height, "swarm.mov", MovieMaker.JPEG, MovieMaker.HIGH, 25);
}

public void setup ()
{
  //size(1440, 900, OPENGL);
  

  initGraphics();
  initControls();
  //initMIDI();
  initAudio();

  map_init();

  // dodger attributes
    rotVel = 0;   // current rotation velocity
    startVel = 2.8f * changeVel;
    scVel = 0.002f * changeVel;
    // sponge something is horribly broken here, dodger always turns the same speed
    rotAcc = random(0.02f, 0.03f); // current rotation acceleration
    scAcc = 0.00001f * changeVel;
    currentPos = new PVector(width/2, height*3/4);
    dodger = new Dodger(currentPos.x, currentPos.y, currentAng);




  //-------------------------------------------------
  // Init swarm
  //-------------------------------------------------
  swarm = new AtomSwarmGenetic();
  swarm.outbus = fx_bus.index;

  AtomListener listener = new AtomListener(swarm);
  swarm.add(listener);
  swarm.listener = listener;

  trace("init: swarm");
}





public void initAudio ()
{
  //-------------------------------------------------
  // Init SuperCollider
  //-------------------------------------------------
  trace("init: Audio");
  Server.local = new Server("127.0.0.1", 57110);

  //-------------------------------------------------
  // Init samples
  //-------------------------------------------------
  //  trace("init: samples");
  //  this.loadSamples("/Users/daniel/audio/samples/acoustic/toy piano/studio/onset");
  //   this.loadSamples("/Users/daniel/audio/samples/speech/yorkshire_tics");

  //-------------------------------------------------
  // Init fx
  //-------------------------------------------------
  fx_bus = new Bus("audio", Server.local, 8);

  synth_reverb = new Synth("atom_gverb");
  synth_reverb.set("wet", 0.15f);
  synth_reverb.set("reverbtime", 5.0f);
  synth_reverb.set("damp", 0.1f);
  synth_reverb.set("inbus", fx_bus.index);
  synth_reverb.set("outbus", (int) w_outbus);
  synth_reverb.set("amp", 1.0f);
  synth_reverb.addToTail(Server.local.root_group);

  if (w_record_audio > 0)
  {
    // if we're making a 5.1 recording, add 1 for LFE
    int buffer_channels = (int) w_channels;
    if (buffer_channels == 5)
      buffer_channels = 6;

    long time = (long) millis() / 1000;

    buffer_diskout = new Buffer(Server.local, 65536, (int) buffer_channels);
    buffer_diskout.alloc();
    buffer_diskout.write(w_record_path + "/audio." + time + ".aif", "aiff", "int32", -1, 0, 1);

    synth_diskout = new Synth("diskOut_" + (int) w_channels);
    synth_diskout.set("bufnum", buffer_diskout.index);
    synth_diskout.set("inbus", (int) w_outbus);
    synth_diskout.addToTail(Server.local.root_group);
  }

  if (w_cache_synths > 0)
  {
    cacheSynths();
  }
}



/*
 * Terminate processing, & perform cleanup tasks.
 *--------------------------------------------------------------*/

public void stop ()
{
  trace("-- TERMINATING");
  //  if (w_record_video > 0)
  //    mm.finishMovie();

  if (w_record_audio > 0)
  {
    buffer_diskout.close();
    buffer_diskout.free();
    synth_diskout.free();
  }

  synth_reverb.free();
  swarm.destroy();
}


public void draw ()
{
  //  if ((w_fullscreen > 0) && (w_fullscreen_secondary == 0))
  //    noCursor();

  //  translate(0, 0, -1000);

  background(15);

  fill(255);
  dodger.update();
  dodger.bounds();                        // check if dodger is still in bounds (if not, put back)
  dodger.drawCircle(false, dodger.pos, 10);
  dodger.draw();
  rotAcc = (2 + score*scAcc) * changeVel; // increase the rotation velocity by rotation acceleration
  rotVel += rotAcc;                       // velocity increases by acceleration
  rotVel *= rotDamp;                      // dampen the rotation velocity

  swarm.listener.x = dodger.pos.x;
  swarm.listener.y = dodger.pos.y;

  swarm.move();
  swarm.display();

  //  translate(0, 0, 1000);

  if ((w_record_pip > 0) && ((int) swarm.age == 15))
  {
    synth_pip = new Synth("diskOut_pip");
    synth_pip.addToTail(Server.local.root_group);
    swarm.listener.flash(40, color(255, 255, 255));
  }

  //-------------------------------------------------
  // Recalculate reverberation based on swarm spacing
  //-------------------------------------------------
  float mean_distance = 0.0f;
  if (swarm.size > 1)
  {
    for (int i = 0; i < swarm.size; i++)
    {
      //    Atom peer = swarm.atoms[i].nearestAtom();
      //    mean_distance += dist(swarm.atoms[i].x, swarm.atoms[i].y, peer.x, peer.y);
      mean_distance += dist(swarm.atoms[i].x, swarm.atoms[i].y, swarm.listener.x, swarm.listener.y);
    }

    if (mean_distance > 0)
    {
      mean_distance /= swarm.size;
      // XXX: needs to be calibrated for screen size!
      synth_reverb.set("earlylevel", 0.2f + mean_distance / (width / 10));
      synth_reverb.set("taillevel", 0.1f + mean_distance / (width / 10));
    }
  }

  if (w_record_video > 0)
  {
    loadPixels();
    //    mm.addFrame(pixels, width, height);
  }

  //-------------------------------------------------
  // Draw sun
  //-------------------------------------------------
  noStroke();
  fill(40, 150 * sin(2 * PI * swarm.age / w_day_length), 200 * sin(2 * PI * swarm.age / w_day_length), 250);
  //stroke(40, 150 * sin(2 * PI * swarm.age / w_day_length), 200 * sin(2 * PI * swarm.age / w_day_length), 250);
  ellipseMode(CENTER);
  ellipse(115, 115, 100, 100);


  //-------------------------------------------------
  // Handle atom selection
  //-------------------------------------------------
  if (atom_selected != null)
  {
    if (atom_selected.alive)
    {
      stroke(0, 0, 255, 100);
      noFill();
      strokeWeight(swarm.age % 2);
      ellipse(atom_selected.x, atom_selected.y, 20, 20);
    } else
    {
      atom_selected = null;
    }

    drawGenetics();
  }
}



/*
 * controlP5 controls and getter/setter methods.
 *--------------------------------------------------------------*/

public void initControls ()
{
  trace("init: Controls");

  control = new ControlP5(this);

  control.setColorActive(color(0, 255, 255));
  control.setColorBackground(color(0, 0, 30));
  control.setColorForeground(color(50, 150, 100));
  //control.setColorLabel(color(0, 0, 100));
  //control.setColorValue(color(0, 0, 200));

  // controlP5 addSlider syntax has changed -
  // we now require a helper function to build our labelled sliders.
  s_cohesion      = addSlider(control, 0.5f, 0, 0, "setCohesion", "COH");
  s_gravity       = addSlider(control, 0.5f, 3, 0, "setGravity", "GRA");
  s_separation    = addSlider(control, 0.5f, 6, 0, "setSeparation", "SEP");
  s_sepprop       = addSlider(control, 0.5f, 9, 0, "setSepProp", "SEPPR");

  s_brownian      = addSlider(control, 0.5f, 0, 3, "setBrownian", "BRO");
  s_brownian_freq = addSlider(control, 0.5f, 3, 3, "setBrownianFreq", "BRF");
  s_alignment     = addSlider(control, 0.2f, 6, 3, "setAlignment", "ALI");
  s_inertia       = addSlider(control, 0.5f, 9, 3, "setInertia", "INE");

  s_rate          = addSlider(control, 0.4f, 0, 6, "setRate", "RATE");
  s_trail         = addSlider(control, 0.1f, 3, 6, "setTrail", "TRAIL");
  s_amp           = addSlider(control, 0.5f, 6, 6, "setAmp", "AMP");
}

public Slider addSlider (ControlP5 control, float def, int x, int y, String method, String label)
{
  Slider slider = (Slider) control.addSlider(method, 0, 1, def, 20 + (60 * x), 20 + (25 * y), 150, 50);
  slider.setLabel(label);
  slider.setLabel(label);
  return slider;
}

public void setCohesion (float value)
{
  w_cohesion = value * 2;
}

public void setGravity (float value)
{
  w_gravity = value * 2;
}

public void setSeparation (float value)
{
  w_separation = value * 2;
}

public void setSepProp (float value)
{
  w_sepprop = value * 2;
}

public void setBrownian (float value)
{
  w_brownian = value * 2;
}

public void setBrownianFreq (float value)
{
  w_brownian_freq = value * 2;
}

public void setAlignment (float value)
{
  w_alignment = value * 2;
}

public void setInertia (float value)
{
  w_inertia = 0.9f + (value * 0.1f);
}

public void setRate (float value)
{
  if (swarm != null)
    swarm.rate = value * 2;
}

public void setTrail (float value)
{
  if(atom_selected == swarm.listener) {
    w_trail_max_length = value * 300;
  } else {
    w_trail_max_length = value * 100;
  }
}

public void setAmp (float value)
{
  if (synth_reverb != null)
    synth_reverb.set("amp", value * 2.0f);
}



/*
 * Key handlers, primarily to create/destroy atoms.
 *--------------------------------------------------------------*/

public void keyPressed ()
{
  Atom atom;


  if(!clockwise){
    rotVel = max(-150, -22-rotVel/5);
  }
  clockwise = true;

  if (keyCode == 8)
  {
    // backspace pressed - quit!
    for (int i = swarm.size - 1; i >= 0; i--)
    {
      atom = swarm.atoms[i];
      atom.destroy();
    }

    swarm.size = 0;
    swarm.atoms = new Atom[0];

    return;
  }

  switch (key)
  {
  case '\'':
    swarm.scatter();
    break;
  case '\\':
    swarm.unify();
    break;
  case ' ':
    swarm.paused = !swarm.paused;
    break;
  case 'a':
    atom = new AtomSynthGenetic(swarm);
    ((AtomSynthGenetic) atom).play();
    swarm.add(atom);
    break;
  case 'A':
    destroyAtomOfType("AtomSynthGenetic");
    break;
  case 'o':
    atom = new AtomDoppler(swarm);
    swarm.add(atom);
    break;
  case 'O':
    destroyAtomOfType("AtomDoppler");
    break;
  case 'v':
    atom = new AtomVelThreshG(swarm);
    ((AtomSynthGenetic) atom).play();
    swarm.add(atom);
    break;
  case 'V':
    destroyAtomOfType("AtomVelThresh");
    break;
  case 'd':
    atom = new AtomDepositor(swarm);
    swarm.add(atom);
    break;
  case 'D':
    destroyAtomOfType("AtomDepositor");
    break;
  case 'c':
    atom = new AtomCollider(swarm);
    swarm.add(atom);
    break;
  case 'C':
    destroyAtomOfType("AtomCollider");
    break;
  case 's':
    atom = new AtomSpider(swarm);
    swarm.add(atom);
    break;
  case 'S':
    destroyAtomOfType("AtomSpider");
    break;
  case 'w':
    atom = new AtomDirChange(swarm);
    swarm.add(atom);
    break;
  case 'W':
    destroyAtomOfType("AtomDirChange");
    break;
  case 'p':
    atom = new AtomProxim(swarm);
    swarm.add(atom);
    break;
  case 'P':
    destroyAtomOfType("AtomProxim");
    break;
  case 'z':
    atom = new AtomBuzzer(swarm);
    swarm.add(atom);
    break;
  case 'Z':
    destroyAtomOfType("AtomBuzzer");
    break;
  case '+':
    if ((atom_selected != null) && (atom_selected != swarm.listener))
    {
      ((AtomGenetic) atom_selected).reproduce();
    }
    break;

  case '\b':
    if ((atom_selected != null) && (atom_selected != swarm.listener))
    {
      atom_selected.destroy();
      swarm.remove(atom_selected);
      atom_selected = null;
    }
    break;
  }
}


/*
 * Load samples as used by Proxim.
 *--------------------------------------------------------------*/

public void loadSamples(String path)
{
  File file = new File(path);
  FilenameFilter filter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      return !name.startsWith(".");
    }
  };
  File [] files = file.listFiles(filter);

  if (files.length == 0)
    return;

  buffers = new Buffer[files.length];
  for (int i = 0; i < files.length; i++)
  {
    buffers[i] = new Buffer(Server.local, 2);
    buffers[i].read(files[i].toString());
  }

  trace("read " + buffers.length + " sound files");
}




/*
 * Initialise and andle MIDI controls.
 *--------------------------------------------------------------*/

public void initMIDI ()
{
  try {
    midi = MidiIO.getInstance(this);

    // 10 = channel 11 - good for relative mode (faderfox 10)
    // 11 = channel 12 - good for absolute mode (faderfox 12)
    //    midi.openInput(2, 10);
    midi.openInput(2, 10);

    trace("init: MIDI");
  }
  catch (Exception e)
  {
    warn("Could not init MIDI!" + e);
  }
}

public void controllerIn(Controller controller, int device, int channel)
{
  int num = controller.getNumber();
  int val = controller.getValue();

  //  trace("control: (" + num + ", " + val + ")");

  if (midi_relative)
  {
    val = val >= 64 ? val - 128 : val;
    Slider control = null;
    switch (num)
    {
    case 0:
      control = s_cohesion;
      break;
    case 1:
      control = s_gravity;
      break;
    case 2:
      control = s_separation;
      break;
    case 3:
      control = s_sepprop;
      break;

    case 4:
      control = s_brownian;
      break;
    case 5:
      control = s_brownian_freq;
      break;
    case 6:
      control = s_alignment;
      break;
    case 7:
      control = s_inertia;
      break;

    case 8:
      control = s_rate;
      break;
    case 9:
      control = s_trail;
      break;
    case 11:
      control = s_amp;
      break;
    }

    if (control != null)
    {
      float value = control.getValue();
      value = clip(value + (val / 127.0f), 0, 1);
      control.setValue(value);
    }
  } else
  {
    Slider control = null;

    switch (num)
    {
    case 48:
      control = s_cohesion;
      break;
    case 49:
      control = s_gravity;
      break;
    case 50:
      control = s_separation;
      break;
    case 51:
      control = s_sepprop;
      break;

    case 52:
      control = s_brownian;
      break;
    case 53:
      control = s_brownian_freq;
      break;
    case 54:
      control = s_alignment;
      break;
    case 55:
      control = s_inertia;
      break;
    }

    if (control != null)
      control.setValue((float) val / 127.0f);
  }
}

public void noteOn(Note note, int device, int channel)
{
  int num = note.getPitch();
  int vel = note.getVelocity();

  //  trace("note: (" + num + ", " + vel + ")");

  if (vel > 0)
  {
    AtomSynth atom;

    switch (num)
    {
    case 48:
      atom = new AtomDoppler(swarm);
      swarm.add(atom);
      break;
    case 49:
      atom = new AtomVelThresh(swarm);
      swarm.add(atom);
      break;
    case 50:
      atom = new AtomDepositor(swarm);
      swarm.add(atom);
      break;
    case 51:
      atom = new AtomCollider(swarm);
      swarm.add(atom);
      break;

    case 52:
      atom = new AtomBuzzer(swarm);
      swarm.add(atom);
      break;
    case 53:
      atom = new AtomDirChange(swarm);
      swarm.add(atom);
      break;
    case 54:
      atom = new AtomProxim(swarm);
      swarm.add(atom);
      break;

    case 56:
      atom = new AtomSynthGenetic(swarm);
      ((AtomSynthGenetic) atom).play();
      swarm.add(atom);
      break;

    case 58:
      atom = new AtomSpider(swarm);
      swarm.add(atom);
      break;
      // need to be able to destroy

    case 60:
      swarm.scatter();
      break;
    case 61:
      swarm.unify();
      break;
    case 62:
      swarm.addFood((int) random(1, 15));
      break;
    case 63:
      swarm.paused = !swarm.paused;
      break;

    case 64:
      destroyAtomOfType("AtomDoppler");
      break;
    case 65:
      destroyAtomOfType("AtomVelThresh");
      break;
    case 66:
      destroyAtomOfType("AtomDepositor");
      break;
    case 67:
      destroyAtomOfType("AtomCollider");
      break;

    case 68:
      destroyAtomOfType("AtomBuzzer");
      break;
    case 69:
      destroyAtomOfType("AtomDirChange");
      break;
    case 70:
      destroyAtomOfType("AtomProxim");
      break;

    case 72:
      destroyAtomOfType("AtomSynthGenetic");
      break;

    case 74:
      destroyAtomOfType("AtomSpider");
      break;
    }
  }
}




/*
 * General atom create/destroy methods.
 *--------------------------------------------------------------*/

public void createAtomOfType (String type_name)
{
  String type_name_full;
  Class type;
  Atom atom;

  type_name_full = this.getClass().toString() + "$" + type_name;

  try {
    type = Class.forName(type_name_full);

    Class[] args = new Class[1];
    args[0] = swarm.getClass();
    Constructor ctor = type.getConstructor(args);

    atom = (Atom) ctor.newInstance(new Object[] { swarm });
    swarm.add(atom);
  }
  catch (Exception e)
  {
    println("Invalid type name: " + type_name);
    return;
  }
}

public void destroyAtomOfType (String type_name)
{
  int [] indices = new int[swarm.size];
  int indices_count = 0;

  for (int i = 0; i < swarm.size; i++)
  {
    // this is really really messy.
    if (swarm.atoms[i].getClass().toString().indexOf(type_name) > -1)
    {
      indices[indices_count++] = i;
    }
  }

  if (indices_count > 0)
  {
    int index = indices[(int) random(indices_count)];
    swarm.atoms[index].destroy();
    swarm.remove(swarm.atoms[index]);
  }
}



/*
 * Mouse handlers, for closer control over swarm movements.
 *--------------------------------------------------------------*/

public void mousePressed ()
{
  // locate nearest atom
  float distance_prev = 0;
  Atom atom = null;

  for (int i = 0; i < swarm.size; i++)
  {
    float distance = dist(mouseX, mouseY, swarm.atoms[i].x, swarm.atoms[i].y);
    if ((atom == null) || (distance < distance_prev))
    {
      atom = swarm.atoms[i];
      distance_prev = distance;
    }
  }

  if ((atom != null) && (distance_prev < 50))
  {
    // atom has been clicked
    atom_selected = atom;
  }

  if (w_drag_listener > 0)
  {
    swarm.listener.x = mouseX;
    swarm.listener.y = mouseY;
  }
}

public void mouseDragged()
{
  if (w_drag_listener > 0)
  {
    swarm.listener.x = mouseX;
    swarm.listener.y = mouseY;
  }
}



/*
 * Output genetic information on the currently-selected atom.
 *--------------------------------------------------------------*/

public void drawGenetics()
{
  // Called during draw() when an atom is selected
  if (!(atom_selected instanceof AtomGenetic))
    return;

  AtomGenetic atom = (AtomGenetic) atom_selected;
  textSize(20);
  fill(0, 0, 255, 150);

  int x = 800;
  int y = 20;

  y += 20;
  text("G_COLOUR    ", x, y);
  text(atom.gene[G_COLOUR], x + 120, y);
  y += 20;
  text("G_SIZE      ", x, y);
  text(atom.gene[G_SIZE], x + 120, y);
  y += 20;
  text("G_SHAPE     ", x, y);
  text(atom.gene[G_SHAPE], x + 120, y);
  y += 20;
  text("G_AGE       ", x, y);
  text(atom.gene[G_AGE], x + 120, y);
  y += 20;
  text("G_INT       ", x, y);
  text(atom.gene[G_INT], x + 120, y);
  y += 20;
  text("G_PERC      ", x, y);
  text(atom.gene[G_PERCEPTION], x + 120, y);

  y += 20;
  text("AGE         ", x, y);
  text(atom.age, x + 120, y);


  y = 20;
  x = 1050;

  y += 20;
  text("G_UP_S      ", x, y);
  text(atom.gene[G_UP_S], x + 120, y);
  y += 20;
  text("G_UP_M      ", x, y);
  text(atom.gene[G_UP_M], x + 120, y);
  y += 20;
  text("G_UP_T      ", x, y);
  text(atom.gene[G_UP_T], x + 120, y);
  y += 20;
  text("G_UP_L      ", x, y);
  text(atom.gene[G_UP_L], x + 120, y);
  y += 20;
  text("G_UP_A      ", x, y);
  text(atom.gene[G_UP_A], x + 120, y);
  y += 20;
  text("G_UP_D      ", x, y);
  text(atom.gene[G_UP_D], x + 120, y);

  y = 20;
  x = 1300;

  y += 20;
  text("G_CYC_S     ", x, y);
  text(atom.gene[G_CYC_S], x + 120, y);
  y += 20;
  text("G_CYC_M     ", x, y);
  text(atom.gene[G_CYC_M], x + 120, y);
  y += 20;
  text("G_CYC_T     ", x, y);
  text(atom.gene[G_CYC_T], x + 120, y);
  y += 20;
  text("G_CYC_L     ", x, y);
  text(atom.gene[G_CYC_L], x + 120, y);
  y += 20;
  text("G_CYC_A     ", x, y);
  text(atom.gene[G_CYC_A], x + 120, y);
  y += 20;
  text("G_CYC_D     ", x, y);
  text(atom.gene[G_CYC_D], x + 120, y);

  y = 20;
  x = 1550;

  y += 20;
  text("G_SON_GEN         ", x, y);
  text(atom.gene[G_SON_GEN], x + 230, y);
  y += 20;
  text("G_SON_DSP         ", x, y);
  text(atom.gene[G_SON_DSP], x + 230, y);
  y += 20;
  text("G_SON_TRIG_T      ", x, y);
  text(atom.gene[G_SON_TRIG_T], x + 230, y);
  y += 20;
  text("G_SON_TRIG_THRESH ", x, y);
  text(atom.gene[G_SON_TRIG_THRESH], x + 230, y);
  y += 20;
  text("G_SON_MAP_FROM    ", x, y);
  text(atom.gene[G_SON_MAP_FROM], x + 230, y);
  y += 20;
  text("G_SON_MAP_TO      ", x, y);
  text(atom.gene[G_SON_MAP_TO], x + 230, y);

  fill(50, 200, 255, 150);
  y = 20;
  x = 2000;

  y += 20;
  text("H_SER", x, y);
  text(atom.hormone[H_SEROTONIN], x + 100, y);
  y += 20;
  text("H_MEL", x, y);
  text(atom.hormone[H_MELATONIN], x + 100, y);
  y += 20;
  text("H_TES", x, y);
  text(atom.hormone[H_TESTOSTERONE], x + 100, y);
  y += 20;
  text("H_LEP", x, y);
  text(atom.hormone[H_LEPTIN], x + 100, y);
  y += 20;
  text("H_ADR", x, y);
  text(atom.hormone[H_ADRENALINE], x + 100, y);
  //  y += 10; text("H_DOP", x, y); text(atom.hormone[H_DOPAMINE], x + 50, y);
}


/*
 * Precache UGens, required to prevent SC memory paging dropouts.
 * (hopefully not needed since SC3.2)
 *--------------------------------------------------------------*/

public void cacheSynths()
{
  String [] synthNames = {
    // synth classes
    // (XXX: names/params to be unified)
    "hornet",
    "inst_pluck",
    "one_cricket",
    "atom_pulse",
    "atomcollider_1p2",
    "atom_velsine",
    "atom_ps",

    // spider
    "fx_ringmod",
    "fx_degrader",
    "fx_pitchshift",
    "fx_delay_seaside",
    "fx_feedli",
    "fx_dist3",
    "fx_rev_iron",
    "fx_granule",
    "fx_charsiesis"
  };
  Synth [] synths = new Synth[synthNames.length];

  for (int i = 0; i < synthNames.length; i++)
  {
    String name = synthNames[i];
    synths[i] = new Synth(name);
    synths[i].set("outbus", 100);
    synths[i].addToTail();
  }

  try
  {
    Thread.currentThread().sleep(100);
  }
  catch (Exception e)
  {
    println("EXCEPTION: " + e);
  }

  trace("Cached " + synths.length + " synths");

  for (int i = 0; i < synths.length; i++)
  {
    synths[i].free();
  }
}

public void keyReleased() { // listen for user input // touchEnded
  if(clockwise){
    // if(soundEffects) {
    //   sLeft.trigger();
    // }
    rotVel = max(-150, -22-rotVel/5);
  }
  clockwise = false;
}
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
  int      col;
  int      ring_color;

  boolean    flash_on = false;
  float      flash_width;
  int      flash_color;

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

  public void destroy()
  {
    alive = false;
    this.flash(40, color(0, 200, 200));
  }



  //-------------------------------------------------
  // calculateVector: Called each frame to update vector
  //-------------------------------------------------
  public void calculateVector()
  {

    //-------------------------------------------------
    // RULE: inertia
    //-------------------------------------------------
    vx = w_inertia * vx;
    vy = w_inertia * vy;

    //-------------------------------------------------
    // RULE: cohesion (from Reynolds)
    //-------------------------------------------------
    vx += w_cohesion * ((swarm.meanX - x) / 200.0f);
    vy += w_cohesion * ((swarm.meanY - y) / 200.0f);

    //-------------------------------------------------
    // RULE: alignment (from Reynolds)
    //-------------------------------------------------
    vx += w_alignment * ((swarm.meanvX - vx) / 20.0f);
    vy += w_alignment * ((swarm.meanvY - vy) / 20.0f);

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
    if (random(1.0f) < sq(w_brownian_freq * 0.2f))
    {
        vx += w_brownian * (random(20.0f) - 10);
        vy += w_brownian * (random(20.0f) - 10);
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
       vx += w_bound * ((w_bound_threshold - x) / 50.0f);
    if (x > width + w_bound_threshold)
       vx -= w_bound * ((x - (width - w_bound_threshold)) / 50.0f);

    if (y < w_bound_threshold)
       vy += w_bound * ((w_bound_threshold - y) / 50.0f);
    if (y > height - w_bound_threshold)
       vy -= w_bound * ((y - (height + w_bound_threshold)) / 50.0f);

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
  public void move()
  {
    age += swarm.rate;

    if (++rate_counter >= (1.0f / swarm.rate))
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

  public void addTrailSegment()
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
  public void scatter ()
    { this.scatter(1.0f); }

  public void scatter (float amp)
  {
    vx += amp * random2(100.0f);
    vy += amp * random2(100.0f);
  }


  //-------------------------------------------------
  // trigger: Placeholder for subclasses
  //-------------------------------------------------
  public void trigger ()
  {
  }


  //-------------------------------------------------
  // set: Placeholder for subclasses
  //-------------------------------------------------
  public void set (String arg, float value)
  {
  }


  //-------------------------------------------------
  // Display routines
  //-------------------------------------------------

  public void display()
  {
    ellipseMode(CENTER);

    noFill();
    stroke(col, 250);
    strokeWeight(1.0f);
    ellipse(x, y, 4.0f, 4.0f);
    stroke(col, 80);
    line(x, y, x - (vx * 1.0f), y - (vy * 1.0f));

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
       fill(flash_color, 50.0f);
       ellipse(x, y, flash_width, flash_width);
       stroke(0, 0, 0, 50);
    }

    if (!alive)
    {
      swarm.remove(this);
    }
  }

  public void drawTrail()
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

      strokeWeight(25.4f * (trail_length - i) / trail_length);
      stroke(col, (flash_on ? 2 : 1) * 70 * (trail_length - i) / trail_length);
      if (pos_last > -1)
         line(trail[pos_last][0], trail[pos_last][1], trail[pos][0], trail[pos][1]);

      pos_last = pos;
    }
  }

  //-------------------------------------------------
  // flash: Display ring for next frame only
  //-------------------------------------------------

  public void flash()
  {
    flash_on = true;
    flash_width = 50.0f;
    flash_color = col;
  }

  public void flash(float v_width)
  {
    flash_on = true;
    flash_width = v_width;
    flash_color = col;
  }

  public void flash(float v_width, int v_color)
  {
    flash_on = true;
    flash_width = v_width;
    flash_color = v_color;
  }



  //-------------------------------------------------
  // nearestAtom: Returns closest peer
  //-------------------------------------------------
  public Atom nearestAtom ()
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
  
  public void move()
  {
    if (w_static_listener > 0)
      return;
    
    if ((w_record_pip > 0) && ((int) swarm.age < 30))
      return;
    
    super.move();
  }
  
  public void display ()
  {
    super.display();
    
    noFill();
    stroke(col, 200);
    strokeWeight(1.0f);
    ellipse(x, y, 9.0f, 9.0f);
    
    noStroke();
    fill(col, 80);
    ellipse(x, y, 18.0f, 18.0f);
  }
  
  public void destroy()
  {
    return;
  }
  
  public AtomGenetic reproduce()
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
  
  public void destroy()
  {
    panner_bus.free();
    panner_synth.set("gate", 0);
    synth.set("gate", 0);
    
    super.destroy();
  }
  
  public void play()
  {
    if (!playing)
    {
      synth.synthname = synth_name;
      synth.set("outbus", panner_bus.index);
      synth.addToHead();
      playing = true;
    }
  }
  
  public void stop()
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
  
  public void set (String key, float value)
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
  
  public void move()
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
  
  public float vecToPan(float vx, float vy)
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
  
  public float vecToAmp(float vx, float vy)
  {
    float distance = dist(0, 0, vx, vy);

    if (distance > w_audio_range)
       return 0;
       
    float amp = (w_audio_range - distance) / w_audio_range;
    amp = amp * amp * amp;
    
    return amp;
  }

  public float vecToPanWidth(float vx, float vy)
  {
    // inefficient - same calculation as vecToPan...
    float distance = dist(0, 0, vx, vy);

    if (distance > w_audio_range)
       return 1;
       
    float panwidth = linexp(w_audio_range - distance, 0, w_audio_range, 2, w_channels);
    
    return panwidth;
  }
  
  public void autoPan()
    { this.autoPan(0.5f); }
  
  public void autoPan (float weight)
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

  
  public void eat(AtomFood food)
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
  
  
  float tuning_basenote = 33.0f, // A3
        tuning_mutate_chance = 0.001f,
        tuning_infect_chance = 0.01f;
  
  AtomTuned (AtomSwarmGenetic swarm)
  {
    super(swarm);

    ring_color = color(random(255), 255, 200);
  }
  
  public void display()
  { 
    super.display();
    strokeWeight(1.0f);
    stroke(ring_color);
    ellipse(x, y, 6.0f, 6.0f);
  }
  
  public void setBaseNote (float value)
  {
    // value is midi note
    tuning_basenote = value;
    ring_color = color((value % 12) * 255 / 12, 255, 200);
  }
  
  public void move ()
  {
    super.move();
    
    if (random(1.0f) < tuning_mutate_chance)
    {
      this.setBaseNote(21 + (int) random(36));
    }
    
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;
      
      if ((swarm.atoms[i] == this) || !(swarm.atoms[i] instanceof AtomTuned))
        continue;
      
      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);
      if ((distance < 25.0f) && (random(1.0f) < tuning_infect_chance))
      {
        this.flash(40.0f, color(80, 250, 200));
        ((AtomTuned) swarm.atoms[i]).setBaseNote(tuning_basenote);
      }
    }
  }
  
  public void scatter (float amp)
  {
    super.scatter(amp);
    if (random(1.0f) < 0.2f)
       this.setBaseNote(tuning_basenote + random2(amp * 5.0f));
  }
}
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
  
  public void play ()
  {
    synth.set("on", 0);
    super.play();
  }

  public void move ()
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
      
        synth.set("freq", midicps(tuning_basenote - 12) * partial + random2(2.0f));
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
      this.autoPan(0.5f);
    }
  }
  
  public void display ()
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
  
  public void play()
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
    map_vel = new Spec("amp", 0.1f, 0.4f, 0);
    
    this.play();
  }
 
  public void play ()
  {
    int octave = (int)random(4) + 2;
    synth.set("octave", octave);
    super.play();
  }
  
  public void move()
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
    
    freq = (midicps(tuning_basenote) * 2) + (nearest_delta * 5.0f);
    synth.set("freq", clip(freq, 40, 10000));
    
    this.autoPan(0.4f);
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
  
  public void move()
  {
    super.move();
    
    deposit_counter += velocity; // * env.rate
    if (deposit_counter > deposit_period)
    {
      this.trigger();
      deposit_counter = deposit_counter - deposit_period;
    }
  }
  
  public void trigger()
  {
    Atom nearest = this.nearestAtom();
    
    float damp = 0.95f;
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
      damp = 1 - sq(clip(distance / 400.0f, 0, 0.4f));
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
  
  public void move()
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
  
  public void trigger()
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
  
  public void move()
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
        bearing_thresh = 0.4f;
        
  AtomDirChange (AtomSwarmGenetic swarm)
  {
   super(swarm);
    
   synth_name = "atom_pulse";
   name = "firefly";
  }
 
  public void move()
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
  
  public void trigger()
  {
    synth = new Synth(synth_name);
    synth.set("outbus", panner_bus.index);
    synth.set("freq", random(30, 800));
    synth.set("releasetime", 0.2f + (velocity / w_max_velocity));
    synth.addToHead();
    
    this.autoPan(0.8f);
    
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
  
  public void move()
  {
    super.move();
    
    Atom nearest = this.nearestAtom();
    
    if (nearest != nearest_prev)
    {
      this.trigger();
      nearest_prev = nearest;
    }
  }
  
  public void trigger()
  {
    synth = new Synth(synth_name);
    synth.set("outbus", panner_bus.index);
    
    int bufnum = (int) random(10) * 2;
    println("buffer: " + bufnum);
    // only want buffers that divide by 2 -
    // - stereo input so odd buffers unused
    synth.set("bufnum", bufnum);
    
    synth.set("amp", 0.4f);
    synth.set("rate", 0.7f + (velocity / w_max_velocity));
    synth.addToHead();
    
    this.autoPan(0.8f);
    
    this.flash();
  }
}
/*
 * AtomConstants: Global constants and configuration.
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 * 
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/


float

w_channels              = 2,          // number of output channels
w_fullscreen            = 0,          // fullscreen mode
w_fullscreen_secondary  = 0,          // ..on secondary display

w_pan_range             = 200.0f,      // panning distance between speakers (pixels)
w_audio_range           = 300.0f,      // amplitude range from min..max (pixels)
w_day_length            = 2500,       // (seconds)
w_lifespan              = 5000,       // mean lifespan of agent (seconds)
w_swarm_limit           = 50,         // max swarm size
w_outbus                = 0,          // audio output bus

w_record_audio          = 0,          // record audio within SC?
w_record_pip            = 0,          // play initial synchro pip
w_record_video          = 0,          // record video within P5?
w_cache_synths          = 0,          // precache synth objects to prevent dropouts?

w_inertia               = 0.97f,       //       
w_max_velocity          = 12.0f,       // (pixels/sec)
w_display_names         = 1,          // display agent names on spawn
w_drag_swarm            = 0,          // use cursor to drag swarm?
w_drag_listener         = 0,          // use cursor to drag listener?
w_static_listener       = 0,          // freeze listener in space?

w_cohesion              = 1,          // RULE STRENGTHS
w_gravity               = 1,          //
w_separation            = 1,          //
w_sepprop               = 1,          //
w_brownian              = 1,          //
w_brownian_freq         = 1,          //
w_alignment             = 1,          //
w_centre                = 1,          //
w_leader                = 1,          //
w_cursor                = 1,          //
w_bound                 = 1.5f,        //

w_bound_threshold       = 100,        // bounding distance from edge of screen (pixels)
w_separation_threshold  = 30,         // threshold to avoid other agents (pixels)
w_gravity_threshold     = 150,        // threshold under which gravity takes effect
w_velthresh_limit       = 5,          // 

w_trail_max_length      = 40;         // max number of trail segments

String

w_record_path           = "/Users/daniel/projects/swarm/audio";
/*
 * AtomGenConst: Constants used in genetic/metabolic algorithms
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 * 
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/


float w_gene_mutate_chance = 0.05f,
      w_gene_variation = 0.1f;



/*-------------------------------------------------------------------
 * Gene types.
 *-------------------------------------------------------------------*/

final int G_COLOUR          = 00,
          G_SIZE            = 01,
          G_SHAPE           = 02,
          G_AGE             = 03,
          G_INT             = 04,
          G_PERCEPTION      = 05,
          
          G_UP_S            = 10,
          G_UP_M            = 11,
          G_UP_T            = 12,
          G_UP_L            = 13,
          G_UP_A            = 14,
          G_UP_D            = 15,  
          
          G_CYC_S           = 20,
          G_CYC_M           = 21,
          G_CYC_T           = 22,
          G_CYC_L           = 23,
          G_CYC_A           = 24,
          G_CYC_D           = 25,
          
          G_SON_GEN         = 30,
          G_SON_DSP         = 31,
          G_SON_TRIG_T      = 32,
          G_SON_TRIG_THRESH = 33,
          G_SON_MAP_FROM    = 34,
          G_SON_MAP_TO      = 35;

final int G_GENE_COUNT      = 48;

/*-------------------------------------------------------------------
 * G_SON_GEN_NAMES: Unadorned names of SC generator synths.
 *-------------------------------------------------------------------*/

final String [] G_SON_GEN_NAMES =
{
  "velsine",
  "cricket",
  "bump",
  "firefly",
  "throb",
  "tick",
  "zipper",
};

/*-------------------------------------------------------------------
 * G_SON_DSP_NAMES: Codes used by SC signal processing synths. 
 *-------------------------------------------------------------------*/

final String [] G_SON_DSP_NAMES =
{
  "00",
  "00",
  "00",
  "sq",
  "qq",
  "pi",
  "ri",
  "gr",
  "wl",
};


/*-------------------------------------------------------------------
 * G_SON_TRIG_T: Enumerated types of trigger events.
 *-------------------------------------------------------------------*/

final int G_SON_TRIG_T_COLL   = 0,
          G_SON_TRIG_T_VELTH  = 1,
          G_SON_TRIG_T_TURN   = 2,
          G_SON_TRIG_T_DEPOS  = 3,
          G_SON_TRIG_T_PROX   = 4;
final int G_SON_TRIG_T_COUNT  = 5;


/*-------------------------------------------------------------------
 * G_SON_MAP_FROM: Enumerated mapping source parameters.
 *-------------------------------------------------------------------*/

final int G_SON_MAP_FROM_X    = 0,
          G_SON_MAP_FROM_Y    = 1,
          G_SON_MAP_FROM_V    = 2,
          G_SON_MAP_FROM_PROX = 3,
          G_SON_MAP_FROM_DIR  = 4;
final int G_SON_MAP_FROM_COUNT = 5;


/*-------------------------------------------------------------------
 * G_SON_MAP_TO: Specified mapping destination parameters.
 *-------------------------------------------------------------------*/

Spec [] G_SON_MAP_TO_SPECS =
{
   new Spec("amp", 0.2f, 0.8f, 0),
   new Spec("xa", 0.0f, 1.0f, 0),
   new Spec("xb", 0.0f, 1.0f, 0)
};


/*-------------------------------------------------------------------
 * G_MEME_GENES: Genes affected by memetic infection.
 *-------------------------------------------------------------------*/

final int [] G_MEME_GENES =
{
  G_SON_GEN,
  G_SON_DSP,
  G_SON_MAP_TO
};



//-------------------------------------------------
// Hormone types.
//-------------------------------------------------
final int H_SEROTONIN       = 0,
          H_MELATONIN       = 1,
          H_TESTOSTERONE    = 2,
          H_LEPTIN          = 3,
          H_ADRENALINE      = 4;
          
final int H_HORMONE_COUNT   = 5;
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

  float trigger_counter = 0.0f;
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
      gene[i] = random(1.0f);
        
    hormone = new float[H_HORMONE_COUNT];
    for (int i = 0; i < H_HORMONE_COUNT; i++)
      hormone[i] = random(-0.1f, 0.1f);
    
    col = color(255 * gene[G_COLOUR], 100, 240);
    
    int map_from     = (int) (gene[G_SON_MAP_FROM] * (float) G_SON_MAP_FROM_COUNT);
    int map_to       = (int) (gene[G_SON_MAP_TO] * (float) G_SON_MAP_TO_SPECS.length);
    
    int gen_id = (int) (gene[G_SON_GEN] * G_SON_GEN_NAMES.length);
    int dsp_id = (int) (gene[G_SON_DSP] * G_SON_DSP_NAMES.length);
  }
  
  public void display()
  {
    super.display();
    if ((age < 50) && (parent != null))
    {
      stroke(0, 200, 200, 200 - (age * 4));
      strokeWeight(1.0f);
      dottedLine(x, y, parent.x, parent.y, 20);
    }
  }
  
  public void move()
  {
    super.move();
    
    this.calculateMetabolism();
    if (!alive)
       return;
  }
  
  public void calculateMetabolism()
  {
    if ((age > (w_lifespan * (1.5f - gene[G_AGE]))) && (random(1) < 1))
    {
      trace("die: old age");
      this.destroy();
      return;
    }

    
    // CYC: CIRCADIAN
    // 1 day = 2500 ticks = 100s (25fps)
    //------------------------------------------------
    hormone[H_SEROTONIN] += (0.5f * gene[G_CYC_S]) * 2 * PI * cos(2 * PI * swarm.age / w_day_length) / (w_day_length);
    hormone[H_MELATONIN] += (0.5f * gene[G_CYC_M]) * 2 * PI * -cos(2 * PI * swarm.age / w_day_length) / (w_day_length);


    // CYC: MATURITY
    //------------------------------------------------
    if (age > (w_lifespan * (1.5f - gene[G_AGE]) * 0.1f))
    {
      if (age < (w_lifespan * (1.5f - gene[G_AGE]) * 0.5f))
      {
        // young - increase testosterone
        hormone[H_TESTOSTERONE] = hormone[H_TESTOSTERONE] + 0.0001f;
      }
      else {
        hormone[H_TESTOSTERONE] = hormone[H_TESTOSTERONE] - 0.0001f;
      }
    }

    // ACT: EAT
    //-------------------------------------------------
    if (hormone[H_LEPTIN] < random(0, 0.5f))
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
    if ((hormone[H_TESTOSTERONE] > 0.5f) &&
        (hormone[H_LEPTIN] > 0))
    {
      float random_thresh = 0.01f;
      
      // limit chances of reproduction when swarm is near its limit
      if (swarm.size > 0.75f * w_swarm_limit)
      {
        random_thresh *= (w_swarm_limit - swarm.size) / (0.25f * w_swarm_limit);
      }

      if (random(1) < random_thresh)
      {
        this.reproduce();
        
        // was 0.5 - lowered to increase breeding
        hormone[H_TESTOSTERONE] -= 0.4f;
      }
    }
    

    // CYC: ADRENAL REGULATION
    //-------------------------------------------------
    hormone[H_ADRENALINE] = hormone[H_ADRENALINE] * (1.0f - (0.01f * gene[G_CYC_A]));
    


    // CYC: HUNGER
    //-------------------------------------------------
    hormone[H_LEPTIN] = hormone[H_LEPTIN] - 0.001f * gene[G_CYC_L];
    

    // happier when full / unhappier when hungry
    hormone[H_SEROTONIN] += 0.0001f * hormone[H_LEPTIN];

    // ACT: STARVE
    //-------------------------------------------------
    if (hormone[H_LEPTIN] < -0.9f)
    {
      trace("die: starvation");
      this.destroy();
      return;
    }

    // ACT: OVERLOAD
    //-------------------------------------------------
    if ((hormone[H_ADRENALINE] > 0.9f) ||
        (hormone[H_TESTOSTERONE] > 0.9f) ||
        (hormone[H_SEROTONIN] < -0.9f))
     {
       trace("die: hormone overload");
       this.destroy();
       return;
     }
  }


  public void calculateVector()
  {
    // RULE: INERTIA
    //-------------------------------------------------
    float w_inertia_local = clip1(
      w_inertia *
      (hormone[H_ADRENALINE] * 0.1f + 1) *
      (1 - hormone[H_MELATONIN] * 0.3f)
    );
    vx = w_inertia_local * vx;
    vy = w_inertia_local * vy;
  
  
    // RULE: COHESION
    //-------------------------------------------------
    vx += w_cohesion * ((swarm.meanX - x) / 200.0f) * (2 - gene[G_INT] * 2) * (1 + hormone[H_SEROTONIN] * 0.5f);
    vy += w_cohesion * ((swarm.meanY - y) / 200.0f) * (2 - gene[G_INT] * 2) * (1 + hormone[H_SEROTONIN] * 0.5f);
    
    
    // RULE: ALIGNMENT
    //-------------------------------------------------
    vx += w_alignment * ((swarm.meanvX - vx) / 20.0f) * (1 - gene[G_INT]);
    vy += w_alignment * ((swarm.meanvY - vy) / 20.0f) * (1 - gene[G_INT]);
    
    
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
        vx -= w_separation * 0.2f * (swarm.atoms[i].x - this.x) * (gene[G_INT] * 2);
        vy -= w_separation * 0.2f * (swarm.atoms[i].y - this.y) * (gene[G_INT] * 2);
        
        // ACT: COLLIDE
        //-------------------------------------------------
        hormone[H_TESTOSTERONE] += 0.002f * gene[G_UP_T];
        hormone[H_ADRENALINE] += 0.001f * gene[G_UP_A];
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
        vx += w_gravity * 10 * (swarm.atoms[i].x - this.x) / sq(distance) * (1.5f - gene[G_INT]);
        vy += w_gravity * 10 * (swarm.atoms[i].y - this.y) / sq(distance) * (1.5f - gene[G_INT]);
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
    float w_brownian_freq_local = sq(w_brownian_freq * 0.2f)
        * (hormone[H_ADRENALINE] + 1)
        * (hormone[H_TESTOSTERONE] + 1);
        
    if (random(1.0f) < w_brownian_freq_local)
    {
      float w_brownian_local = w_brownian
          * ((hormone[H_ADRENALINE] + 1));
          
      vx += w_brownian_local * (random(20.0f) - 10);
      vy += w_brownian_local * (random(20.0f) - 10);
    }
    
    // center
//    vx += w_centre * ((width / 2) - this.y) / width;
//    vy += w_centre * ((height / 2) - this.y) / height;
    
    if (x < w_bound_threshold)
       vx += w_bound * ((w_bound_threshold - x) / 50.0f);
    if (x > width - w_bound_threshold)
       vx -= w_bound * ((x - (width - w_bound_threshold)) / 50.0f);

    if (y < w_bound_threshold)
       vy += w_bound * ((w_bound_threshold - y) / 50.0f);
    if (y > height - w_bound_threshold)
       vy -= w_bound * ((y - (height - w_bound_threshold)) / 50.0f);

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
  public AtomGenetic reproduce()
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

  public void eat(AtomFood food)
  {
    swarm.removeFood(food);
    this.flash(20, color(40, 100, 200));
    hormone[H_LEPTIN] = hormone[H_LEPTIN] + 0.25f + (0.5f * gene[G_UP_L]);
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
  
  public void play()
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

  public AtomGenetic reproduce()
  {
    AtomSynthGenetic atom = (AtomSynthGenetic) super.reproduce();
    if (atom != null)
    {
      atom.play();
    }
    return atom;
  }
  
  public void move()
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
    if ((meme == null) && (random(1.0f) < 0.0001f))
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
            (random(1.0f) < meme.strength * 0.2f))
        {
          ((AtomSynthGenetic) nearest).infectMeme(meme);
        }
      }
    }
  }
  
  public void display()
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
  
  public void infectMeme(AtomMeme meme)
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
  
  public void processTriggers()
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
      float bearing_thresh = map1(gene[G_SON_TRIG_THRESH], 0.2f, 0.6f);
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
          synth.set("freq", midicps(tuning_basenote - 12) * partial + random2(2.0f));
          
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
  
  public void trigger()
  {
    if (synth != null)
    {
      synth.set("t_trig", 1);
      this.flash();
    }
  }
  
  public void setBaseNote (float value)
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
  
  public void play()
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
    size = random(5.0f, 15.0f);
    //alpha = linlin(size, 0.8, 2.0, 50, 150);
    alpha = random(50, 150);
  }
  
  public void display()
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
    this(G_MEME_GENES[(int) random(G_MEME_GENES.length)], random(1.0f));
  }
  
  AtomMeme(int gene, float value)
  {
    this.gene = gene;
    this.value = value;
    this.strength = random(1.0f);
    this.age = 0;
    this.age_max = 500 + random(200);
  }
}
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

float w_spider_capture_chance = 0.02f,
      w_spider_capture_thresh = 100,
      w_spider_release_chance = 0.02f,
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
	map_vel = new Spec("pitchratio", 0.2f, 2, 1);
        break;
      case 3:
        synth_name = "fx_delay_seaside";
        map_axis[0] = new Spec("delay_time", 0.01f, 0.2f, 0);
        map_axis[1] = new Spec("decay_time", 0.1f, 5.0f, 0);
        break;
      case 4:
        synth_name = "fx_feedli";
        map_axis[0] = new Spec("feedback", 0.6f, 1.0f, 0);
        map_axis[1] = new Spec("delaytime", 0.01f, 0.5f, 0);
        break;
      case 5:
        synth_name = "fx_ringmod";
        map_vel = new Spec("freq", 5, 30, 1);
        break;
      case 6:
        synth_name = "fx_rev_iron";
        map_axis[0] = new Spec("reverbtime", 2.0f, 10.0f, 0);

        map_vel = new Spec("wet");
        break;
      case 7:
        synth_name = "fx_granule";
        map_vel = new Spec("pitchratio", 0.1f, 10, 1);
//        map_axis[0] = new Spec("windowsize", 0.01, 0.2, 0);
        map_axis[1] = new Spec("amp", 1, 2, 0);
        break;
        
        // can sometimes blow up and get load - avoid for now
      case 8:
        synth_name = "fx_charsiesis";
        map_vel = new Spec("feedback", 0.0f, 0.4f, 0);
        map_axis[0] = new Spec("rate_range", 0.01f, 5.0f, 1);
        map_axis[1] = new Spec("lpf_f", 300, 5000, 1);
        break;
    }

    synth.set("wet", 1.0f);
    synth.set("inbus", bus.index);
    
    this.play();
  }
  
  public void destroy()
  {
    Object [] prey_array = prey.toArray();

    // wet parameter has a Lag of 0.5s
    synth.set("wet", 0.0f);

    for (int i = 0; i < prey.size(); i++)
    {
      Atom atom = (Atom) prey_array[i];
      atom.set("outbus", swarm.outbus);
    }
    
    super.destroy();
  }
  
  public void play()
  {
    if (!playing)
    {
      synth.synthname = synth_name;
      synth.set("outbus", outbus);
      synth.addToTail();
      playing = true;
    }
  }

  public void display()
  {
    super.display();
    
    noFill();
    stroke(0, 0, 255, 220);
    ellipse(x, y, 10.0f, 10.0f);
   
    stroke(0, 50, 200, 250);

    strokeWeight(1.0f);
    
    for (int i = 0; i < swarm.size; i++)
    {
      float distance;
      
      if (swarm.atoms[i] == this)
        continue;
        
      distance = dist(this.x, this.y, swarm.atoms[i].x, swarm.atoms[i].y);

      if (prey.contains(swarm.atoms[i]))
      {
        dottedLine(x, y, swarm.atoms[i].x, swarm.atoms[i].y, 20);
        
        if ((distance > w_spider_release_thresh) && (random(1.0f) < w_spider_release_chance))
        {
          swarm.atoms[i].set("outbus", swarm.outbus);
          prey.remove(swarm.atoms[i]); 
        }
      }
      else {
        if ((distance < w_spider_capture_thresh) && (random(1.0f) < w_spider_capture_chance))
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
/*
 * AtomSwarm: Encapsulates a collective population of agents.
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 *
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/

int SIZE_LIMIT = 256;


class AtomSwarm
{
  Atom[] atoms;

  int size = 0;
  float meanX,
        meanY,
        meanvX,
        meanvY;

  float age = 0;
  boolean paused = false;

  float rate = 0.8f,
        rate_counter = 0;

  AtomListener listener;

  int outbus = (int) w_outbus;

  AtomSwarm()
  {
    atoms = new Atom[SIZE_LIMIT];

    trace("Creating swarm");
  }

  public void calculateNorms ()
  {
    int x = 0, y = 0;
    int vx = 0, vy = 0;

    if (size == 0)
       return;

    for (int i = 0; i < size; i++)
    {
      x += atoms[i].x;
      y += atoms[i].y;
      vx += atoms[i].vx;
      vy += atoms[i].vy;
    }

    meanX = x / size;
    meanY = y / size;
    meanvX = vx / size;
    meanvY = vy / size;
  }

  public void move ()
  {
    if (paused)
       return;

    age = age + rate;

    this.calculateNorms();

    for (int i = 0; i < size; i++)
    {
      atoms[i].move();
    }
  }

  public void display ()
  {
    for (int i = 0; i < size; i++)
    {
      atoms[i].display();
    }

  }

  public void destroy ()
  {
    for (int i = 0; i < size; i++)
    {
      atoms[i].destroy();
    }
  }

  public void add (Atom atom)
  {
    if (size < SIZE_LIMIT)
       atoms[size++] = atom;

    trace("Added: " + atom.name + ", new size:" + size);
  }

  public void remove (Atom atom)
  {
    int index = -1;

    if (atom == listener)
      return;

    for (int i = 0; i < atoms.length; i++)
    {
      if (atoms[i] == atom)
      {
        index = i;
        break;
      }
    }

    if (index > -1)
    {
      Atom[] atoms_new = new Atom[SIZE_LIMIT];

      for (int i = 0; i < index; i++)
        atoms_new[i] = atoms[i];
      for (int i = index; i < atoms.length - 1; i++)
        atoms_new[i] = atoms[i + 1];

      atoms = atoms_new;
      size = size - 1;
    }
  }

  public void scatter ()
  {
    for (int i = 0; i < size; i++)
    {
      atoms[i].scatter();
    }
  }

  public void unify ()
  {
    int [] basenotes = new int[100];
    int basenote_max_index = -1;
    int basenote_max_count = -1;

    for (int i = 0; i < 100; i++)
        basenotes[i] = 0;

    for (int i = 0; i < size; i++)
    {
      if (atoms[i] instanceof AtomTuned)
         basenotes[(int) ((AtomTuned) atoms[i]).tuning_basenote]++;
    }

    for (int i = 0; i < 100; i++)
    {
      if (basenotes[i] > basenote_max_count)
      {
        basenote_max_count = basenotes[i];
        basenote_max_index = i;
      }
    }

    if (basenote_max_index > 0)
    {
      for (int i = 0; i < size; i++)
      {
        if (atoms[i] instanceof AtomTuned)
           ((AtomTuned) atoms[i]).setBaseNote(basenote_max_index);
      }
    }
  }
}


/*
 * AtomSwarmGenetic: Adds metabolic function to a swarm.
 *
 *   - Creates food deposits at regular intervals.
 *-------------------------------------------------------------------*/

class AtomSwarmGenetic extends AtomSwarm
{
  AtomFood [] food;
  int         food_count = 0;

  AtomSwarmGenetic()
  {
    super();

    food = new AtomFood[512];

    this.addFood((int) random(1, 10));
    this.addFood((int) random(1, 10));
    this.addFood((int) random(1, 10));
  }

  public void move()
  {
    super.move();

    if (paused)
      return;

    if (random(1) < 0.0015f)
    {
      // create food stock
      this.addFood((int) random(1, 15));
    }
  }

  public void display()
  {
    super.display();

    for (int i = 0; i < food_count; i++)
      food[i].display();
  }

  public void addFood(int count)
  {
    float x = random(50, width - 50);
    float y = random(50, height - 50);

    for (int i = 0; i < count; i++)
    {
      float x_this = clip(bilinrand(100) + x, 5, width - 5);
      float y_this = clip(bilinrand(100) + y, 5, height - 5);
      this.addFood(x_this, y_this);
    }
  }

  public void addFood(float x, float y)
  {
    if (food_count < 512)
    {
      AtomFood morsel = new AtomFood(x, y);
      food[food_count++] = morsel;
    }
  }

  public void removeFood (AtomFood morsel)
  {
    int index = -1;

    for (int i = 0; i < food.length; i++)
    {
      if (food[i] == morsel)
      {
        index = i;
        break;
      }
    }

    if (index > -1)
    {
      AtomFood[] food_new = new AtomFood[512];

      for (int i = 0; i < index; i++)
        food_new[i] = food[i];
      for (int i = index; i < food.length - 1; i++)
        food_new[i] = food[i + 1];

      food = food_new;
      food_count = food_count - 1;
    }
  }
}
class Dodger {

  //the dodger has x and y coordinates and an angle
  PVector pos;
  PVector move;
  float a;
  float size = dodgerSize;
  float vel = startVel;
  float auraTrans = 0;
                         // scales dodger aura

  Dodger (float _x, float _y, float _a) {
    pos = new PVector(_x, _y);
    a = _a;
  }

  //// draw the aura of dodger
  public void drawCircle(boolean scrType, PVector cPos, int scale) {
    // scrType false: score true:highScore

    pushMatrix();
    translate(cPos.x, cPos.y);

    // float auraSize = 1 + map(score, 0, highScore + 10, 0, 2);

    noStroke();
    // if(!clockwise){
    //   pg.fill(255, 255, 255, 35);
    // } else {
    //   pg.fill(255, 255, 255, 45);
    // }
    // map the transparencz of dogers aura to rotation velocity for more action
    auraTrans = (map(abs(rotVel), 0, 110, 30, 85)/3 + auraTrans)*2/3;
    fill(255, 255, 255, auraTrans);
    if(!scrType) {
      ellipse(0, 0, 2*size*(                             score% 9 /9 * scale/7),                       2*size*(     score% 9 /9 * scale/7) );
      ellipse(0, 0, 2*size*(                ((score- (score%9))/9)%9/9 * scale/5),              2*size*( ((score- (score%9))/9)%9/9 * scale/5) );
      stroke(255, 255, 255, 100);
      strokeWeight(4);
      ellipse(0, 0, 2*size*(   ((score- (score%81) )/81)%9/9 * scale/4), 2*size*(  ((score- (score%81) )/81)%9/9* scale/4) );
      noStroke();
      fill(0);
      ellipse(0, 0, size, size);
    } else {
      ellipse(0, 0, 2*size*(                             highScore% 9 /9 * scale/7),                             2*size*(     highScore% 9 /9 * scale/7) );
      ellipse(0, 0, 2*size*(                ((highScore- (highScore%9))/9)%9/9 * scale/5),                2*size*( ((highScore- (highScore%9))/9)%9/9 * scale/5) );
      stroke(255, 255, 255, 100);
      strokeWeight(4);
      ellipse(0, 0, 2*size*(   ((highScore- (highScore%81))/81)%9/9 * scale/4), 2*size*(  ((highScore- (highScore%81))/81)%9/9* scale/4) );
    }
    noStroke();
    popMatrix();
  }

  //// draw dodger
  public void draw() {
    rectMode(CENTER);
    pushMatrix();
    translate(pos.x, pos.y);
    rotate(a);
    // rect(0, 0, sin(a)*30, 50);
    stroke(255);
    strokeWeight(6);
    line(-0.5f * size, -1 * size, 0, 1 * size);
    line(0.5f * size, -1 * size, 0, 1 * size);
    // pg.line(-0.4 * size, -0.6 * size, 0.4 * size, -0.6 * size); //back line
    popMatrix();
  }

  //// update dodger position
  public void update() {
    //dodger moves
    move = new PVector(0, vel + score*scVel); // velocity adjust
    if(!clockwise){
      a -= 0.001f * rotVel;
    } else {
      a += 0.001f * rotVel;
    }
    move = move.rotate(a);
    pos.add(move);
  }

  //// check if dodger is inside the boundaries
  public void bounds() {
    if(pos.x < 0+size*2/3) {
      pos.x = 0+size*2/3;
    } else if(pos.x > width-size*2/3) {
      pos.x = width-size*2/3;
    }
    if(pos.y < 0+size*2/3) {
      pos.y = 0+size*2/3;
    } else if(pos.y > height-size*2/3) {
      pos.y = height-size*2/3;
    }
  }
}
/*
 * Drawing utility functions.
 *
 * Copyright (c) Daniel Jones 2008.
 *   <http://www.erase.net/>
 * 
 * This program can be freely distributed and modified under the
 * terms of the GNU General Public License version 2.
 *   <http://www.gnu.org/copyleft/gpl.html>
 *
 *-------------------------------------------------------------------*/

public void dottedLine (float x1, float y1, float x2, float y2, int dot_count)
{
  float delta_x = (x2 - x1) / (float) (dot_count - 1);
  float delta_y = (y2 - y1) / (float) (dot_count - 1);


  for (int i = 0; i < dot_count; i++)
  {
    // FIX FOR OPENGL
    // point(x1 + delta_x * i, y1 + delta_y * i);
    line(x1 + delta_x * i, y1 + delta_y * i, x1 + delta_x * i + 1, y1 + delta_y * i + 1);
  }
}


/*
 * Enter/exit full screen mode.
 *--------------------------------------------------------------*/

/* void fullscreenOn()  
{ 
  GraphicsDevice gdevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();    
    
  Frame fsframe = new Frame();
  fsframe.setTitle("FullScreen"); 
  fsframe.setUndecorated(true);
  fsframe.setLayout(null); 
  fsframe.setSize(gdevice.getDisplayMode().getWidth(), gdevice.getDisplayMode().getHeight()); 

  frame.dispose();
  frame.removeAll();
  frame.setVisible(false);
  frame.setLocation(0, 0);
   
  if (gdevice.isDisplayChangeSupported())
  {    
    gdevice.setFullScreenWindow(frame);  
    DisplayMode myDisplayMode = new DisplayMode(width, height, 32, DisplayMode.REFRESH_RATE_UNKNOWN);    
    gdevice.setDisplayMode(myDisplayMode); 
   
    frame.add(this); 
    this.setLocation(0, 0);
    this.requestFocus(); 
    
    Component[] myComponents = frame.getComponents();
	for (int i = 0; i < myComponents.length; i++) {
	  if (myComponents[i] instanceof PApplet) {
	    myComponents[i].setLocation(0, 0);
	  }
	}
  }
}
 
   
void fullscreenOff()  
{    
  GraphicsDevice myGraphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice ();    
  myGraphicsDevice.setFullScreenWindow( null );    
}   
*/



/*-------------------------------------------------------------------
 * trace: Debugging output (NOT YET PROPERLY IMPLEMENTED)
 *-------------------------------------------------------------------*/
public void trace (String text)
{
 println("-- " + text); 
}

/*-------------------------------------------------------------------
 * warn: Debugging output (NOT YET PROPERLY IMPLEMENTED)
 *-------------------------------------------------------------------*/
public void warn (String text)
{
 println("** " + text); 
}
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



final int MAP_LIN = 0,
          MAP_EXP = 1,
          MAP_AMP = 2;  // squared

Hashtable map_index = new Hashtable(128);





/*-------------------------------------------------------------------
 * map: Maps a value across a predefined spec range
 *-------------------------------------------------------------------*/
public float map (String map_name, float value)
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
public float linlin (float x, float a, float b, float c, float d)
{
  if (x <= a) return c;
  if (x >= b) return d;
  return (x - a) / (b - a) * (d - c) + c;
}

/*-------------------------------------------------------------------
 * linexp: Map linear range onto exponential range
 *-------------------------------------------------------------------*/
public float linexp (float x, float a, float b, float c, float d)
{
  if (x <= a) return c;
  if (x >= b) return d;
  return pow(d/c, (x-a)/(b-a)) * c;
}

/*-------------------------------------------------------------------
 * explin: Map exponential range onto linear range
 *-------------------------------------------------------------------*/
public float explin (float x, float a, float b, float c, float d)
{
  if (x <= a) return c;
  if (x >= b) return d;
  return (log(x / a)) / (log(b / a)) * (d - c) + c;
}

/*-------------------------------------------------------------------
 * expexp: Map exponential range onto exponential range
 *-------------------------------------------------------------------*/
public float expexp (float x, float a, float b, float c, float d)
{
  if (x <= a) return c;
  if (x >= b) return d;
  return pow(d / c, log(x / a) / log(b / a)) * c;
}

/*-------------------------------------------------------------------
 * map1: Linear map from [0..1] to [low..high]
 *-------------------------------------------------------------------*/
public float map1 (float value, float low, float high)
{
  return low + value * (high - low);
}

/*-------------------------------------------------------------------
 * map_register: Registers a named Spec in the global hashtable,
 *               which can be subsequently used with new Spec("name");
 *-------------------------------------------------------------------*/
public void map_register (String map_name, float v_min, float v_max, int v_type)
{
  map_index.put(map_name, new Spec(v_min, v_max, v_type));
}


/*-------------------------------------------------------------------
 * map_init: Populates global spec table with default specs.
 *-------------------------------------------------------------------*/
public void map_init ()
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
public Spec spec (String name)
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
  
  public float map (float value)
  {
    value = clip(value, 0.0f, 1.0f);
    float mapped = 0.0f;
    switch (type)
    {
      case MAP_LIN: mapped = min + ((max - min) * value); break;
      case MAP_EXP: mapped = min * pow(max / min, value); break;
      case MAP_AMP: mapped = min + ((max - min) * sq(value)); break;
    }
    
    return mapped;
  }
  
  public float unmap (float value)
  {
    float unmapped = 0.0f;
    switch (type)
    {
      case MAP_LIN: unmapped = (value - min) / (max - min); break;
      case MAP_EXP: unmapped = log(value / min) / log(max / min); break;
      case MAP_AMP: unmapped = (value - min) / (max - min);
              if (unmapped > 0) unmapped = sqrt(unmapped);
              break;
    }
    
    return clip(unmapped, 0.0f, 1.0f);
  }
}
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
public float midicps (float note)
{
  return 440.0f * pow(2, (note - 69) / 12);
}

/*-------------------------------------------------------------------
 * cpsmidi: Map from frequency to MIDI note
 *-------------------------------------------------------------------*/
public float cpsmidi (float freq)
{
  return (log(freq / 440.0f) / log(2.0f)) * 12 + 69;
}





/*-------------------------------------------------------------------
 * random2: Generate uniformly random number between [-limit..limit]
 *-------------------------------------------------------------------*/
public float random2 (float limit)
{
  return random(limit * 2) - limit;
}

/*-------------------------------------------------------------------
 * linrand: Random number up to limit with linear distribution
 *-------------------------------------------------------------------*/
public float linrand (float limit)
{
  return min(random(limit), random(limit));
}


/*-------------------------------------------------------------------
 * bilinrand: Random number up to limit with bilinear distribution
 *-------------------------------------------------------------------*/
public float bilinrand (float limit)
{
  return (random(1) - random(1)) * limit;
}




/*-------------------------------------------------------------------
 * clip: Clips value between [v_min, v_max]
 *-------------------------------------------------------------------*/
public float clip (float value, float v_min, float v_max)
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
public float clip1 (float value)
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
public float clip2 (float value)
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
public float clip2 (float value, float limit)
{
  if (value < -limit)
    return -limit;
  else if (value > limit)
    return limit;
  else
    return value;
}
  public void settings() {  fullScreen(); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "AtomSwarm_G" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
