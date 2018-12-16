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

w_pan_range             = 200.0,      // panning distance between speakers (pixels)
w_audio_range           = 300.0,      // amplitude range from min..max (pixels)
w_day_length            = 2500,       // (seconds)
w_lifespan              = 5000,       // mean lifespan of agent (seconds)
w_swarm_limit           = 50,         // max swarm size
w_outbus                = 0,          // audio output bus

w_record_audio          = 0,          // record audio within SC?
w_record_pip            = 0,          // play initial synchro pip
w_record_video          = 0,          // record video within P5?
w_cache_synths          = 0,          // precache synth objects to prevent dropouts?

w_inertia               = 0.97,       //       
w_max_velocity          = 12.0,       // (pixels/sec)
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
w_bound                 = 1.5,        //

w_bound_threshold       = 100,        // bounding distance from edge of screen (pixels)
w_separation_threshold  = 30,         // threshold to avoid other agents (pixels)
w_gravity_threshold     = 150,        // threshold under which gravity takes effect
w_velthresh_limit       = 5,          // 

w_trail_max_length      = 40;         // max number of trail segments

String

w_record_path           = "/Users/daniel/projects/swarm/audio";
