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

import processing.opengl.*;

import oscP5.*;
import supercollider.*;
import controlP5.ControlP5;
import controlP5.Slider;

import promidi.*;            // Required for MIDI controls
//import moviemaker.*;         // Required for video recording

import java.lang.reflect.Constructor;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Frame;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.awt.DisplayMode;


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
float changeVel = 3.85;               // modifies all velocities
Dodger dodger;
int dodgerSize = 15;
float startVel;                       // beginning velocity of dodger, increases by scVel for every score
float scVel;
float rotVel;                         // rotation velocity of dodger
float rotAcc;                        // rotation acceleration of dodger, increases by scAcc for every score
float scAcc;
float rotMod = 1;                    // rotation modulation with q and e buttons
float rotDamp = 0.985;                // rotation velocity dampening
boolean clockwise;                    // is the player turning clockwise
PVector currentPos;                   // holds the current pos of dodger
float currentAng;                     // holds the current rotation of dodger
PVector highScorePosition;

/// Aura
float circleFactor = 0.25;             // size of aura per obstacle size
int circleAdd = 20;                  // added to size of aura
int circleTransparency = 20;
float bossCFactor = 1.5;              // boss has smaller circle and no add


void init()
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


void initGraphics()
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

void setup ()
{
  //size(1440, 900, OPENGL);
  fullScreen();

  initGraphics();
  initControls();
  //initMIDI();
  initAudio();

  map_init();

  // dodger attributes
    rotVel = 0;   // current rotation velocity
    startVel = 2.8 * changeVel;
    scVel = 0.002 * changeVel;
    // sponge something is horribly broken here, dodger always turns the same speed
    rotAcc = random(0.02, 0.03); // current rotation acceleration
    scAcc = 0.00001 * changeVel;
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





void initAudio ()
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
  synth_reverb.set("wet", 0.15);
  synth_reverb.set("reverbtime", 5.0);
  synth_reverb.set("damp", 0.1);
  synth_reverb.set("inbus", fx_bus.index);
  synth_reverb.set("outbus", (int) w_outbus);
  synth_reverb.set("amp", 1.0);
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

void stop ()
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


void draw ()
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
  float mean_distance = 0.0;
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
      synth_reverb.set("earlylevel", 0.2 + mean_distance / (width / 10));
      synth_reverb.set("taillevel", 0.1 + mean_distance / (width / 10));
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

void initControls ()
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
  s_cohesion      = addSlider(control, 0.5, 0, 0, "setCohesion", "COH");
  s_gravity       = addSlider(control, 0.5, 3, 0, "setGravity", "GRA");
  s_separation    = addSlider(control, 0.5, 6, 0, "setSeparation", "SEP");
  s_sepprop       = addSlider(control, 0.5, 9, 0, "setSepProp", "SEPPR");

  s_brownian      = addSlider(control, 0.5, 0, 3, "setBrownian", "BRO");
  s_brownian_freq = addSlider(control, 0.5, 3, 3, "setBrownianFreq", "BRF");
  s_alignment     = addSlider(control, 0.2, 6, 3, "setAlignment", "ALI");
  s_inertia       = addSlider(control, 0.5, 9, 3, "setInertia", "INE");

  s_rate          = addSlider(control, 0.4, 0, 6, "setRate", "RATE");
  s_trail         = addSlider(control, 0.1, 3, 6, "setTrail", "TRAIL");
  s_amp           = addSlider(control, 0.5, 6, 6, "setAmp", "AMP");
}

Slider addSlider (ControlP5 control, float def, int x, int y, String method, String label)
{
  Slider slider = (Slider) control.addSlider(method, 0, 1, def, 20 + (60 * x), 20 + (25 * y), 150, 50);
  slider.setLabel(label);
  slider.setLabel(label);
  return slider;
}

void setCohesion (float value)
{
  w_cohesion = value * 2;
}

void setGravity (float value)
{
  w_gravity = value * 2;
}

void setSeparation (float value)
{
  w_separation = value * 2;
}

void setSepProp (float value)
{
  w_sepprop = value * 2;
}

void setBrownian (float value)
{
  w_brownian = value * 2;
}

void setBrownianFreq (float value)
{
  w_brownian_freq = value * 2;
}

void setAlignment (float value)
{
  w_alignment = value * 2;
}

void setInertia (float value)
{
  w_inertia = 0.9 + (value * 0.1);
}

void setRate (float value)
{
  if (swarm != null)
    swarm.rate = value * 2;
}

void setTrail (float value)
{
  if(atom_selected == swarm.listener) {
    w_trail_max_length = value * 300;
  } else {
    w_trail_max_length = value * 100;
  }
}

void setAmp (float value)
{
  if (synth_reverb != null)
    synth_reverb.set("amp", value * 2.0);
}



/*
 * Key handlers, primarily to create/destroy atoms.
 *--------------------------------------------------------------*/

void keyPressed ()
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

void loadSamples(String path)
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

void initMIDI ()
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

void controllerIn(Controller controller, int device, int channel)
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
      value = clip(value + (val / 127.0), 0, 1);
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
      control.setValue((float) val / 127.0);
  }
}

void noteOn(Note note, int device, int channel)
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

void createAtomOfType (String type_name)
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

void destroyAtomOfType (String type_name)
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

void mousePressed ()
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

void mouseDragged()
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

void drawGenetics()
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

void cacheSynths()
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

void keyReleased() { // listen for user input // touchEnded
  if(clockwise){
    // if(soundEffects) {
    //   sLeft.trigger();
    // }
    rotVel = max(-150, -22-rotVel/5);
  }
  clockwise = false;
}
