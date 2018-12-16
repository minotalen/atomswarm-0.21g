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

  float rate = 0.8,
        rate_counter = 0;

  AtomListener listener;

  int outbus = (int) w_outbus;

  AtomSwarm()
  {
    atoms = new Atom[SIZE_LIMIT];

    trace("Creating swarm");
  }

  void calculateNorms ()
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

  void move ()
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

  void display ()
  {
    for (int i = 0; i < size; i++)
    {
      atoms[i].display();
    }

  }

  void destroy ()
  {
    for (int i = 0; i < size; i++)
    {
      atoms[i].destroy();
    }
  }

  void add (Atom atom)
  {
    if (size < SIZE_LIMIT)
       atoms[size++] = atom;

    trace("Added: " + atom.name + ", new size:" + size);
  }

  void remove (Atom atom)
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

  void scatter ()
  {
    for (int i = 0; i < size; i++)
    {
      atoms[i].scatter();
    }
  }

  void unify ()
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

  void move()
  {
    super.move();

    if (paused)
      return;

    if (random(1) < 0.0015)
    {
      // create food stock
      this.addFood((int) random(1, 15));
    }
  }

  void display()
  {
    super.display();

    for (int i = 0; i < food_count; i++)
      food[i].display();
  }

  void addFood(int count)
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

  void addFood(float x, float y)
  {
    if (food_count < 512)
    {
      AtomFood morsel = new AtomFood(x, y);
      food[food_count++] = morsel;
    }
  }

  void removeFood (AtomFood morsel)
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
