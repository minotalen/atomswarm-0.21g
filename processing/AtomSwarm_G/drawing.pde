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

void dottedLine (float x1, float y1, float x2, float y2, int dot_count)
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
void trace (String text)
{
 println("-- " + text); 
}

/*-------------------------------------------------------------------
 * warn: Debugging output (NOT YET PROPERLY IMPLEMENTED)
 *-------------------------------------------------------------------*/
void warn (String text)
{
 println("** " + text); 
}
