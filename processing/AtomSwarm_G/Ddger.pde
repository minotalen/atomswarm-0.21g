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
  void drawCircle(boolean scrType, PVector cPos, int scale) {
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
  void draw() {
    rectMode(CENTER);
    pushMatrix();
    translate(pos.x, pos.y);
    rotate(a);
    // rect(0, 0, sin(a)*30, 50);
    stroke(255);
    strokeWeight(6);
    line(-0.5 * size, -1 * size, 0, 1 * size);
    line(0.5 * size, -1 * size, 0, 1 * size);
    // pg.line(-0.4 * size, -0.6 * size, 0.4 * size, -0.6 * size); //back line
    popMatrix();
  }

  //// update dodger position
  void update() {
    //dodger moves
    move = new PVector(0, vel + score*scVel); // velocity adjust
    if(!clockwise){
      a -= 0.001 * rotVel;
    } else {
      a += 0.001 * rotVel;
    }
    move = move.rotate(a);
    pos.add(move);
  }

  //// check if dodger is inside the boundaries
  void bounds() {
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
