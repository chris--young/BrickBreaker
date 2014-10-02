/* *
 * BrickBreaker.java
 * Author: Chris Young
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.math.*;
import java.util.*;
import javax.swing.*;
import java.applet.*;
import java.net.*;

public class BrickBreaker extends JPanel implements KeyListener {
  private class Vector {
    double x, y;

    Vector( double x, double y ) {
      this.x = x;
      this.y = y;
    }

    public void set( double x, double y ) {
      this.x = x;
      this.y = y;
    }
  }

  private class Brick {
    public Vector position;
    public int width, height;
    public Color color;

    Brick( Vector position, int width, int height, Color color ) {
      this.position = position;
      this.width = width;
      this.height = height;
      this.color = color;
    }
  }

  private class MovingBrick extends Brick {
    public Vector velocity;

    MovingBrick( Vector position, int width, int height, Color color ) {
      super( position, width, height, color );
      this.velocity = new Vector( 0, 0 );
    }
  }

  private double BALL_VELOCITY = 2.5;
  private final int PLAYER_VELOCITY = 2;
  private final double DEFLECTION_ANGLE = 5.0*Math.PI/2.0;

  private final static int WINDOW_WIDTH = 640;
  private final static int WINDOW_HEIGHT = 480;

  private boolean leftKeyDown = false;
  private boolean rightKeyDown = false;
  private boolean spaceKeyDown = false;

  private boolean newGame = true;
  private boolean gamePaused = false;
  private boolean gameOver = false;
  private boolean gameWon = false;

  private int score = 0;

  private ArrayList<Brick> bricks;
  private MovingBrick player = new MovingBrick( new Vector( WINDOW_WIDTH/2, WINDOW_HEIGHT-40 ), 80, 10, new Color( 100, 100, 100 ) );
  private MovingBrick ball = new MovingBrick( new Vector( player.position.x, player.position.y-10 ), 10, 10, new Color( 50, 50, 50 ) );

  private boolean bleepyBloopy = true;
  private AudioClip bleep, bloop, win, lose;

  private Vector a = new Vector(0, 0);
  private Vector b = new Vector(0, 0);
  private Vector c = new Vector(0, 0);
  private Vector d = new Vector(0, 0);
  private Vector collision = new Vector(0, 0);
  
  private double intersect, deflectionCoefficient, theta;

  private Color clearColor = new Color( 225, 225, 225 );

  private int index = 0;

  BrickBreaker() {
    this.setFocusable( true );
    addKeyListener( this );

    try {
      bleep = Applet.newAudioClip( new URL( "file:" + System.getProperty("user.dir") + "/bleep.wav" ));
      bloop = Applet.newAudioClip( new URL( "file:" + System.getProperty("user.dir") + "/bloop.wav" ));
      win = Applet.newAudioClip( new URL( "file:" + System.getProperty("user.dir") + "/win.wav" ));
      lose = Applet.newAudioClip( new URL( "file:" + System.getProperty("user.dir") + "/lose.wav" ));
    } catch( MalformedURLException exception ) {
      System.out.println( exception );
      System.exit( 1 );
    }

    setupGame();
  }

  private void setupGame() {
    bricks = new ArrayList<Brick>();
    ball.position.set( player.position.x, player.position.y-10 );

    bricks.add( player );

    for( int y = 1; y < 5; y++ )
      for( int x = 1; x < 17; x++ )
        bricks.add( new Brick( new Vector( WINDOW_WIDTH/16*x-(WINDOW_WIDTH/32), (10*y+y)+30 ), (WINDOW_WIDTH-17)/16, 10, new Color( 125, 125, 125 ) ) );
  }

  private void handleInput() {
    if( leftKeyDown && player.position.x-player.width/2 > 1 ) {
      player.velocity.x = 0-PLAYER_VELOCITY;
      if( newGame )
        ball.velocity.x = 0-PLAYER_VELOCITY;
    } else if( rightKeyDown && player.position.x+player.width/2 < WINDOW_WIDTH-1 ) {
      player.velocity.x = PLAYER_VELOCITY;
      if( newGame )
        ball.velocity.x = PLAYER_VELOCITY;
    } else {
      player.velocity.x = 0;
      if( newGame )
        ball.velocity.x = 0;
    }

    if( spaceKeyDown && newGame ) {
      ball.velocity.y = 1-BALL_VELOCITY;
      newGame = false;
    } else if( spaceKeyDown && gameOver ) {
      setupGame();
      newGame = true;
      gameOver = false;
      score = 0;
      BALL_VELOCITY = 2.5;
      bleepyBloopy = true;
    }
  }

  private Vector checkCollision( Brick brick ) {
    a.set( brick.position.x-brick.width/2, brick.position.y-brick.height/2 );
    b.set( brick.position.x+brick.width/2, brick.position.y-brick.height/2 );
    c.set( brick.position.x+brick.width/2, brick.position.y+brick.height/2 );
    d.set( brick.position.x-brick.width/2, brick.position.y+brick.height/2 );

    if( ball.position.y < a.y ) {
      if( ball.position.x < a.x ) {
        if( ball.position.x+ball.width/2 > a.x && ball.position.y+ball.height/2 > a.y ) {
          collision.set( 1, 1 );
          return collision;
        }
      } else if( ball.position.x < b.x ) {
        if( ball.position.y+ball.height/2 > a.y ) {
          collision.set( 0, 1 );
          return collision;
        }
      } else {
        if( ball.position.x-ball.width/2 < b.x && ball.position.y+ball.height/2 > b.y ) {
          collision.set( -1, 1 );
          return collision;
        }
      }
    } else if( ball.position.y < c.y ) {
      if( ball.position.x > c.x ) {
        if( ball.position.x-ball.width/2 < c.x ) {
          collision.set( -1, 0 );
          return collision;
        }
      } else if( ball.position.x < d.x ) {
        if( ball.position.x+ball.width/2 > d.x ) {
          collision.set( 1, 1 );
          return collision;
        }
      }
    } else {
      if( ball.position.x > c.x ) {
        if( ball.position.x-ball.width/2 < c.x && ball.position.y-ball.height/2 < c.y ) {
          collision.set( -1, -1 );
          return collision;
        }
      } else if( ball.position.x > d.x ) {
        if( ball.position.y-ball.height/2 < d.y ) {
          collision.set( 0, -1 );
          return collision;
        }
      } else {
        if( ball.position.x+ball.width/2 > d.x && ball.position.y-ball.height/2 < d.y ) {
          collision.set( 1, -1 );
          return collision;
        }
      }
    }
    
    collision.set( 0, 0 );
    return collision;
  }

  private void handleCollision( boolean collidingWithPlayer ) {
    if( collision.x != 0 ) {
      ball.velocity.x *= -1;
      ball.position.x = (collision.x > 0) ? ball.position.x-ball.width/2 : ball.position.x+ball.width/2;
    }
    if( collision.y != 0 ) {
      ball.velocity.y *= -1;
      ball.position.y = (collision.y > 0) ? ball.position.y-ball.width/2 : ball.position.y+ball.width/2;
    }

    if( collidingWithPlayer ) {
      intersect = ball.position.x-player.position.x;
      deflectionCoefficient = intersect/player.width/2;
      theta = deflectionCoefficient*DEFLECTION_ANGLE;
	
      ball.velocity.x = Math.sin( theta )*BALL_VELOCITY;
      ball.velocity.y = 0-Math.cos( theta )*BALL_VELOCITY;
    }

    if (bleepyBloopy == true)
      bleep.play();
    else
      bloop.play();
    bleepyBloopy = !bleepyBloopy;
  }

  public synchronized void paintComponent( Graphics graphics ) {
    super.paintComponent( graphics );

    graphics.setColor( clearColor );
    graphics.fillRect( 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT );

    for( index = 0; index < bricks.size(); index++ ) {
      graphics.setColor( bricks.get(index).color );
      graphics.fillRect( (int)Math.round( bricks.get(index).position.x-bricks.get(index).width/2 ), (int)Math.round( bricks.get(index).position.y-bricks.get(index).height/2 ), bricks.get(index).width, bricks.get(index).height );
    }

    graphics.setColor( ball.color );
    graphics.fillOval( (int)Math.round( ball.position.x-ball.width/2 ), (int)Math.round( ball.position.y-ball.height/2 ), ball.width, ball.height );

    if( newGame && !gamePaused ) {
      graphics.drawString( "Use the left and right arrows to move.", WINDOW_WIDTH/2-117, WINDOW_HEIGHT/2 );
      graphics.drawString( "Escape pauses the game.", WINDOW_WIDTH/2-76, WINDOW_HEIGHT/2+20 );
      graphics.drawString( "Press the spacebar to begin.", WINDOW_WIDTH/2-87, WINDOW_HEIGHT/2+40 );
    } else if( gamePaused && !gameOver )
      graphics.drawString( "Paused", WINDOW_WIDTH/2-22, WINDOW_HEIGHT/2+20 );
    else if( gameOver ) {
      graphics.drawString( "Game Over", WINDOW_WIDTH/2-33, WINDOW_HEIGHT/2+20 );
      graphics.drawString( "Press the spacebar to try again.", WINDOW_WIDTH/2-87, WINDOW_HEIGHT/2+40 );
    } else if( gameWon )
      graphics.drawString( "You Win!", WINDOW_WIDTH/2-27, WINDOW_HEIGHT/2+20 );

    graphics.drawString( "Score: " + score, 17, 23 );
  }

  public void keyTyped( KeyEvent e ) {} // This method is not used but must be declared to implement KeyListener.

  public void keyPressed( KeyEvent e ) {
    switch( e.getKeyCode() ) {
      case 32:
        spaceKeyDown = true;
        break;
      case 37:
        leftKeyDown = true;
        break;
      case 39:
        rightKeyDown = true;
        break;
    }
  }

  public void keyReleased( KeyEvent e ) {
    switch( e.getKeyCode() ) {
      case 27:
        if( !gameOver )
          gamePaused = !gamePaused;
        break;
      case 32:
        spaceKeyDown = false;
        break;
      case 37:
        leftKeyDown = false;
        break;
      case 39:
        rightKeyDown = false;
        break;
    }
  }

  public synchronized void physics() {
    handleInput();

    if( !gamePaused ) {
      ball.position.set( ball.position.x+ball.velocity.x, ball.position.y+ball.velocity.y );
      player.position.set( player.position.x+player.velocity.x, player.position.y+player.velocity.y );

      if( !newGame && !gameOver ) {
        if( ball.position.x-ball.width/2 <= 1 ) {
          collision.set( -1, 0 );
          handleCollision( false );
        } else if( ball.position.x+ball.width/2 >= WINDOW_WIDTH-1 ) {
          collision.set( 1, 0 );
          handleCollision( false );
        } else if( ball.position.y-ball.height/2 <= 1 ) {
          collision.set( 0, -1 );
          handleCollision( false );
        } else if( ball.position.y+ball.height/2 >= WINDOW_HEIGHT-21 ) {
          if( bricks.size() > 1 && gameOver == false ) {
            lose.play();
            gameOver = true;
            ball.velocity.set( 0, 0 );
          } else {
            collision.set( 0, 1 );
            handleCollision( false );
          }
        }

        if( bricks.size() == 1 && gameWon == false ) {
          win.play();
          gameWon = true;
        }

        for( index = 0; index < bricks.size(); index++ ) {
          Vector collisionNormal = checkCollision( bricks.get(index) );

          if( collisionNormal.x != 0 || collisionNormal.y != 0 ) {
            if( bricks.get(index) == player )
              handleCollision( true );
            else {
              handleCollision( false );
              bricks.remove(bricks.get(index));
              index--;
              score++;
              BALL_VELOCITY += 0.1;
            }
            break;
          }
        }
      }
    }
  }

  public static void main( String arguments[] ) {
    JFrame frame = new JFrame( "BrickBreaker" );
    BrickBreaker game = new BrickBreaker();

    frame.add( game );
    frame.setBounds( 100, 100, WINDOW_WIDTH, WINDOW_HEIGHT );
    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    frame.setResizable( false );
    frame.setVisible( true );

    long before = System.currentTimeMillis();
    long now, timeSplice;

    while( true ) {
      now = System.currentTimeMillis();
      timeSplice = now-before;

      if( 16-timeSplice > 0 ) {
        try {
          Thread.sleep( 16-timeSplice );
        } catch( InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      before = now;

      game.physics();
      game.repaint();
    }
  }
}
