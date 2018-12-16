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


float w_gene_mutate_chance = 0.05,
      w_gene_variation = 0.1;



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
   new Spec("amp", 0.2, 0.8, 0),
   new Spec("xa", 0.0, 1.0, 0),
   new Spec("xb", 0.0, 1.0, 0)
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
